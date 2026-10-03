package plutus

import cats.effect.*
import cats.syntax.all.*
import fs2.io.file.CopyFlag
import fs2.io.file.CopyFlags
import porcupine.*

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

// The file-level safety net around every command that opens the book: refuse a
// path that isn't there, copy the book aside before a real run, and afterwards
// keep that copy as <input>.<yyyyMMddTHHmmssZ>.bak if the run changed anything
// (or failed) and delete it if it didn't.
//
// Whether a run will write anything isn't knowable until the book is open — an
// import with nothing to file can still create, move, rename, un-hide or tag
// an asset account — so the copy is taken before we know, under a temporary
// name, and promoted only once SQLite's own total_changes() says something
// changed. A run that changes nothing therefore leaves no artefact behind, and
// no run's backup overwrites another's: the name carries the run's own
// timestamp, so every backup is kept and each undoes exactly the run it
// precedes. Nothing prunes them — see the README.
//
// The temporary name carries that timestamp too, so promoting is only dropping
// the .tmp. That is what lets a run that died before promoting be finished off
// rather than overwritten (promoteAbandonedBackups), and under the dead run's
// own stamp rather than this one's.
//
// `body` picks its own transaction boundary (db.transactOrRollBack) rather
// than being wrapped here, because restore-account has to keep its interactive
// prompt outside the write lock. What is enforced here either way is that a
// dry run leaves the book untouched: total_changes() counts rolled-back rows
// too, so a write that slipped past a dryRun guard is caught and reported
// rather than passing the plan off as complete.
//
// The copy is taken before the book is opened and so before GncLock has had a
// chance to refuse the run, which costs a wasted copy on a book somebody else
// has open. The alternative — look at the lock first, then copy, then take it
// — would put the whole duration of that copy between the look and the take,
// which is exactly the window the lock exists to close.
def withBook[A](
    input: fs2.io.file.Path,
    now: Instant,
    dryRun: Boolean,
    ignoreLock: Boolean
)(
    body: Database[IO] => IO[A]
)(using verbosity: Verbosity): IO[A] = for
  _ <- requireExistingBook(input)
  backup = fs2.io.file.Path(s"$input.${formatBackupTimestamp(now)}.bak")
  temporaryBackup = fs2.io.file.Path(s"$backup.tmp")
  _ <- IO.unlessA(dryRun):
    for
      _ <- promoteAbandonedBackups(input)
      _ <- fs2.io.file.Files[IO].copy(input, temporaryBackup)
      _ <- info(s"Copied $input to $temporaryBackup.")
    yield ()
  resultAndChanged <- Database
    .open[IO](input.toString)
    .use: db =>
      given Database[IO] = db
      GncLock
        .hold(input, dryRun, ignoreLock)
        .surround:
          for
            // Counted from after the lock was taken rather than from the
            // connection's own zero: our gnclock row is a row change like any
            // other, and a run that inserted one and changed nothing else would
            // otherwise look like a run that changed the book — and keep a
            // backup identical to it, aging out the one that could undo the
            // last run that did write.
            before <- db.rowsChanged
            result <- body(db)
            after <- db.rowsChanged
          yield (result = result, changed = after - before)
    .onError:
      // Nothing ran, so the copy is of a book this run never touched — and
      // keeping it would age out the backup that could undo the last run that
      // did write, the same way an unchanged run's copy would.
      case _: BookInUse =>
        IO.unlessA(dryRun):
          fs2.io.file.Files[IO].delete(temporaryBackup) *>
            info(s"Nothing ran, so deleted $temporaryBackup.")
      // Keep the snapshot: the transaction rolls the book back, but a run that
      // failed is exactly when you want the copy that predates it.
      case _ => IO.unlessA(dryRun)(promoteBackup(temporaryBackup, backup))
  _ <-
    if dryRun then
      // The rollback has already undone them, so the book is intact and this
      // is a report about the code rather than about the book: a dry run
      // reaching any write at all means a dryRun guard is missing, and the
      // next real run would write whatever that path writes without anyone
      // having seen it in the plan.
      IO.raiseWhen(resultAndChanged.changed > 0):
        Error(
          s"Dry run attempted ${resultAndChanged.changed} row change(s), which were rolled back; the book is unchanged. This is a bug — please report it."
        )
    // The book is untouched when nothing changed, so its backup would be a
    // copy of a file that already exists, aging out the one that could undo
    // the last run that did write.
    else if resultAndChanged.changed > 0 then
      promoteBackup(temporaryBackup, backup)
    else
      fs2.io.file.Files[IO].delete(temporaryBackup) *>
        info(s"Nothing changed, so deleted $temporaryBackup.")
yield resultAndChanged.result

// Refuse a book that isn't there rather than letting SQLite create an empty
// one: porcupine opens with SQLITE_OPEN_CREATE, so a mistyped --input would
// otherwise leave a stray zero-table file beside the real book and fail
// several queries later with a bare NoSuchElementException from Account.root.
def requireExistingBook(input: fs2.io.file.Path): IO[Unit] =
  fs2.io.file
    .Files[IO]
    .exists(input)
    .flatMap: exists =>
      IO.raiseUnless(exists):
        Error(s"No GnuCash book at $input.")

// A leftover temporary backup is a copy taken by a run that died between
// taking it and promoting it. That run may well have committed its writes
// first — withBook commits inside `body`, then reads total_changes(), then
// closes the book, then promotes — and from the outside the two cases are
// indistinguishable, so the copy is treated as the one thing that could undo
// it and is kept rather than discarded. Nothing is lost by keeping a redundant
// one: it is a valid snapshot either way, just of a book that didn't change.
//
// Matching is by name, on this book only, so a temporary backup of another
// book in the same directory is left alone.
def promoteAbandonedBackups(
    input: fs2.io.file.Path
)(using verbosity: Verbosity): IO[Unit] =
  fs2.Stream
    .eval(bookDirectory(input))
    .flatMap(fs2.io.file.Files[IO].list)
    .filter: path =>
      val fileName = path.fileName.toString
      fileName.startsWith(s"${input.fileName}.") && fileName.endsWith(
        ".bak.tmp"
      )
    .evalMap: abandoned =>
      warn(
        s"$abandoned was left by a run that didn't finish, so keeping it as a backup of the book as it was before that run."
      ) *> promoteBackup(
        abandoned,
        // The filter guarantees the suffix, and the rest of the name is the
        // dead run's own timestamped backup name.
        fs2.io.file.Path(abandoned.toString.stripSuffix(".tmp"))
      )
    .compile
    .drain

// --input defaults to a bare Accounts.gnucash, so an input with no parent is
// the ordinary case rather than a degenerate one: the name resolves in the
// working directory, so that is where this book's backups are. Asking for it
// by name rather than scanning "." keeps the logged path unambiguous.
def bookDirectory(input: fs2.io.file.Path): IO[fs2.io.file.Path] =
  input.parent.fold(fs2.io.file.Files[IO].currentWorkingDirectory)(IO.pure)

// AtomicMove, so the promotion either happens or doesn't: the backup never
// appears under its final name half-formed, and never vanishes without
// arriving. Both paths sit beside the book, so the rename stays within one
// filesystem, which is what lets it be atomic. ReplaceExisting rides along so
// a second run inside the same second can't fail here, after its writes have
// already been committed — the JVM ignores it in favour of ATOMIC_MOVE, whose
// rename(2) replaces the target regardless, while Scala Native's Files.move
// reads it and ignores ATOMIC_MOVE (it renames either way).
def promoteBackup(
    temporaryBackup: fs2.io.file.Path,
    backup: fs2.io.file.Path
)(using verbosity: Verbosity): IO[Unit] =
  fs2.io.file
    .Files[IO]
    .move(
      temporaryBackup,
      backup,
      CopyFlags(CopyFlag.AtomicMove, CopyFlag.ReplaceExisting)
    ) *> info(s"Moved $temporaryBackup to $backup.")

// Compact UTC, so backups sort chronologically by name, and no colons, which
// Finder renders as slashes. The instant is the one the command took at
// invocation, so every artefact of a run carries the same stamp.
val backupTimestamp: DateTimeFormatter =
  DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")

def formatBackupTimestamp(instant: Instant): String =
  instant.atOffset(ZoneOffset.UTC).format(backupTimestamp)

// The two accounts every placement decision hangs off, resolved once per run
// rather than once per account that asks. The subroot is an IO because the
// first retired account is what brings it into being: resolving it eagerly
// would create one in a run that turns out to have nothing to retire. Memoised,
// so however many accounts ask, the book is read once, the subroot is created
// once, and a dry run fabricates one guid and prints one "would create" line
// instead of one per account.
final case class BookRoots(root: Account, archiveSubroot: IO[Account])

object BookRoots:

  // For the commands that may have to bring the Archive subroot into being:
  // transactions --to-book retiring a closed account, and archive-accounts.
  // restore-account builds its own from a subroot that must already exist.
  def creating(dryRun: Boolean)(using
      db: Database[IO],
      verbosity: Verbosity
  ): IO[BookRoots] =
    for
      root <- Account.root
      archiveSubroot <- archiveSubrootFor(root, dryRun).memoize
    yield BookRoots(root, archiveSubroot)

// The Archive subroot's name. Placement, not schema, so it lives here with the
// rest of what decides where an account belongs rather than on Account.
val ArchiveName: String = "Archive"

// The Archive subroot, created on demand — or, in a dry run of a book that has
// never archived anything, the would-be account createOrRetrieveChild yields
// without inserting, so the rest of the plan can name paths under it. It goes
// through the same creator as every other structural account precisely so the
// two runs agree: the subroot used to be created silently by a real run and
// announced by a dry one.
def archiveSubrootFor(root: Account, dryRun: Boolean)(using
    db: Database[IO],
    verbosity: Verbosity
): IO[Account] =
  createOrRetrieveChild(
    root,
    canonicalPathString(Nil, retired = false),
    ArchiveName,
    dryRun,
    placeholder = true,
    // Hidden by definition: everything under it is retired.
    hidden = true,
    // A copy of the root account down to its code and description, as every
    // mirror is a copy of its counterpart.
    template = Some(root)
  )

// One resolver for every Monzo-backed asset account. The online_id tag is the
// only thing it matches on, and identity therefore survives moves and
// renames. An account a past GUI import of the OFX sink's own output
// associated already carries that slot (see Slot.OnlineId), so an untagged
// account is one no run and no import has ever touched: a fresh account is
// created at the canonical spot and tagged, which is what puts tags in the
// book at all and what makes every later run find it by tag.
//
// That creation is unconditional (createChild, not createOrRetrieveChild):
// an account already sitting at the canonical path is left alone, and the new
// one lands beside it under the same name — which GnuCash permits. Adopting
// it instead would be a guess, since nothing in the book says it belongs to
// this Monzo account, and a wrong guess is permanent: the tag would send
// every later run's rows to the same place and online_id dedup would skip
// them, so they could never be re-filed. Two same-named siblings are the
// visible, fixable outcome instead — the tagged one is what later runs post
// to, so move the other's transactions into it and delete the empty one.
// A fresh child needs no placement enforcing afterwards: it is created at the
// canonical spot, under the canonical name, hidden iff the Monzo side is
// retired, and with no description to clear.
def resolveAssetAccount(
    livePath: List[String],
    retired: Boolean,
    monzoAccountId: monzo.AccountId,
    tagged: Option[Account],
    roots: BookRoots,
    dryRun: Boolean
)(using db: Database[IO], verbosity: Verbosity): IO[Account] =
  val canonicalPath = assetAccountPath(livePath, monzoAccountId)
  for
    account <- tagged match
      case Some(account) =>
        enforcePlacement(account, canonicalPath, retired, roots, dryRun)
      case None =>
        for
          parent <- parentFor(canonicalPath.init, retired, roots, dryRun)
          child <- createChild(
            parent,
            canonicalPathString(canonicalPath.init, retired),
            canonicalPath.last,
            dryRun,
            placeholder = false,
            // Born hidden if the Monzo side is already retired, rather than
            // created visible and hidden a line later: hidden tracks
            // retirement (see alignHidden), and an account this run is
            // bringing into being has nothing to align against.
            hidden = retired,
            template = None
          )
        yield child
    _ <- IO.unlessA(dryRun || tagged.isDefined):
      // The tag, and nothing but the tag, is what every later run finds this
      // account by; Account.tagOnlineId writes the slot, the Monzo ID it
      // carries is this call site's business.
      account.tagOnlineId(monzoAccountId.value)
  yield account

// The canonical path of the asset account a Monzo account posts into: the
// code-defined path with that account's Monzo ID in the leaf name. Several
// Monzo accounts can share a code-defined path — the map is keyed by type, so
// a closed account and the one that replaced it land on the same one, and two
// pots may share a name — and each gets an account of its own regardless
// (resolveAssetAccount never adopts by location). The ID in the name is what
// tells those siblings apart in GnuCash's account tree, and it puts the
// identity the resolver actually matches on, the online_id tag, in plain
// sight beside them.
def assetAccountPath(
    livePath: List[String],
    monzoAccountId: monzo.AccountId
): List[String] =
  livePath.init :+ s"${livePath.last} (${monzoAccountIdLabel(monzoAccountId)})"

// The Monzo account ID as it reads in an account name: acc_ dropped, since
// every account named this way is a Monzo account and the prefix tells a
// reader nothing, and the rest upper-cased, so it sits among account names
// the way a sort code or an account number does rather than as a stretch of
// mixed-case noise. Purely cosmetic: the identity the resolver matches on is
// the raw ID in the online_id tag, so nothing needs the ID back out of a
// name. Upper-casing is lossy — Monzo's IDs are mixed-case — but only for the
// name: two IDs differing only in case still get an account each, because
// resolution matches on the tag, not on the name. Locale.ROOT because a
// Turkish-locale machine upper-cases i to İ, which would give one account two
// different canonical names on two machines.
def monzoAccountIdLabel(monzoAccountId: monzo.AccountId): String =
  monzoAccountId.value.stripPrefix("acc_").toUpperCase(Locale.ROOT)

// A canonical path's parent chain: the live one while the Monzo side is
// live, its Archive-nested twin once retired.
//
// Not boundaryParentFor on both sides, despite its `retired = false` also
// naming a chain under the root: that one *mirrors an archived chain back out*,
// and the paths here are code-defined (AssetAccounts.default) with no archived
// counterpart to mirror. Routing them through it would also force the Archive
// subroot — creating one in a run whose accounts are all live, which is the
// laziness BookRoots exists for.
def parentFor(
    pathInit: List[String],
    retired: Boolean,
    roots: BookRoots,
    dryRun: Boolean
)(using db: Database[IO], verbosity: Verbosity): IO[Account] =
  if retired then boundaryParentFor(pathInit, retired = true, roots, dryRun)
  else liveParentFor(pathInit, roots, dryRun)

// Textual, so a dry run can name targets whose parents don't exist yet.
def canonicalPathString(livePath: List[String], retired: Boolean): String =
  val canonical =
    if retired then ArchiveName :: livePath else livePath
  ("Root Account" :: canonical).mkString("/")

// Monzo is authoritative for a Monzo-backed asset account's whole placement.
// Its canonical parent is the code-defined live path while the Monzo side is
// live, and the same path nested under the Archive subroot once retired (a
// closed account, a deleted pot); its name is the path's leaf — for pots, the
// pot's current Monzo name — with the Monzo account ID appended (see
// assetAccountPath); its description is empty (see alignDescription); and
// hidden tracks retirement. All of it is enforced in both directions on every
// run — un-archiving, un-hiding, renaming and un-describing included — so a
// hand-move lasts only until the next import. A *different* account already
// occupying the canonical spot fails the run: merging or deleting it could
// orphan its transactions, so the user resolves that collision by hand.
def enforcePlacement(
    account: Account,
    livePath: List[String],
    retired: Boolean,
    roots: BookRoots,
    dryRun: Boolean
)(using db: Database[IO], verbosity: Verbosity): IO[Account] =
  val name = livePath.last
  val targetPath = canonicalPathString(livePath, retired)
  for
    parent <- parentFor(livePath.init, retired, roots, dryRun)
    inPlace = account.parentGuid.contains(parent.guid) && account.name == name
    placed <-
      if inPlace then IO.pure(account)
      else
        for
          collision <- parent.child(name)
          _ <- IO.raiseWhen(collision.exists(_.guid != account.guid)):
            Error(
              s"A different account already sits at $targetPath; resolve it by hand — merging or deleting it automatically could orphan its transactions."
            )
          accountPath <- account.pathString
          moved <-
            if dryRun then
              info(s"Would move $accountPath to $targetPath.").as(account)
            else
              account
                .update(parent = parent, name = name)
                .flatTap: _ =>
                  info(s"Moved $accountPath to $targetPath.")
        yield moved
    aligned <- alignHidden(placed, livePath, retired, dryRun)
    described <- alignDescription(aligned, livePath, retired, dryRun)
  yield described

// Hidden tracks retirement, aligned in both directions. Takes its arguments in
// the same order as alignDescription, which runs beside it on every enforced
// account.
def alignHidden(
    account: Account,
    livePath: List[String],
    retired: Boolean,
    dryRun: Boolean
)(using db: Database[IO], verbosity: Verbosity): IO[Account] =
  val label = canonicalPathString(livePath, retired)
  if account.hidden == retired then IO.pure(account)
  else if dryRun then
    info(s"Would ${if retired then "hide" else "unhide"} $label.").as(account)
  else
    account
      .updateHidden(retired)
      .flatTap: _ =>
        info(s"${if retired then "Hid" else "Unhid"} $label.")

// The canonical description of a Monzo-backed asset account is none at all:
// the name already says which Monzo account or pot posts into it, down to the
// ID that keeps two of a kind apart (see assetAccountPath), so anything here
// could only restate it. Aligned like hidden, so a description added by hand
// or carried in by a GUI OFX import doesn't outlive the next run. No
// description and an empty one both count as aligned, so whichever of the two
// the book holds is left alone.
def alignDescription(
    account: Account,
    livePath: List[String],
    retired: Boolean,
    dryRun: Boolean
)(using db: Database[IO], verbosity: Verbosity): IO[Account] =
  val label = canonicalPathString(livePath, retired)
  if account.description.forall(_.isEmpty) then IO.pure(account)
  else if dryRun then
    info(s"Would clear the description of $label.").as(account)
  else
    account.clearDescription
      .flatTap: _ =>
        info(s"Cleared the description of $label.")

// The parent chain on the far side of the live/archive boundary: going in, the
// live chain mirrored under the Archive subroot; coming back out, the archived
// chain mirrored under the root. All three commands cross that boundary —
// import retiring a closed account or a deleted pot, archive-accounts,
// restore-account — so each names a direction here rather than pairing `from`
// and `to` for itself and risking a mismatched pair.
def boundaryParentFor(
    pathInit: List[String],
    retired: Boolean,
    roots: BookRoots,
    dryRun: Boolean
)(using db: Database[IO], verbosity: Verbosity): IO[Account] =
  roots.archiveSubroot.flatMap: archiveSubroot =>
    mirrorParentFor(
      pathInit,
      from = if retired then roots.root else archiveSubroot,
      to = if retired then archiveSubroot else roots.root,
      retired = retired,
      dryRun
    )

// One parent chain mirrored across the live/archive boundary: (from = root,
// to = the Archive subroot) going in, the reverse coming out. The direction
// is boundaryParentFor's to choose — all three commands reach a mirror
// through it, so they share one notion of what a mirror is rather than each
// carrying its own.
// Missing segments are created on demand, each a placeholder copy of its
// counterpart under `from` — or of the parent it's created under when there
// is no counterpart. `retired` says which side `to` is, which is where the fold
// starts naming paths from, for the same reason createChild takes a parent
// path: a dry run's Archive subroot may be one this run only pretended to
// create. That last case is import's alone: archive-accounts and
// restore-account read their paths out of the book, so every segment has a
// counterpart by construction, while import's are code-defined
// (AssetAccounts.default) and only each path's top-level account is
// guaranteed to exist. A Monzo account closed before the book ever saw it is
// the ordinary way there: its archive chain mirrors a live chain that was
// never created, since nothing was ever live to create it.
def mirrorParentFor(
    pathInit: List[String],
    from: Account,
    to: Account,
    retired: Boolean,
    dryRun: Boolean
)(using db: Database[IO], verbosity: Verbosity): IO[Account] =
  pathInit
    .foldLeftM(
      (
        mirror = to,
        mirrorPath = canonicalPathString(Nil, retired),
        counterpart = Some(from): Option[Account]
      )
    ): (cursors, segment) =>
      for
        nextCounterpart <- cursors.counterpart match
          case Some(counterpart) => counterpart.child(segment)
          case None              => IO.pure(None)
        nextMirror <- createOrRetrieveChild(
          cursors.mirror,
          cursors.mirrorPath,
          segment,
          dryRun,
          placeholder = true,
          hidden = false,
          template = nextCounterpart
        )
      yield (
        mirror = nextMirror,
        mirrorPath = s"${cursors.mirrorPath}/$segment",
        counterpart = nextCounterpart
      )
    .map(_.mirror)

// The live parent chain for a code-defined path, created on demand below its
// top-level account — which must already exist, and does in any freshly
// created book (GnuCash makes Assets, Expenses, Income and Liabilities).
// Intermediate segments are placeholders: only leaves take postings.
def liveParentFor(
    pathInit: List[String],
    roots: BookRoots,
    dryRun: Boolean
)(using db: Database[IO], verbosity: Verbosity): IO[Account] =
  for
    top <- roots.root
      .child(pathInit.head)
      .flatMap:
        IO.fromOption(_):
          Error(s"No account at ${pathInit.head}")
    parent <- pathInit.tail
      .foldLeftM(
        (
          account = top,
          path = canonicalPathString(List(pathInit.head), retired = false)
        )
      ): (cursor, segment) =>
        createOrRetrieveChild(
          cursor.account,
          cursor.path,
          segment,
          dryRun,
          placeholder = true,
          hidden = false,
          template = None
        ).map: child =>
          (account = child, path = s"${cursor.path}/$segment")
      .map(_.account)
  yield parent

// Get-or-create one child, for the paths where sharing is the point: a
// category leaf several transactions file into, and the structural segments
// above it.
def createOrRetrieveChild(
    parent: Account,
    parentPath: String,
    name: String,
    dryRun: Boolean,
    placeholder: Boolean,
    hidden: Boolean,
    template: Option[Account]
)(using db: Database[IO], verbosity: Verbosity): IO[Account] =
  parent
    .child(name)
    .flatMap:
      case Some(child) => IO.pure(child)
      case None        =>
        createChild(
          parent,
          parentPath,
          name,
          dryRun,
          placeholder,
          hidden,
          template
        )

// Create one child, whether or not a sibling of that name already exists —
// GnuCash identifies an account by guid and permits the duplicate name, and
// resolveAssetAccount relies on that to avoid adopting an account it can't
// know is the right one.
//
// A created child inherits its account type and commodity from `template` —
// by default the parent, so an Expenses child is an EXPENSE account and a
// Liabilities child a LIABILITY (the literal accounts.account_type values);
// mirrorParentFor passes the counterpart being mirrored so archived and
// restored parents match what they mirror. Code and description come from an
// explicit template only, never from the parent: a mirror is a copy of its
// counterpart down to those fields, while a fresh asset or category account
// is born carrying neither, so nothing inherits a description enforcePlacement
// would then clear. Structural path segments are created as placeholders,
// leaves that take postings are not; `hidden` is likewise the child's own —
// never the template's — so a mirror created under the Archive subroot is
// visible within it, and only an account whose Monzo side is already retired
// (or the subroot itself) is born hidden. None of the three defaults, so that
// adding a flag here can't leave a call site silently taking the old one. A
// dry run inserts nothing but still
// yields the would-be account, so the rest of the plan can proceed against
// it — which is why `parentPath` is passed in rather than read back out of
// the book: in a dry run the parent may itself be a would-be account that was
// never inserted, and Account.pathString walks parent_guid through the
// accounts table, so it would come back empty and log "/Monzo".
def createChild(
    parent: Account,
    parentPath: String,
    name: String,
    dryRun: Boolean,
    placeholder: Boolean,
    hidden: Boolean,
    template: Option[Account]
)(using db: Database[IO], verbosity: Verbosity): IO[Account] =
  for
    guid <- newGuid
    child = template
      .getOrElse(parent)
      .copy(
        guid = guid,
        name = name,
        parentGuid = Some(parent.guid),
        code = template.flatMap(_.code),
        description = template.flatMap(_.description),
        hidden = hidden,
        placeholder = placeholder
      )
    _ <- IO.unlessA(dryRun)(child.insert)
    _ <- info:
      val verb = if dryRun then "Would create" else "Created"
      s"$verb account $parentPath/$name."
  yield child
