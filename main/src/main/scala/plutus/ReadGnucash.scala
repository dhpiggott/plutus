package plutus

import cats.effect.*
import com.monovore.decline.*
import smithy4s.*
import smithy4s.xml.*

import java.nio.file.Path

private lazy val readGnucashOpts: Opts[IO[Unit]] = Opts.subcommand(
  name = "read-gnucash",
  help = "Read transactions from GnuCash XML file."
):
  inputOpts.map: input =>
    readGnucash(
      fs2.io.file.Path.fromNioPath:
        input
    )

private lazy val inputOpts: Opts[Path] =
  Opts
    .option[Path](
      "input",
      help =
        "Path to read GnuCash XML file from. If not specified defaults to Accounts.gnucash in the current directory."
    )
    .orElse(
      Opts:
        Path.of:
          "Accounts.gnucash"
    )

private def readGnucash(input: fs2.io.file.Path): IO[Unit] =
  for
    bytes <- fs2.io.file
      .Files[IO]
      .readAll:
        input
      .compile
      .to(Array)
    content <- IO.fromEither:
      Xml.read[gnucash.GnuCash](Blob(bytes))
    _ <- IO.println:
      s"Read GnuCash file with book count: ${content.countData} and book ID: ${content.book.id}"
    _ <- IO.println:
      "Round trip looks like this:"
    _ <- printGnucash(content)
  yield ()

private def printGnucash(content: gnucash.GnuCash): IO[Unit] =
  (fs2.Stream:
    fs2.data.xml.XmlEvent
      .XmlDecl(version = "1.0", encoding = Some("utf-8"), standalone = None)
  ++ XmlDocument.documentEventifier
    .eventify:
      XmlDocument.Encoder
        .fromSchema:
          gnucash.GnuCash.schema
        .encode:
          content
  )
    .through:
      fs2.data.xml.render.prettyPrint(width = 60, indent = 4)
    .through:
      fs2.text.utf8.encode
    .through:
      fs2.io.stdout[IO]
    .compile
    .drain
