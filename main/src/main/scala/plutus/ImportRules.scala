package plutus

import cats.effect.*
import cats.syntax.all.*
import porcupine.*

// Maps Monzo's own transaction category to a GnuCash account path. A transaction
// whose category isn't in the map — or which carries no category at all — lands
// in `fallback` (Expenses:Uncategorised). Keyed on Monzo's category rather than
// hand-maintained merchant substrings: Monzo already categorises every
// transaction, so the table stays small and stable and needs touching only when
// you want to re-file a whole category.
final case class ImportRules(
    byCategory: Map[String, List[String]],
    fallback: List[String]
):

  def accountPathFor(transaction: monzo.Transaction): List[String] =
    transaction.category
      .flatMap: category =>
        byCategory.get(category.value)
      .getOrElse(fallback)

  // Every distinct target path resolved to a live Account once, up front, so
  // the per-transaction loop is a Map lookup and a missing account is a single
  // clear failure rather than a surprise mid-run.
  def resolve(using db: Database[IO]): IO[Map[List[String], Account]] =
    val paths = (fallback :: byCategory.values.toList).distinct
    paths
      .traverse: path =>
        Account
          .atPath(path)
          .flatMap:
            case Some(account) => IO.pure(path -> account)
            case None          =>
              IO.raiseError(Error(s"No account at ${path.mkString(":")}"))
      .map(_.toMap)

object ImportRules:

  // A starting point — edit freely. Keys are Monzo's category strings; every
  // target account must already exist (they're resolved up front). Categories
  // absent here fall through to Uncategorised, so a new Monzo category surfaces
  // there rather than being mis-filed.
  val default: ImportRules =
    ImportRules(
      byCategory = Map(
        "groceries" -> List("Expenses", "Groceries"),
        "eating_out" -> List("Expenses", "Eating Out"),
        "transport" -> List("Expenses", "Transport"),
        "bills" -> List("Expenses", "Bills"),
        "shopping" -> List("Expenses", "Shopping"),
        "entertainment" -> List("Expenses", "Entertainment"),
        "holidays" -> List("Expenses", "Holidays"),
        "personal_care" -> List("Expenses", "Personal Care")
      ),
      fallback = List("Expenses", "Uncategorised")
    )
