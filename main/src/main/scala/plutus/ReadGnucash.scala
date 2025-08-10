package plutus

import cats.effect.*
import cats.syntax.all
import cats.syntax.all.*
import com.monovore.decline.*
import porcupine.*
import porcupine.Codec.*

import java.nio.file.Path

private lazy val readGnucashOpts: Opts[IO[Unit]] = Opts.subcommand(
  name = "read-gnucash",
  help = "Read transactions from GnuCash SQLite3 file."
):
  inputOpts.map: input =>
    readGnucash:
      fs2.io.file.Path.fromNioPath:
        input

private lazy val inputOpts: Opts[Path] =
  Opts
    .option[Path](
      "input",
      help =
        "Path to read GnuCash SQLite3 file from. If not specified defaults to Accounts.gnucash in the current directory."
    )
    .orElse:
      Opts:
        Path.of:
          "Accounts.gnucash"

private def readGnucash(input: fs2.io.file.Path): IO[Unit] =
  Database
    .open[IO]:
      input.toString
    .use: db =>
      given Database[IO] = db
      for
        rootAccount <- Accounts.root
        hidden <- rootAccount.allChildren(select = Select.Hidden)
        _ <- Accounts.archive(hidden)
        // _ <- rootAccount.allChildren(print = true, select = Select.All)
        // _ <- rootAccount.allChildren(print = true, select = Select.Hidden)
        // _ <- rootAccount.allChildren(print = true, select = Select.Visible)
        transactions <- Transactions.read
        splits <- Splits.read
      yield ()

enum Select:
  case All
  case Hidden
  case Visible

private object Accounts:

  def root(using db: Database[IO]): IO[Account] =
    db.unique:
      sql"""
        select * from accounts
        where name = 'Root Account'
      """.query:
        (text *: text *: text *: text.opt *: integer *: integer *:
          text.opt *: text.opt *: text.opt *: integer *: integer *:
          nil)
          .pmap[Account]

  def archive(accounts: List[Account])(using db: Database[IO]): IO[Unit] =
    for _ <- (IO.traverse:
      accounts
    ): account =>
      for
        parentPath <- account.parentPath
        _ <- IO.println:
          parentPath
            .map:
              _.name
            .toList
            .mkString("/")
        archiveParent <- createOrRetrieveArchiveParent(parentPath)
        archiveParentPath <- archiveParent.parentPath
        _ <- IO.println:
          archiveParentPath
            .map:
              _.name
            .toList
            .mkString("/")
      yield ()
    // TODO: Move.
    // _ <- hidden.traverse: account =>
    //   db.execute(
    //     query = sql"update accounts set hidden = 0 where guid = $text",
    //     args = account.guid
    //   )
    yield ()

  // TODO: Names.
  def createOrRetrieveArchiveParent(parentPath: List[Account])(using db: Database[IO]): IO[Account] =
    parentPath.reverse match
      case Nil =>
        ???

      case root :: Nil =>
        createOrRetrieveArchiveParent(root, "Archive")

      case parent :: parents =>
        for
          archiveGrandParent <- createOrRetrieveArchiveParent(parents)
          archiveParent <- createOrRetrieveArchiveParent(archiveGrandParent, parent.name)
        yield archiveParent

  def createOrRetrieveArchiveParent(
      parent: Account,
      name: String
  )(using db: Database[IO]): IO[Account] =
    for
      maybeArchiveParent <- db.option(
        query = sql"""
          select * from accounts
          where parent_guid = $text and name = $text
        """.query:
          (text *: text *: text *: text.opt *: integer *: integer *:
            text.opt *: text.opt *: text.opt *: integer *: integer *:
            nil)
            .pmap[Account]
        ,
        args = (parent.guid, name)
      )
      archiveParent <- maybeArchiveParent match
        case Some(archiveParent) =>
          IO.pure:
            archiveParent

        case None =>
          IO.pure:
            // TODO
            Account(
              guid = java.util.UUID.randomUUID().toString.replaceAll("-", ""),
              name = name,
              accountType = "ROOT",
              commodityGuid = None,
              commodityScu = 1,
              nonStdScu = 1,
              parentGuid = Some(parent.guid),
              code = None,
              description = Some(s"Auto-created archive account '$name'"),
              _hidden = 0,
              _placeholder = 1
            )
    yield archiveParent

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
private final case class Account(
    guid: String,
    name: String,
    accountType: String,
    commodityGuid: Option[String],
    commodityScu: Long,
    nonStdScu: Long,
    parentGuid: Option[String],
    code: Option[String],
    description: Option[String],
    _hidden: Long,
    _placeholder: Long
):

  // TODO: Write a custom codec to avoid this?

  def hidden: Boolean = _hidden == 1

  def placeholder: Boolean = _placeholder == 1

  def allChildren(
      // TODO: Don't conflate retrieval and observability.
      print: Boolean = false,
      select: Select = Select.All,
      indent: Int = 0
  )(using
      db: Database[IO]
  ): IO[List[Account]] = for
    directChildren <- directChildren
    allChildrenOfDirectChildren <- directChildren.traverse: directChild =>
      (select, directChild.hidden) match
        // We're selecting only visible accounts, but this one is hidden, so we
        // skip it and all children, because the hidden property applies
        // recursively.
        case (Select.Visible, true) =>
          IO.pure:
            Nil

        // We're selecting only hidden accounts, and this one is not, but it may
        // have children which are, in which case we need to include those.
        case (Select.Hidden, false) =>
          for
            allChildrenOfDirectChild <- directChild.allChildren(
              print = false,
              select = select,
              indent + 2
            )
            _ <- (IO.whenA:
              print && allChildrenOfDirectChild.nonEmpty
            ):
              (directChild.print:
                indent
              ) *>
                directChild.allChildren(print, select, indent + 2).void
          yield allChildrenOfDirectChild

        // TODO: Add special case for hidden accounts such that children are not
        // returned, so the archiving process doesn't needlessly create
        // equivalents.

        // We're either selecting all accounts indiscriminately, or this
        // account's visibility directly matches the selection requested.
        case (_, _) =>
          for
            allChildrenOfDirectChild <- directChild.allChildren(
              print = false,
              select = select,
              indent + 2
            )
            _ <- (IO.whenA:
              print
            ):
              (directChild.print:
                indent
              ) *>
                directChild.allChildren(print, select, indent + 2).void
          yield directChild +: allChildrenOfDirectChild
  yield allChildrenOfDirectChildren.flatten

  def directChildren(using
      db: Database[IO]
  ): IO[List[Account]] =
    db.execute(
      query = sql"""
        select *
        from accounts
        where parent_guid = $text
        order by name
      """.query:
        (text *: text *: text *: text.opt *: integer *: integer *:
          text.opt *: text.opt *: text.opt *: integer *: integer *:
          nil)
          .pmap[Account]
      ,
      args = guid
    )

  def parentPath(using db: Database[IO]): IO[List[Account]] =
    parentGuid match
      case None =>
        IO.pure(Nil)

      case Some(guid) =>
        for
          parent <- db.unique(
            query = sql"""
            select *
            from accounts
            where guid = $text
          """.query:
              (text *: text *: text *: text.opt *: integer *: integer *:
                text.opt *: text.opt *: text.opt *: integer *: integer *:
                nil)
                .pmap[Account]
            ,
            args = guid
          )
          parentParentPath <- parent.parentPath
        yield parentParentPath :+ parent

  def print(indent: Int): IO[Unit] =
    IO.println:
      " " * indent +
        name.take(70 - (indent + 2)).padTo(70 - indent, ' ') +
        s"hidden: ${hidden.toString.padTo(5, ' ')}, " +
        s"placeholder: ${placeholder.toString.padTo(5, ' ')}"

private object Transactions:

  def read(using db: Database[IO]): IO[Map[String, Transaction]] =
    db.stream(
      query = query,
      args = (),
      chunkSize = 1024
    ).map: transaction =>
      transaction.guid -> transaction
    .compile
      .to:
        Map

  val query = sql"""
    select *
    from transactions
  """.query:
    (text *: text *: text *: text.opt *: text.opt *: text.opt *:
      nil)
      .pmap[Transaction]

/** sqlite> .schema transactions CREATE TABLE transactions( guid text(32)
  * PRIMARY KEY NOT NULL, currency_guid text(32) NOT NULL, num text(2048) NOT
  * NULL, post_date text(19), enter_date text(19), description text(2048) );
  * CREATE INDEX tx_post_date_index ON transactions(post_date);
  *
  * @param guid
  * @param currencyGuid
  * @param num
  * @param postDate
  * @param enterDate
  * @param description
  */
private final case class Transaction(
    guid: String,
    currencyGuid: String,
    num: String,
    postDate: Option[String],
    enterDate: Option[String],
    description: Option[String]
)

private object Splits:

  def read(using db: Database[IO]): IO[Map[String, Split]] =
    db.stream(
      query = query,
      args = (),
      chunkSize = 1024
    ).map: split =>
      split.guid -> split
    .compile
      .to:
        Map

  val query = sql"""
    select *
    from splits
  """.query:
    (text *: text *: text *: text *: text *: text *: text.opt *:
      integer *: integer *: integer *: integer *: text.opt *:
      nil)
      .pmap[Split]

/** sqlite> .schema splits CREATE TABLE splits( guid text(32) PRIMARY KEY NOT
  * NULL, tx_guid text(32) NOT NULL, account_guid text(32) NOT NULL, memo
  * text(2048) NOT NULL, action text(2048) NOT NULL, reconcile_state text(1) NOT
  * NULL, reconcile_date text(19), value_num bigint NOT NULL, value_denom bigint
  * NOT NULL, quantity_num bigint NOT NULL, quantity_denom bigint NOT NULL,
  * lot_guid text(32) ); CREATE INDEX splits_tx_guid_index ON splits(tx_guid);
  * CREATE INDEX splits_account_guid_index ON splits(account_guid);
  *
  * @param guid
  * @param txGuid
  * @param accountGuid
  * @param memo
  * @param action
  * @param reconcileState
  * @param reconcileDate
  * @param valueNum
  * @param valueDenom
  * @param quantityNum
  * @param quantityDenom
  * @param lotGuid
  */
private final case class Split(
    guid: String,
    txGuid: String,
    accountGuid: String,
    memo: String,
    action: String,
    reconcileState: String,
    reconcileDate: Option[String],
    valueNum: Long,
    valueDenom: Long,
    quantityNum: Long,
    quantityDenom: Long,
    lotGuid: Option[String]
)
