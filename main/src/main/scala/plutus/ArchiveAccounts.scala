package plutus

import cats.effect.*
import cats.effect.std.UUIDGen
import cats.syntax.all.*
import com.monovore.decline.*
import porcupine.*
import porcupine.Codec.*

import java.nio.file.Path

// TODO: Add unarchive (as an account level command) too.

lazy val archiveAccountsOpts: Opts[IO[Unit]] = Opts.subcommand(
  name = "archive-accounts",
  help = "Archive hidden accounts."
):
  (verbosityOpts, inputOpts).tupled.map: (verbosity, input) =>
    archiveAccounts(
      verbosity,
      input = fs2.io.file.Path.fromNioPath:
        input
    )

lazy val inputOpts: Opts[Path] =
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

def archiveAccounts(
    verbosity: Verbosity,
    input: fs2.io.file.Path
): IO[Unit] =
  Database
    .open[IO]:
      input.toString
    .use: db =>
      given Database[IO] = db
      for
        root <- Account.root
        archiveSubroot <- root.createOrRetrieveArchiveParent(
          parent = root,
          name = "Archive"
        )
        _ <- (IO.whenA:
          verbosity.intValue >= Verbosity.DEFAULT.intValue
        ):
          IO.println:
            fansi.Color.Green:
              "Finding hidden accounts…"
        hiddenAccounts <- root.hiddenChildren:
          archiveSubroot
        _ <- (IO.traverse:
          hiddenAccounts
        ): hiddenAccount =>
          for
            path <- hiddenAccount.path
            archiveParent <- hiddenAccount.createOrRetrieveArchiveParent(
              root = root,
              archiveSubroot = archiveSubroot
            )
            _ <- hiddenAccount.update(parent = archiveParent, hidden = false)
            archiveParentPath <- archiveParent.path
            _ <- (IO.whenA:
              verbosity.intValue >= Verbosity.DEFAULT.intValue
            ):
              IO.println:
                s"Moved ${hiddenAccount.name} to ${archiveParentPath
                    .map(_.name)
                    .mkString("/")}."
          yield ()
        _ <- (IO.whenA:
          verbosity.intValue >= Verbosity.DEFAULT.intValue
        ):
          IO.println:
            fansi.Color.Green:
              "Finished archiving hidden accounts."
      yield ()

val boolean: Codec[Boolean] = integer.imap(_ != 0)(if _ then 1 else 0)

object Account:

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

  def createOrRetrieveArchiveParent(
      parent: Account,
      name: String
  )(using db: Database[IO]): IO[Account] = parent
    .child(name)
    .flatMap:
      case None =>
        for
          guid <- UUIDGen[IO].randomUUID
          archiveParent = copy(
            guid = guid.toString.replaceAll("-", ""),
            name = name,
            parentGuid = Some(parent.guid),
            hidden = false,
            placeholder = true
          )
          _ <- archiveParent.insert
        yield archiveParent

      case Some(archiveParent) =>
        IO.pure:
          archiveParent

  // TODO: Remove?
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

  // TODO: Remove?
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

  def createOrRetrieveArchiveParent(
      root: Account,
      archiveSubroot: Account
  )(using db: Database[IO]): IO[Account] =
    if this == root then
      // The root account is it's own archive equivalent.
      IO.pure:
        this
    else
      for
        maybeParent <- parent
        archiveParent <- maybeParent match
          case None =>
            // This shouldn't be possible because we've already handled the root
            // account case, which is the only one that can have no parent.
            IO.raiseError:
              Error:
                "Parent account not found."

          // The parent being the root account means this is a subroot account
          // (i.e. one of Assets/Expenses/Equity/Income/Liabilities), and needs to
          // become a child of the Archive subroot, i.e. we need to find or create
          // the Archive subroot (as a child of the root account), and return that
          // as the archive parent.
          case Some(parent) if parent == root =>
            IO.pure:
              archiveSubroot

          // This is somewhere else in the branch between the subroot and the
          // leaf, so we need to find or create the mirror of the parent within
          // the archive subtree.
          case Some(parent) =>
            for
              parentArchiveParent <- parent.createOrRetrieveArchiveParent(
                root = root,
                archiveSubroot = archiveSubroot
              )
              archiveParent <- parent.createOrRetrieveArchiveParent(
                parent = parentArchiveParent,
                name = parent.name
              )
              _ <- (IO.raiseUnless:
                archiveParent.copy(
                  guid = parent.guid,
                  parentGuid = parent.parentGuid,
                  hidden = parent.hidden,
                  placeholder = parent.placeholder
                ) == parent
              ):
                Error:
                  "Conflicting archive parent found."
            yield archiveParent
      yield archiveParent

  def update(parent: Account, hidden: Boolean)(using
      db: Database[IO]
  ): IO[Unit] =
    db.execute(
      query = sql"""
        update accounts
        set parent_guid = $text,
            hidden = $boolean
        where guid = $text
      """.command,
      args = (parent.guid, hidden, guid)
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

  // TODO: Remove?
  def print(indent: Int): IO[Unit] =
    IO.println:
      " " * indent +
        name.take(70 - (indent + 2)).padTo(70 - indent, ' ') +
        s"hidden: ${hidden.toString.padTo(5, ' ')}, " +
        s"placeholder: ${placeholder.toString.padTo(5, ' ')}"

object Transaction:

  // TODO: Remove?
  def read(using db: Database[IO]): IO[Map[String, Transaction]] =
    db.stream(
      query = sql"""
        select *
        from transactions
      """.query:
        decoder
      ,
      args = (),
      chunkSize = 1024
    ).map: transaction =>
      transaction.guid -> transaction
    .compile
      .to:
        Map

  val decoder: Decoder[Transaction] =
    (text *:
      text *:
      text *:
      text.opt *:
      text.opt *:
      text.opt *:
      nil).pmap[Transaction]

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
final case class Transaction(
    guid: String,
    currencyGuid: String,
    num: String,
    postDate: Option[String],
    enterDate: Option[String],
    description: Option[String]
)

object Split:

  // TODO: Remove?
  def read(using db: Database[IO]): IO[Map[String, Split]] =
    db.stream(
      query = sql"""
        select *
        from splits
      """.query:
        decoder
      ,
      args = (),
      chunkSize = 1024
    ).map: split =>
      split.guid -> split
    .compile
      .to:
        Map

  val decoder: Decoder[Split] =
    (text *:
      text *:
      text *:
      text *:
      text *:
      text *:
      text.opt *:
      integer *:
      integer *:
      integer *:
      integer *:
      text.opt *:
      nil).pmap[Split]

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
final case class Split(
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
