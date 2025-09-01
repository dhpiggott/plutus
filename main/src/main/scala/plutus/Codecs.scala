package plutus

import cats.syntax.all.*
import porcupine.*
import porcupine.Codec.*

val boolean: Codec[Boolean] = integer.imap(_ != 0)(if _ then 1 else 0)
