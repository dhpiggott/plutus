package plutus

import cats.effect.*
import cats.syntax.all.*
import com.monovore.decline.*
import cue4s.*
import fs2.io.file.CopyFlag
import fs2.io.file.CopyFlags
import porcupine.*

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import scala.collection.immutable.SortedMap

lazy val gnucashOpts: Opts[IO[Unit]] = Opts.subcommand(
  name = "gnucash",
  help = "GnuCash housekeeping."
):
  archiveAccountsOpts orElse restoreAccountOpts orElse importTransactionsOpts

lazy val archiveAccountsOpts: Opts[IO[Unit]] = Opts.subcommand(
  name = "archive-accounts",
  help = "Archive hidden accounts."
):
  (verbosityOpts, inputOpts, archiveDryRunOpts, ignoreLockOpts).tupled.map:
    (verbosity, input, dryRun, ignoreLock) =>
      archiveAccounts(input, dryRun, ignoreLock)(using verbosity)

lazy val archiveDryRunOpts: Opts[Boolean] =
  Opts
    .flag(
      "dry-run",
      help =
        "Print what would be archived without writing to the book and without taking a backup."
    )
    .orFalse

// Shared by all three commands rather than spelled out per command like
// --dry-run above, whose help text differs by what each one would print: this
// one says the same thing whichever command carries it.
lazy val ignoreLockOpts: Opts[Boolean] =
  Opts
    .flag(
      "ignore-lock",
      help =
        "Open the book even though GnuCash's gnclock table says another process has it open. Only for a lock left behind by a GnuCash that crashed — a live one will overwrite whatever this run writes."
    )
    .orFalse

def archiveAccounts(
    input: fs2.io.file.Path,
    dryRun: Boolean,
    ignoreLock: Boolean
)(using verbosity: Verbosity): IO[Unit] =
  for
    now <- IO.realTimeInstant
    _ <- withBook(input, now, dryRun, ignoreLock): db =>
      given Database[IO] = db
      for
        // TODO: Change this to accept a single account to archive, like
        // restore-account does?
        _ <- info:
          "Finding hidden accounts…"
        // Whole run in one transaction, the Archive subroot's own creation
        // included: a failure partway through rolls every account archived so
        // far in this run — and, on a book's first archive, the subroot it
        // created to hold them — back to where it started, rather than
        // leaving some archived and others not.
        _ <- db.transactOrRollBack(dryRun):
          for
            roots <- BookRoots.creating(dryRun)
            archiveSubroot <- roots.archiveSubroot
            hiddenAccounts <- hiddenAccountsToArchive(
              roots.root,
              archiveSubroot
            )
            _ <- (IO.traverse:
              hiddenAccounts
            ): hiddenAccount =>
              for
                hiddenAccountPath <- hiddenAccount.pathString
                livePathInit <- hiddenAccount.pathInitBelow(roots.root)
                archiveParent <- boundaryParentFor(
                  livePathInit,
                  retired = true,
                  roots,
                  dryRun
                )
                // Where the account will sit, spelled out rather than read
                // back: in a dry run its parents are would-be accounts that
                // were never inserted, so pathString would come back empty.
                archivedPath = canonicalPathString(
                  livePathInit :+ hiddenAccount.name,
                  retired = true
                )
                _ <- cleanUpRedundantMirror(
                  original = hiddenAccount,
                  originalPath = hiddenAccountPath,
                  mirrorParent = archiveParent,
                  mirrorKind = "Archive",
                  dryRun
                )
                _ <- IO.unlessA(dryRun):
                  hiddenAccount.update(parent = archiveParent).void
                _ <- info:
                  val verb = if dryRun then "Would archive" else "Archived"
                  s"$verb $hiddenAccountPath to $archivedPath."
              yield ()
          yield ()
        _ <- info:
          "Finished archiving hidden accounts."
      yield ()
  yield ()

lazy val restoreAccountOpts: Opts[IO[Unit]] = Opts.subcommand(
  name = "restore-account",
  help = "Restore archived account."
):
  (verbosityOpts, inputOpts, restoreDryRunOpts, ignoreLockOpts).tupled.map:
    (verbosity, input, dryRun, ignoreLock) =>
      restoreAccount(input, dryRun, ignoreLock)(using verbosity)

lazy val restoreDryRunOpts: Opts[Boolean] =
  Opts
    .flag(
      "dry-run",
      help =
        "Print what would be restored without writing to the book and without taking a backup."
    )
    .orFalse

def restoreAccount(
    input: fs2.io.file.Path,
    dryRun: Boolean,
    ignoreLock: Boolean
)(using verbosity: Verbosity): IO[Unit] =
  for
    now <- IO.realTimeInstant
    _ <- withBook(input, now, dryRun, ignoreLock): db =>
      given Database[IO] = db
      for
        root <- Account.root
        nothingToRestore = Error("No archived accounts to restore.")
        // Retrieved, never created: a book that has never archived anything
        // has no Archive subroot and nothing to restore, and creating one
        // here would be a write outside the transaction below, on behalf of a
        // command that is about to do nothing. Which is also why the roots
        // this command hands the mirror below are both already resolved —
        // there is nothing left for them to bring into being.
        archiveSubroot <- root
          .child(ArchiveName)
          .flatMap:
            IO.fromOption(_):
              nothingToRestore
        roots = BookRoots(root, IO.pure(archiveSubroot))
        archivedAccounts <- archiveSubroot.allChildren
        _ <- IO.raiseWhen(archivedAccounts.isEmpty):
          nothingToRestore
        archivedAccountsByPath <- (IO
          .traverse:
            archivedAccounts
          ): account =>
            account.pathString.map(_ -> account)
          .map:
            SortedMap.from
        _ <- IO.raiseUnless(
          archivedAccountsByPath.size == archivedAccounts.size
        ):
          Error:
            "Archived accounts have duplicate paths."
        archivedAccountPath <- IO.blocking:
          Prompts.sync.use:
            _.singleChoice(
              "Choose account to restore",
              archivedAccountsByPath.keys.toList
            ).getOrRaise
        archivedAccount = archivedAccountsByPath(archivedAccountPath)
        // Only the writes are transacted, not the prompt above: begin
        // immediate takes SQLite's write lock immediately, and holding that
        // open while waiting on stdin would block any other writer for as
        // long as the prompt sits unanswered.
        _ <- db.transactOrRollBack(dryRun):
          for
            archivePathInit <- archivedAccount.pathInitBelow(archiveSubroot)
            nonArchiveParent <- boundaryParentFor(
              archivePathInit,
              retired = false,
              roots,
              dryRun
            )
            restoredPath = canonicalPathString(
              archivePathInit :+ archivedAccount.name,
              retired = false
            )
            _ <- cleanUpRedundantMirror(
              original = archivedAccount,
              originalPath = archivedAccountPath,
              mirrorParent = nonArchiveParent,
              mirrorKind = "Non-archive",
              dryRun
            )
            _ <- IO.unlessA(dryRun):
              archivedAccount.update(parent = nonArchiveParent).void
            _ <- info:
              val verb = if dryRun then "Would restore" else "Restored"
              s"$verb $archivedAccountPath to $restoredPath."
          yield ()
      yield ()
  yield ()

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
  // import-transactions retiring a closed account, and archive-accounts.
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

// The hidden accounts archive-accounts will move: the frontier of hidden
// accounts below `root`, stopping at the first hidden account on each branch
// (its children are implicitly hidden, so listing them too would archive a
// subtree a node at a time) and skipping the Archive subroot, since the point
// of the scan is to find what still needs moving into it.
//
// One recursive query and an in-memory walk, rather than a directChildren query
// per account: the walk visits exactly the nodes that query returned, and a
// book whose tree is mostly not hidden used to pay a round trip for each.
def hiddenAccountsToArchive(root: Account, archiveSubroot: Account)(using
    db: Database[IO]
): IO[List[Account]] =
  root.allChildren.map: descendants =>
    val byParentGuid = descendants.groupBy(_.parentGuid)
    // By name, as the directChildren query this replaces ordered them.
    def frontier(account: Account): List[Account] =
      byParentGuid
        .getOrElse(Some(account.guid), Nil)
        .sortBy(_.name)
        .flatMap: child =>
          if child.guid == archiveSubroot.guid then Nil
          else if child.hidden then List(child)
          else frontier(child)
    frontier(root)

// Handles the case where a mirror already exists at `mirrorParent` with the
// same name as `original`. This happens when a child was already
// archived/restored, resulting in the creation of a mirror of the parent
// account we're now archiving/restoring.
//
// The correct handling is to move the children of the existing mirror to be
// children of the account we're now archiving/restoring (their original
// parent) and to delete the newly redundant mirror (because it will be
// replaced when the original is moved into its place).
def cleanUpRedundantMirror(
    original: Account,
    originalPath: String,
    mirrorParent: Account,
    mirrorKind: String,
    dryRun: Boolean
)(using db: Database[IO], verbosity: Verbosity): IO[Unit] =
  for
    maybeExistingMirror <- mirrorParent.child(original.name)
    _ <- (IO.traverse:
      maybeExistingMirror
    ): existingMirror =>
      for
        _ <- warn:
          s"$mirrorKind mirror for $originalPath already exists."
        // Read before anything moves, and read rather than spelled out: the
        // mirror is always a real account here — a fabricated dry-run parent
        // has no children to find one under — but where it sits is only known
        // to the book.
        existingMirrorPath <- existingMirror.pathString
        existingChildren <- existingMirror.directChildren
        _ <- (IO.traverse:
          existingChildren
        ): child =>
          val childPath = s"$existingMirrorPath/${child.name}"
          // Where the child ends up, spelled out rather than read back after
          // the move: a dry run doesn't move it, and the path would otherwise
          // have to be queried twice to say the same thing.
          val movedChildPath = s"$originalPath/${child.name}"
          IO.unlessA(dryRun)(child.update(parent = original).void) *> warn:
            val verb = if dryRun then "Would move" else "Moved"
            s"$verb $childPath to $movedChildPath."
        _ <- IO.unlessA(dryRun)(existingMirror.delete)
        _ <- warn:
          val verb = if dryRun then "Would delete" else "Deleted"
          s"$verb existing ${mirrorKind.toLowerCase} mirror $existingMirrorPath."
      yield ()
  yield ()

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

// Lives here rather than under `monzo` because it's conceptually a GnuCash
// import — a future variant could read the CSVs the Monzo app exports instead
// of the API. The Monzo session plumbing it borrows (fetchTransactionsByAccount)
// stays in MonzoCommands.
lazy val importTransactionsOpts: Opts[IO[Unit]] = Opts.subcommand(
  name = "import-transactions",
  help = "Import Monzo transactions directly into the GnuCash book."
):
  (
    verbosityOpts,
    inputOpts,
    sinceOpts,
    beforeOpts,
    importDryRunOpts,
    ignoreLockOpts
  ).tupled.map: (verbosity, input, since, before, dryRun, ignoreLock) =>
    importTransactions(input, since, before, dryRun, ignoreLock)(using
      verbosity
    )

lazy val importDryRunOpts: Opts[Boolean] =
  Opts
    .flag(
      "dry-run",
      help =
        "Print the plan (a line per transaction that would be filed, the already-present count, and the accounts that would be created) without writing to the book and without taking a backup."
    )
    .orFalse

def importTransactions(
    input: fs2.io.file.Path,
    since: Option[Instant],
    before: Option[Instant],
    dryRun: Boolean,
    ignoreLock: Boolean
)(using verbosity: Verbosity): IO[Unit] = for
  // Checked here as well as in withBook below, so a mistyped --input costs a
  // message rather than the whole OAuth-and-fetch round trip that would
  // otherwise run before the book is ever opened.
  _ <- requireExistingBook(input)
  // The zone the transactions' calendar dates are taken in (see
  // neutralPostDate), read once so every row of a run agrees.
  zone <- IO.delay(ZoneId.systemDefault)
  (now, byAccount, pots) <- fetchTransactionsByAccount(since, before)
  _ <- withBook(input, now, dryRun, ignoreLock): db =>
    given Database[IO] = db
    val assetAccounts = AssetAccounts.default
    // Only material transactions get posted; an account with none needs no
    // asset account (byAccount lists every account, active or not) and would
    // only add noise below, so drop it here.
    val materialByAccount = byAccount
      .map: (account, transactions) =>
        (account, materialTransactions(transactions))
      .filter: (_, transactions) =>
        transactions.nonEmpty
    val run = for
      // Fail fast, before anything is resolved or written: an account type
      // missing from the map needs a byAccountType entry, not a guess.
      unmappedTypes = materialByAccount
        .flatMap: (account, _) =>
          account.accountType.filterNot: accountType =>
            assetAccounts.byAccountType.contains(accountType.value)
        .map(_.value)
        .distinct
      _ <- IO.raiseUnless(unmappedTypes.isEmpty):
        Error(
          s"No asset account mapped for Monzo account type(s) ${unmappedTypes.mkString(", ")}; add them to AssetAccounts.byAccountType."
        )
      // Fail fast on the assumption the dedup set below rests on. That
      // set is read once, before the write loop, and never updated as
      // postings are inserted, so one transaction ID fetched twice in the
      // same run would be judged unseen twice and posted twice — and there
      // is no unique constraint on the online_id slot to catch it. Monzo's
      // model says a pot transfer is one transaction in the main account's
      // statement and a separate one in the pot's own (see
      // fetchTransactionsByAccount), so neither check below should ever
      // fire; they are here because the alternative to them firing is a
      // silent double-post. They are separate checks because the two say
      // different things about Monzo: the same ID under two accounts means
      // one transaction appears in two statements, while the same ID twice
      // under one account means a page repeated it.
      occurrences = materialByAccount
        .flatMap: (account, material) =>
          material.map: transaction =>
            transaction.id.value -> account.id.value
        .groupMap((transactionId, _) => transactionId): (_, monzoAccountId) =>
          monzoAccountId
      acrossAccounts = occurrences.filter: (_, monzoAccountIds) =>
        monzoAccountIds.distinct.sizeIs > 1
      _ <- IO.raiseUnless(acrossAccounts.isEmpty):
        val listed = acrossAccounts.toList
          .sortBy((transactionId, _) => transactionId)
          .map: (transactionId, monzoAccountIds) =>
            s"$transactionId (in ${monzoAccountIds.distinct.sorted.mkString(", ")})"
          .mkString("; ")
        Error(
          s"Monzo returned the same transaction under more than one account in a single run: $listed. Filing every occurrence would double-count it, so nothing has been written. Please report this."
        )
      withinAccount = occurrences.filter: (_, monzoAccountIds) =>
        monzoAccountIds.distinct.sizeIs == 1 && monzoAccountIds.sizeIs > 1
      _ <- IO.raiseUnless(withinAccount.isEmpty):
        val listed = withinAccount.toList
          .sortBy((transactionId, _) => transactionId)
          .map: (transactionId, monzoAccountIds) =>
            s"$transactionId (${monzoAccountIds.size} times in ${monzoAccountIds.head})"
          .mkString("; ")
        Error(
          s"Monzo returned the same transaction more than once for one account in a single run: $listed. Filing every occurrence would double-count it, so nothing has been written. Please report this."
        )
      // Every typed account paired with the code-defined path its type
      // maps to. The mapping is type-keyed, so several Monzo accounts — a
      // closed account and the one that replaced it — can share a path;
      // the Monzo account ID in the leaf name is what keeps them apart
      // (see assetAccountPath), so each gets its own asset account and is
      // retired on its own closure rather than the whole type's.
      typedAccountsAndPaths = byAccount.flatMap: (account, _) =>
        account.accountType
          .flatMap: accountType =>
            assetAccounts.byAccountType.get(accountType.value)
          .map: assetPath =>
            (account, assetPath)
      allMonzoPotAccountIds = byAccount.collect:
        case (account, _) if isPotBacking(account) => account.id
      materialMonzoPotAccountIds = materialByAccount.collect:
        case (account, _) if isPotBacking(account) => account.id
      // Every online_id in the book, in one scan: the tags below and the
      // dedup check further down are the run's only two readers of them,
      // and both would otherwise scan an unindexed table that grows with
      // the book's history. See Slot.onlineIds.
      onlineIds <- Slot.onlineIds
      // The book is the durable home of the account associations: each
      // asset account is tagged with the Monzo account ID that posts into
      // it in an account-level online_id slot, so a book that outlives the
      // state store (say, moved to a new machine) still resolves by tag.
      // Resolved from the prefetch by primary key, and only for the IDs
      // this run asks about — most online_id slots name a split, not an
      // account. Two accounts tagged with one Monzo account ID fail the
      // run: nothing in the book says which is meant, and posting into the
      // wrong one would be permanent.
      monzoAccountIds = typedAccountsAndPaths
        .map((account, _) => account.id) ++ allMonzoPotAccountIds
      taggedGuids = onlineIds
        .filter: (value, _) =>
          monzoAccountIds.exists(_.value == value)
        .groupMap((value, _) => value)((_, objGuid) => objGuid)
      taggedByMonzoAccountId <- monzoAccountIds
        .traverse: monzoAccountId =>
          taggedGuids
            .getOrElse(monzoAccountId.value, Nil)
            .distinct
            .traverse(Account.byGuid)
            .map(_.flatten)
            .flatMap:
              case Nil            => IO.none
              case account :: Nil => IO.pure(Some(account))
              case accounts       =>
                IO.raiseError:
                  Error(
                    s"Several accounts are tagged with the Monzo account ID ${monzoAccountId.value}: ${accounts.map(_.name).sorted.mkString(", ")}; resolve by hand — only one account can be the one it posts into."
                  )
            .map(monzoAccountId -> _)
        .map(_.toMap)
      taggedPots = allMonzoPotAccountIds
        .flatMap: monzoAccountId =>
          taggedByMonzoAccountId(monzoAccountId).map(monzoAccountId -> _)
        .toMap
      // Fail fast on a pot the book doesn't know and no recorded link
      // names: it can't be filed into its own account, and a mis-filed row
      // would be permanent — online_id dedup skips it on every later run.
      // One run whose window spans a transfer for the pot records the link.
      unnamedPots = materialMonzoPotAccountIds.filterNot: monzoAccountId =>
        taggedPots.contains(monzoAccountId) || pots.contains(monzoAccountId)
      _ <- IO.raiseUnless(unnamedPots.isEmpty):
        Error(
          s"Nothing identifies the pot(s) behind ${unnamedPots.map(_.value).mkString(", ")} — no tagged account in the book and no recorded pot link; re-run with --since spanning a transfer for each to record the link(s)."
        )
      currency <- Commodity.gbp
      // Fail fast on a pot denominated in anything but the book's currency:
      // its minor units would otherwise be posted as if they were pence.
      foreignPots = materialMonzoPotAccountIds.flatMap: monzoAccountId =>
        pots
          .get(monzoAccountId)
          .filterNot(_.currency.value == currency.mnemonic)
          .map: pot =>
            s"${pot.name.value} (${pot.currency.value})"
      _ <- IO.raiseUnless(foreignPots.isEmpty):
        Error(
          s"Pot(s) not denominated in the book's currency (${currency.mnemonic}): ${foreignPots.mkString(", ")}."
        )
      // The root and the Archive subroot every placement below hangs off,
      // resolved once for the run. The subroot stays lazy inside: a run with
      // nothing retired must not create one.
      roots <- BookRoots.creating(dryRun)
      // Typed asset accounts and pot accounts resolve through the same
      // path: resolveAssetAccount finds by online_id tag, creates the
      // account otherwise, enforces placement, and tags one it created.
      typedAssets <- typedAccountsAndPaths
        .traverse: (account, path) =>
          resolveAssetAccount(
            livePath = path,
            // Each Monzo account has its own asset account, so retirement
            // is its own closure: a closed account is archived while the
            // account that replaced it goes on being posted to.
            retired = account.closed.exists(_.value),
            monzoAccountId = account.id,
            tagged = taggedByMonzoAccountId(account.id),
            roots = roots,
            dryRun = dryRun
          ).map(account.id -> _)
        .map(_.toMap)
      // A pot's canonical leaf name is its current Monzo name plus its
      // backing-account ID, so renames propagate and two pots that share a
      // name still get an account each. Only pots that are material this
      // run or already known to the book get resolved; the rest are posted
      // to as-is. `pots` only holds a backing account whose pot some
      // transfer has named (see State.potIds), and a tagged account can
      // outlive the state store that named it — so until a window spanning
      // one of its transfers is fetched there is no name and no deleted
      // flag to enforce against.
      // The run that does fetch one records the link and enforces then.
      potAssets <- allMonzoPotAccountIds
        .traverse: monzoAccountId =>
          pots.get(monzoAccountId) match
            case Some(pot)
                if materialMonzoPotAccountIds.contains(monzoAccountId) ||
                  taggedPots.contains(monzoAccountId) =>
              resolveAssetAccount(
                livePath = assetAccounts.pots :+ pot.name.value,
                retired = pot.deleted.value,
                monzoAccountId = monzoAccountId,
                tagged = taggedByMonzoAccountId(monzoAccountId),
                roots = roots,
                dryRun = dryRun
              ).map(account => monzoAccountId -> Some(account))
            case _ =>
              IO.pure(monzoAccountId -> taggedPots.get(monzoAccountId))
        .map:
          _.collect:
            case (monzoAccountId, Some(account)) => monzoAccountId -> account
          .toMap
      assets = typedAssets ++ potAssets
      // One book account per Monzo account, checked rather than assumed.
      // Resolution never adopts an account it found by location, so two
      // Monzo accounts can only land on one book account if the book itself
      // says they do: an online_id tag added by hand to an account another
      // one already answers to. Sharing an account would commingle two
      // Monzo accounts' rows permanently — online_id dedup skips them on
      // every later run — so the run fails instead.
      // Resolution creates, moves and renames accounts but files nothing,
      // and the whole run is one transaction (a dry run writes nothing at
      // all), so failing here leaves the book unimported.
      overloaded = assets.toList
        .groupBy((_, account) => account.guid)
        .values
        .filter(_.sizeIs > 1)
        .map: shared =>
          val (_, account) = shared.head
          val monzoAccountIds =
            shared.map((monzoAccountId, _) => monzoAccountId.value)
          s"${account.name} (${monzoAccountIds.sorted.mkString(", ")})"
        .toList
      _ <- IO.raiseUnless(overloaded.isEmpty):
        Error(
          s"Book account(s) shared by several Monzo accounts: ${overloaded.mkString("; ")}; resolve by hand — filing two Monzo accounts into one book account would commingle their transactions."
        )
      // Monzo's categories are authoritative: each files into the account
      // categoryTarget names, created on first sight — no mapping to
      // maintain. Grouped by parent so each distinct parent chain is
      // resolved once, not once per category.
      categories <- materialByAccount
        .flatMap: (_, transactions) =>
          transactions
        .map(categoryTarget)
        .distinct
        .groupBy(_.init)
        .toList
        .flatTraverse: (parentPath, paths) =>
          liveParentFor(parentPath, roots, dryRun).flatMap: parent =>
            paths.traverse: path =>
              createOrRetrieveChild(
                parent,
                canonicalPathString(parentPath, retired = false),
                path.last,
                dryRun,
                // A category leaf takes postings, is never retired on its
                // own, and is born from its parent.
                placeholder = false,
                hidden = false,
                template = None
              ).map(path -> _)
        .map(_.toMap)
      // Every account a posting touches is denominated in the book's
      // currency, so a split's value and its quantity are the same rational
      // number and neither needs an exchange rate — which is what lets
      // Posting.fromMonzo write one pair of numerators and denominators for
      // both. An account in another commodity would need a rate, and
      // posting Monzo's minor units into it as though they were the book's
      // would be wrong by it, so fail rather than file: online_id dedup
      // skips a mis-filed row on every later run, so it could never be
      // re-filed.
      foreignAccounts = (assets.values ++ categories.values).toList
        .filterNot(_.commodityGuid.contains(currency.guid))
        .map(_.name)
        .distinct
      _ <- IO.raiseUnless(foreignAccounts.isEmpty):
        Error(
          s"Account(s) not denominated in the book's currency (${currency.mnemonic}): ${foreignAccounts.sorted.mkString(", ")}."
        )
      // The dedup keys out of the same prefetch: the whole run sits in a
      // single SQLite transaction and fetched transaction IDs are unique,
      // so the set can't go stale mid-run. The account tags resolution has
      // just written aren't in it, and needn't be — those are Monzo account
      // IDs, and what this answers is whether a Monzo *transaction* ID is
      // already filed.
      importedIds = onlineIds.map((value, _) => value).toSet
      // Every row the run will file, settled before any of them is
      // written: the lines below pad each column to the widest value in
      // it, which isn't known until every row is.
      rows <- materialByAccount.flatTraverse: (account, material) =>
        // Total: unmapped types and unnamed pots failed the run up front,
        // and `assets` was built from this same list of accounts.
        val assetAccount = assets(account.id)
        material
          .filterNot: transaction =>
            importedIds.contains(transaction.id.value)
          .traverse: transaction =>
            val categoryPath = categoryTarget(transaction)
            Posting
              .fromMonzo(
                transaction,
                assetAccount,
                categories(categoryPath),
                currency,
                now,
                zone
              )
              .map: posting =>
                (
                  transaction = transaction,
                  assetAccount = assetAccount,
                  category = categoryPath.mkString(":"),
                  amount = formatAmount(posting.assetSplit.valueNum, currency),
                  posting = posting
                )
      skipped = materialByAccount
        .flatMap((_, material) => material)
        .count: transaction =>
          importedIds.contains(transaction.id.value)
      // One line per transaction filed, so the plan can be read row by row
      // rather than trusted as a count: the post date and payee GnuCash
      // will show, the signed amount as it lands on the asset leg (the
      // category leg is its negation), and the two accounts the money moves
      // between. The asset account is named by its leaf, which carries the
      // Monzo account ID, so two accounts of a kind are told apart without
      // repeating the path on every line. A dry run says what it would do,
      // as the account creations above do.
      verb = if dryRun then "Would file" else "Filed"
      // Each column padded to the widest value in it, so a run's lines read
      // as a table rather than as prose of varying length. The amounts are
      // right-aligned — every one carries the currency's full fraction, so
      // that lines them up on the decimal point — and the rest left.
      amountWidth = rows.map(_.amount.length).maxOption.getOrElse(0)
      assetWidth = rows.map(_.assetAccount.name.length).maxOption.getOrElse(0)
      categoryWidth = rows.map(_.category.length).maxOption.getOrElse(0)
      _ <- rows.traverse: row =>
        val line = List(
          verb,
          formatConsoleTimestamp(row.transaction.created.value.asInstant),
          " " * (amountWidth - row.amount.length) + row.amount,
          row.assetAccount.name.padTo(assetWidth, ' '),
          "/",
          // The colon punctuates the category, so it goes before the
          // padding rather than after it, where it would sit a column away
          // from the word it belongs to.
          (row.category + ":").padTo(categoryWidth + 1, ' '),
          s"${payee(row.transaction)}."
        ).mkString(" ")
        IO.unlessA(dryRun)(row.posting.insert) *> info(line)
      _ <- info:
        // A dry run files nothing, so it says what it would have done,
        // as the account creations above do.
        val verb = if dryRun then "would file" else "filed"
        s"${rows.size} $verb, $skipped already present."
    yield ()
    // Everything-or-nothing either way: a real run commits, a dry run is
    // rolled back rather than merely left unwritten, so that a write that
    // slipped past a dryRun guard doesn't survive a run that took no
    // backup.
    db.transactOrRollBack(dryRun)(run)
yield ()

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
// Finder renders as slashes. The instant is the Monzo session's own, so every
// artefact of a run carries the same stamp.
val backupTimestamp: DateTimeFormatter =
  DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")

def formatBackupTimestamp(instant: Instant): String =
  instant.atOffset(ZoneOffset.UTC).format(backupTimestamp)

// The console's own timestamp, deliberately not the one the transactions table
// is written with: the two happen to agree on a shape, and sharing a formatter
// would mean a change to how GnuCash stores a date silently reshaped what a
// run prints. UTC, matching the raw instant Monzo reports, rather than the
// local calendar day post_date is normalised onto — the line is a record of
// what was fetched.
val consoleTimestamp: DateTimeFormatter =
  DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

def formatConsoleTimestamp(instant: Instant): String =
  instant.atOffset(ZoneOffset.UTC).format(consoleTimestamp)

// Minor units as the book's currency reads them: 1234 at a fraction of 100 is
// "£12.34". Scaled rather than divided, so the string is exact and keeps its
// trailing zeroes; the scale is the fraction's digit count, which is what a
// power-of-ten fraction means, and GnuCash's currency commodities have no
// other kind. The sign goes outside the symbol ("-£3.60"), where a reader
// expects it.
def formatAmount(minorUnits: Long, currency: Commodity): String =
  val scale = currency.fraction.toString.length - 1
  val magnitude = BigDecimal(minorUnits.abs, scale).toString
  val sign = if minorUnits < 0 then "-" else ""
  s"$sign${currencySymbol(currency.mnemonic)}$magnitude"

// GnuCash's commodities table carries a mnemonic but no symbol, and
// java.util.Currency would answer locale-dependently — the same book would
// read "£12.34" on one machine and "GBP12.34" on another. So the symbol is
// named here, and a currency not named falls back to its own mnemonic, which
// is unambiguous if less compact. Only GBP is reachable today: Commodity.gbp
// is where the book's currency comes from.
def currencySymbol(mnemonic: String): String = mnemonic match
  case "GBP"    => "£"
  case mnemonic => mnemonic

// The account path a transaction's category leg posts to. Monzo's categories
// are authoritative, but not all of them are spending: income files under
// Income — as "General", mirroring the expense side's catch-all, since
// title-casing the category itself would produce Income:Income — and the two
// transfer-ish categories share one wash account under Assets: the two legs
// of a pot transfer cancel there, and what remains is money moved to
// institutions the book imports nothing from (still an asset, not an
// expense), awaiting manual re-filing. Everything else is an expense, named
// by title-casing the category (eating_out -> "Eating Out"; no category ->
// "General", Monzo's default). A refund arrives sign-flipped in its spending
// category and negates the expense, which is why the amount's sign plays no
// part here.
def categoryTarget(transaction: monzo.Transaction): List[String] =
  transaction.category.fold("general")(_.value) match
    case "income"                => List("Income", "General")
    case "savings" | "transfers" => List("Assets", "Transfers")
    case category                => List("Expenses", titleCased(category))

def titleCased(category: String): String =
  category
    .split('_')
    .filter(_.nonEmpty)
    .map(_.capitalize)
    .mkString(" ")

// One resolver for every Monzo-backed asset account. The online_id tag is the
// only thing it matches on, and identity therefore survives moves and
// renames. An account a past GUI import of export-transactions' OFX
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
