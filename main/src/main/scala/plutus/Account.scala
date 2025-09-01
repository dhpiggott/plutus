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
  *
  * @param guid
  * @param name
  * @param accountType
  * @param commodityGuid
  * @param commodityScu
  * @param nonStdScu
  * @param parentGuid
  * @param code
  * @param description
  * @param hidden
  * @param placeholder
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

  def allChildren(using db: Database[IO]): IO[List[Account]] = for
    directChildren <- directChildren
    allChildren <- directChildren.traverse: child =>
      child.allChildren.map:
        child +: _
  yield allChildren.flatten

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

  // TODO: Find a use for this, or remove it.
  def visibleChildren(using db: Database[IO]): IO[List[Account]] = for
    directChildren <- directChildren
    visibleChildren <- directChildren.traverse: child =>
      if child.hidden then
        // Children of a hidden account are implicitly hidden, i.e. we don't
        // want to include them in the results, hence we don't recurse (or
        // include this child).
        IO.pure:
          Nil
      else
        child.visibleChildren.map:
          _ :+ child
  yield visibleChildren.flatten

  def path(using db: Database[IO]): IO[List[Account]] =
    parent.flatMap:
      case None =>
        IO.pure:
          this :: Nil

      case Some(parent) =>
        parent.path.map:
          _ :+ this

  def pathString(using db: Database[IO]): IO[String] =
    path.map(
      _.map(_.name)
        .mkString:
          "/"
    )

  def createOrRetrieveArchiveParent(
      root: Account,
      archiveSubroot: Account
  )(using db: Database[IO]): IO[Account] =
    parent.flatMap:
      case None =>
        // The root account is it's own archive equivalent.
        IO.pure:
          this

      // The parent being the root account means this is a subroot account (i.e.
      // one of Assets/Expenses/Equity/Income/Liabilities), and needs to become
      // a child of the Archive subroot.
      case Some(parent) if parent == root =>
        IO.pure:
          archiveSubroot

      // This is somewhere else in the branch between the subroot and the leaf,
      // so we need to find or create the mirror of the parent within the
      // archive subtree.
      case Some(parent) =>
        for
          parentArchiveParent <- parent.createOrRetrieveArchiveParent(
            root = root,
            archiveSubroot = archiveSubroot
          )
          archiveParent <- parent.createOrRetrieveMirror(
            parent = parentArchiveParent,
            name = parent.name
          )
        yield archiveParent

  def createOrRetrieveNonArchiveParent(
      root: Account,
      archiveSubroot: Account
  )(using db: Database[IO]): IO[Account] =
    parent.flatMap:
      case None =>
        // The root account is it's own archive equivalent.
        IO.pure:
          this

      // The archive subroot is the one we're looking for, and it needs to
      // disappear from the chain.
      case Some(parent) if parent == archiveSubroot =>
        IO.pure:
          root

      // This is somewhere else in the branch between the subroot and the leaf,
      // so we need to find or create the mirror of the parent within the
      // non-archive subtree.
      case Some(parent) =>
        for
          parentNonArchiveParent <- parent.createOrRetrieveNonArchiveParent(
            root = root,
            archiveSubroot = archiveSubroot
          )
          nonArchiveParent <- parent.createOrRetrieveMirror(
            parent = parentNonArchiveParent,
            name = parent.name
          )
        yield nonArchiveParent

  def update(parent: Account, hidden: Boolean)(using
      db: Database[IO]
  ): IO[Account] =
    db.execute(
      query = sql"""
        update accounts
        set parent_guid = $text,
            hidden = $boolean
        where guid = $text
      """.command,
      args = (parent.guid, hidden, guid)
    ).as(
      copy(
        parentGuid = Some(parent.guid),
        hidden = hidden
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
