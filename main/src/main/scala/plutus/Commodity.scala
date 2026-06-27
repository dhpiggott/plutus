package plutus

import cats.effect.*
import porcupine.*
import porcupine.Codec.*

object Commodity:

  // The currency every split's `value` is denominated in. Already present in any
  // GBP book; we only read it, never create it.
  def gbp(using db: Database[IO]): IO[Commodity] =
    db.unique:
      sql"""
        select guid, fraction
        from commodities
        where namespace = 'CURRENCY' and mnemonic = 'GBP'
      """.query:
        (text *: integer *: nil).pmap[Commodity]

/** sqlite> .schema commodities CREATE TABLE commodities( guid text(32) PRIMARY
  * KEY NOT NULL, namespace text(2048) NOT NULL, mnemonic text(2048) NOT NULL,
  * fullname text(2048), cusip text(2048), fraction integer NOT NULL, quote_flag
  * integer NOT NULL, quote_source text(2048), quote_tz text(2048) );
  */
final case class Commodity(guid: String, fraction: Long)
