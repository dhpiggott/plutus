package plutus

import cats.effect.*
import cats.effect.std.UUIDGen
import cats.syntax.all.*
import porcupine.*
import porcupine.Codec.*

// GnuCash GUIDs are 32-char hex with the UUID dashes stripped.
val newGuid: IO[String] =
  UUIDGen[IO].randomUUID.map(_.toString.replaceAll("-", ""))

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

  def createOrRetrieveArchiveSubroot(using db: Database[IO]): IO[Account] =
    for
      root <- Account.root
      archiveSubroot <- root.createOrRetrieveMirror(
        parent = root,
        name = "Archive",
        hidden = true
      )
    yield archiveSubroot

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

  def pathString(using db: Database[IO]): IO[String] =
    path.map(
      _.map(_.name)
        .mkString:
          "/"
    )

  // (from, to) is the boundary pair: when `from` would appear as a parent it
  // is replaced by `to`. Archiving uses (root, archiveSubroot); restoring
  // uses (archiveSubroot, root).
  def createOrRetrieveMirrorParent(
      from: Account,
      to: Account
  )(using db: Database[IO]): IO[Account] =
    parent.flatMap:
      case None =>
        IO.pure:
          this

      case Some(parent) if parent == from =>
        IO.pure:
          to

      case Some(parent) =>
        for
          grandparentMirror <- parent.createOrRetrieveMirrorParent(
            from = from,
            to = to
          )
          mirrorParent <- parent.createOrRetrieveMirror(
            parent = grandparentMirror,
            name = parent.name
          )
        yield mirrorParent

  // Only parent_guid changes here; hidden/placeholder (both column and slot)
  // are set once at insert time and Plutus never toggles them on an existing
  // account, so there is no slot to keep in sync.
  def update(parent: Account)(using db: Database[IO]): IO[Account] =
    db.execute(
      query = sql"""
        update accounts
        set parent_guid = $text
        where guid = $text
      """.command,
      args = (parent.guid, guid)
    ).as(
      copy(
        parentGuid = Some(parent.guid)
      )
    )

  def child(name: String)(using db: Database[IO]): IO[Option[Account]] =
    db.option(
      query = sql"""
        ${Account.selectAccountsWithFlags}
        where accounts.parent_guid = $text
          and accounts.name = $text
      """.query:
        Account.decoder
      ,
      args = (guid, name)
    )

  def insert(using db: Database[IO]): IO[Unit] =
    for
      _ <- db.execute(
        query = sql"""
          insert
          into accounts
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
