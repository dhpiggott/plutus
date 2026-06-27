package plutus

import cats.effect.*
import cats.syntax.all.*
import porcupine.*

// First match wins; anything unmatched lands in `fallback` (Expenses ::
// Uncategorised). A Rule is just a predicate over the Monzo transaction paired
// with a target account path — so you can match on merchant, counterparty,
// notes or amount without changing any of the machinery below.
final case class Rule(
    matches: monzo.Transaction => Boolean,
    accountPath: List[String]
)

final case class ImportRules(rules: List[Rule], fallback: List[String]):

  def accountPathFor(transaction: monzo.Transaction): List[String] =
    rules
      .collectFirst:
        case rule if rule.matches(transaction) => rule.accountPath
      .getOrElse(fallback)

  // Every distinct target path resolved to a live Account once, up front, so
  // the per-transaction loop is a Map lookup and a missing account is a single
  // clear failure rather than a surprise mid-run.
  def resolve(using db: Database[IO]): IO[Map[List[String], Account]] =
    val paths = (fallback :: rules.map(_.accountPath)).distinct
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

  // A starting point — edit freely. Matching is case-insensitive substring on
  // the merchant/counterparty/description, the same string export uses for the
  // OFX name. (Monzo's own `category` field isn't in the smithy model yet;
  // adding it would enable category-keyed defaults with these as overrides.)
  val default: ImportRules =
    def payee(transaction: monzo.Transaction): String = transaction.merchant
      .map(_.name)
      .orElse(transaction.counterparty.name)
      .map(_.value)
      .getOrElse(transaction.description.value)
      .toLowerCase

    def merchant(needle: String, path: String*): Rule =
      Rule(t => payee(t).contains(needle.toLowerCase), path.toList)

    ImportRules(
      rules = List(
        merchant("tesco", "Expenses", "Groceries"),
        merchant("sainsbury", "Expenses", "Groceries"),
        merchant("tfl", "Expenses", "Transport"),
        merchant("northern", "Expenses", "Transport"),
        merchant("greggs", "Expenses", "Eating Out")
      ),
      fallback = List("Expenses", "Uncategorised")
    )
