package plutus

import cats.effect.*
import cats.effect.std.UUIDGen

// GnuCash GUIDs are 32-char hex with the UUID dashes stripped. Not any one row
// type's business — accounts, transactions and splits are all identified this
// way — so it lives on its own rather than in whichever of them happened to
// need it first.
val newGuid: IO[String] =
  UUIDGen[IO].randomUUID.map(_.toString.replaceAll("-", ""))
