package plutus

import cats.effect.*
import cats.effect.std.UUIDGen
import cats.syntax.all.*
import porcupine.*
import porcupine.Codec.*

object Account:

  // Resolve a path of names from the root ("Expenses" :: "Groceries" :: Nil),
  // reusing the existing `child` navigation. None if any segment is missing, so
  // the importer can fail fast rather than silently mis-file.
  def atPath(
      segments: List[String]
  )(using db: Database[IO]): IO[Option[Account]] =
    Account.root.flatMap: root =>
      segments.foldLeftM(Option(root)):
        case (Some(parent), segment) => parent.child(segment)
        case (None, _)               => IO.pure(None)

  val ArchiveName = "Archive"

  def createOrRetrieveArchiveSubroot(using db: Database[IO]): IO[Account] =
    for
      root <- Account.root
      archiveSubroot <- root.createOrRetrieveMirror(
        parent = root,
        name = ArchiveName,
        hidden = true
      )
    yield archiveSubroot

  // The read-only sibling of createOrRetrieveArchiveSubroot, for callers that
  // only ask "is this account already archived?" and must not create the
  // subroot as a side effect (a dry-run import, for one).
  def retrieveArchiveSubroot(using db: Database[IO]): IO[Option[Account]] =
    root.flatMap(_.child(ArchiveName))

  // The account a guid names, if it names one at all — the importer resolves
  // an online_id tag this way, by the primary key, having read every tag in
  // one scan (see Slot.onlineIds). None covers the ordinary case of a tag
  // hanging off something that isn't an account: every imported split carries
  // one too.
  def byGuid(guid: String)(using db: Database[IO]): IO[Option[Account]] =
    db.option(
      query = sql"""
        ${Account.selectAccountsWithFlags}
        where accounts.guid = $text
      """.query:
        decoder
      ,
      args = guid
    )

  def root(using db: Database[IO]): IO[Account] =
    db.unique:
      sql"""
        ${Account.selectAccountsWithFlags}
        where accounts.parent_guid is null
          and accounts.name = 'Root Account'
      """.query:
        decoder

  val decoder: Decoder[Account] =
    (text *:
      text *:
      text *:
      text.opt *:
      integer *:
      integer *:
      text.opt *:
      text.opt *:
      text.opt *:
      boolean *:
      boolean *:
      nil).pmap[Account]

  // hidden and placeholder live in KVP slots, not the eponymous accounts
  // columns — GnuCash reads the flags only from the slot and keeps the column
  // as a denormalised cache. Every read derives both flags from the slot,
  // matching GnuCash's read semantics: the flag is true iff a slot exists and
  // is truthy (string_val = 'true' or a non-zero int64_val). Interpolated as a
  // plain String (literal SQL, no bind parameter), so every read site shares
  // the projection + joins and supplies only its own `where`. See Slot.scala.
  private val selectAccountsWithFlags: String =
    """
      select
        accounts.guid,
        accounts.name,
        accounts.account_type,
        accounts.commodity_guid,
        accounts.commodity_scu,
        accounts.non_std_scu,
        accounts.parent_guid,
        accounts.code,
        accounts.description,
        coalesce(
          hidden_slot.string_val = 'true',
          hidden_slot.int64_val != 0,
          0
        ) as hidden,
        coalesce(
          placeholder_slot.string_val = 'true',
          placeholder_slot.int64_val != 0,
          0
        ) as placeholder
      from accounts
      left join slots hidden_slot
        on hidden_slot.obj_guid = accounts.guid
        and hidden_slot.name = 'hidden'
      left join slots placeholder_slot
        on placeholder_slot.obj_guid = accounts.guid
        and placeholder_slot.name = 'placeholder'
    """

/** sqlite> .schema accounts CREATE TABLE accounts( guid text(32) PRIMARY KEY
  * NOT NULL, name text(2048) NOT NULL, account_type text(2048) NOT NULL,
  * commodity_guid text(32), commodity_scu integer NOT NULL, non_std_scu integer
  * NOT NULL, parent_guid text(32), code text(2048), description text(2048),
  * hidden integer, placeholder integer );
  */
final case class Account(
    guid: String,
    name: String,
    accountType: String,
    commodityGuid: Option[String],
    commodityScu: Long,
    nonStdScu: Long,
    parentGuid: Option[String],
    code: Option[String],
    description: Option[String],
    // Derived on read from KVP slots and written to both the slot and the
    // column on insert — GnuCash stores these flags in slots and treats the
    // columns as a denormalised cache. See Slot.scala and selectAccountsWithFlags.
    hidden: Boolean,
    placeholder: Boolean
):

  def createOrRetrieveMirror(
      parent: Account,
      name: String,
      hidden: Boolean = false
  )(using db: Database[IO]): IO[Account] = parent
    .child(name)
    .flatMap:
      case None =>
        for
          guid <- newGuid
          mirror = copy(
            guid = guid,
            name = name,
            parentGuid = Some(parent.guid),
            hidden = hidden,
            placeholder = true
          )
          _ <- mirror.insert
        yield mirror

      case Some(mirror) =>
        IO.pure:
          mirror

  def allChildren(using db: Database[IO]): IO[List[Account]] =
    db.execute(
      query = sql"""
        with recursive descendants as (
          select guid from accounts where parent_guid = $text
          union all
          select accounts.guid
          from accounts
          join descendants on accounts.parent_guid = descendants.guid
        )
        ${Account.selectAccountsWithFlags}
        where accounts.guid in (select guid from descendants)
      """.query:
        Account.decoder
      ,
      args = guid
    )

  def hiddenChildren(
      archiveSubroot: Account
  )(using db: Database[IO]): IO[List[Account]] = for
    directChildren <- directChildren
    hiddenChildren <- directChildren.traverse: child =>
      if child == archiveSubroot then
        // Hidden or not, we don't want to include anything already in the
        // archive subroot - because the purpose of this function is to discover
        // those which do need moving to it.
        IO.pure:
          Nil
      else if child.hidden then
        // Children of a hidden account are implicitly hidden, i.e. we don't
        // want to include them in the results, hence we don't recurse (but we
        // do include this child).
        IO.pure:
          child :: Nil
        // While this account is not hidden, it may have hidden children.
      else
        child.hiddenChildren:
          archiveSubroot
  yield hiddenChildren.flatten

  // Moves and renames share one updater. hidden has its own (updateHidden)
  // because it also owns a KVP slot; placeholder is still set only at insert
  // time and never toggled.
  def update(parent: Account, name: String = this.name)(using
      db: Database[IO]
  ): IO[Account] =
    db.execute(
      query = sql"""
        update accounts
        set parent_guid = $text, name = $text
        where guid = $text
      """.command,
      args = (parent.guid, name, guid)
    ).as(
      copy(
        parentGuid = Some(parent.guid),
        name = name
      )
    )

  // GnuCash reads hidden from the KVP slot and treats the column as a cache
  // (see selectAccountsWithFlags), and a false flag is the slot's absence —
  // so clearing deletes the slot rather than writing "false".
  def updateHidden(hidden: Boolean)(using db: Database[IO]): IO[Account] =
    for
      _ <- db.execute(
        query = sql"""
          update accounts
          set hidden = $boolean
          where guid = $text
        """.command,
        args = (hidden, guid)
      )
      _ <- Slot.delete(objGuid = guid, name = "hidden")
      _ <- IO.whenA(hidden):
        Slot.stringSlot(objGuid = guid, name = "hidden", value = "true").insert
    yield copy(hidden = hidden)

  // description is a plain accounts column, with no KVP slot behind it (unlike
  // hidden and placeholder), so there is nothing to keep in step here. Null
  // rather than the empty string, so a cleared account decodes back the same
  // way as one that never carried a description.
  def clearDescription(using db: Database[IO]): IO[Account] =
    db.execute(
      query = sql"""
        update accounts
        set description = null
        where guid = $text
      """.command,
      args = guid
    ).as(copy(description = None))

  // Every row rather than db.option, only to say something useful when there
  // is more than one: db.option raises a bare "More than 1 row", and same-named
  // siblings are a state this codebase deliberately creates — resolveAssetAccount
  // creates an asset account beside an untagged one rather than adopting it, and
  // GnuCash itself permits the duplicate name. Steady state never reaches here
  // (enforcePlacement short-circuits on an in-place account), but a rename or a
  // move does, as does a pair a user made by hand on a canonical path — and
  // then the run has to name what it found.
  def child(name: String)(using db: Database[IO]): IO[Option[Account]] =
    db.execute(
      query = sql"""
        ${Account.selectAccountsWithFlags}
        where accounts.parent_guid = $text
          and accounts.name = $text
      """.query:
        Account.decoder
      ,
      args = (guid, name)
    ).flatMap:
      case Nil          => IO.none
      case child :: Nil => IO.pure(Some(child))
      case children     =>
        pathString.flatMap: parentPath =>
          IO.raiseError:
            Error(
              s"${children.size} accounts are named $name under $parentPath; resolve by hand — merge them or rename all but one, since nothing here can tell which of them was meant."
            )

  def pathString(using db: Database[IO]): IO[String] =
    path.map(
      _.map(_.name)
        .mkString:
          "/"
    )

  def path(using db: Database[IO]): IO[List[Account]] =
    db.execute(
      query = sql"""
        with recursive ancestors as (
          select guid, parent_guid, 0 as depth
          from accounts
          where guid = $text
          union all
          select accounts.guid, accounts.parent_guid, ancestors.depth + 1
          from accounts
          join ancestors on accounts.guid = ancestors.parent_guid
        )
        ${Account.selectAccountsWithFlags}
        join ancestors on ancestors.guid = accounts.guid
        order by ancestors.depth desc
      """.query:
        Account.decoder
      ,
      args = guid
    )

  def insert(using db: Database[IO]): IO[Unit] =
    for
      _ <- db.execute(
        // Columns named rather than positional, as in Split.insert and
        // Slot.insert: a GnuCash release that adds one to this table would
        // otherwise shift every value along by a column.
        query = sql"""
          insert into accounts (
            guid,
            name,
            account_type,
            commodity_guid,
            commodity_scu,
            non_std_scu,
            parent_guid,
            code,
            description,
            hidden,
            placeholder
          )
          values (
            $text,
            $text,
            $text,
            ${text.opt},
            $integer,
            $integer,
            ${text.opt},
            ${text.opt},
            ${text.opt},
            $boolean,
            $boolean
          )
        """.command,
        args = (
          guid,
          name,
          accountType,
          commodityGuid,
          commodityScu,
          nonStdScu,
          parentGuid,
          code,
          description,
          hidden,
          placeholder
        )
      )
      // Write the slot too (the column above is just GnuCash's cache); a false
      // flag is the slot's absence, so only write when true. See Slot.scala.
      _ <- IO.whenA(hidden):
        Slot.stringSlot(objGuid = guid, name = "hidden", value = "true").insert
      _ <- IO.whenA(placeholder):
        Slot
          .stringSlot(objGuid = guid, name = "placeholder", value = "true")
          .insert
    yield ()

  def delete(using db: Database[IO]): IO[Unit] =
    for
      // Drop all of the account's slots, not just hidden/placeholder: slots
      // have no foreign key to accounts, so any left behind would be orphaned.
      _ <- Slot.deleteAll(guid)
      _ <- db.execute(
        query = sql"""
          delete from accounts
          where guid = $text
        """.command,
        args = guid
      )
    yield ()

  def directChildren(using db: Database[IO]): IO[List[Account]] =
    db.execute(
      query = sql"""
        ${Account.selectAccountsWithFlags}
        where accounts.parent_guid = $text
        order by accounts.name
      """.query:
        Account.decoder
      ,
      args = guid
    )

  def parent(using db: Database[IO]): IO[Option[Account]] =
    db.option(
      query = sql"""
        ${Account.selectAccountsWithFlags}
        where accounts.guid = ${text.opt}
      """.query:
        Account.decoder
      ,
      args = parentGuid
    )

// GnuCash GUIDs are 32-char hex with the UUID dashes stripped.
val newGuid: IO[String] =
  UUIDGen[IO].randomUUID.map(_.toString.replaceAll("-", ""))
