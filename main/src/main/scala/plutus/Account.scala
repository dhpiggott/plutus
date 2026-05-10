package plutus

import cats.effect.*
import cats.effect.std.UUIDGen
import cats.syntax.all.*
import porcupine.*
import porcupine.Codec.*

object Account:

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
        select *
        from accounts
        where parent_guid is null
          and name = 'Root Account'
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
    // FIXME: Per https://wiki.gnucash.org/wiki/images/8/86/Gnucash_erd.png,
    // these are implemented as slots. Only updating these accounts columns
    // results in inconsistencies between Plutus' view of the world and GnuCash
    // itself.
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
          guid <- UUIDGen[IO].randomUUID
          mirror = copy(
            guid = guid.toString.replaceAll("-", ""),
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
          select * from accounts where parent_guid = $text
          union all
          select accounts.*
          from accounts
          join descendants on accounts.parent_guid = descendants.guid
        )
        select * from descendants
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
          select accounts.*, 0 as depth
          from accounts
          where guid = $text
          union all
          select accounts.*, ancestors.depth + 1
          from accounts
          join ancestors on accounts.guid = ancestors.parent_guid
        )
        select
          guid, name, account_type, commodity_guid, commodity_scu,
          non_std_scu, parent_guid, code, description, hidden, placeholder
        from ancestors
        order by depth desc
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
        select *
        from accounts
        where parent_guid = $text
          and name = $text
      """.query:
        Account.decoder
      ,
      args = (guid, name)
    )

  def insert(using db: Database[IO]): IO[Unit] =
    db.execute(
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

  def delete(using db: Database[IO]): IO[Unit] =
    db.execute(
      query = sql"""
        delete from accounts
        where guid = $text
      """.command,
      args = guid
    )

  def directChildren(using db: Database[IO]): IO[List[Account]] =
    db.execute(
      query = sql"""
        select *
        from accounts
        where parent_guid = $text
        order by name
      """.query:
        Account.decoder
      ,
      args = guid
    )

  def parent(using db: Database[IO]): IO[Option[Account]] =
    db.option(
      query = sql"""
        select *
        from accounts
        where guid = ${text.opt}
      """.query:
        Account.decoder
      ,
      args = parentGuid
    )
