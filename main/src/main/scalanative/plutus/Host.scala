package plutus

import cats.effect.IO
import cats.syntax.all.*

import java.nio.file.Files
import java.nio.file.Paths
import java.time.ZoneId
import java.time.ZoneOffset
import scala.scalanative.posix.unistd

// See the JVM row's Host.scala for why this is written twice. getpid() is
// already in Scala Native's POSIX bindings, so no @extern forwarder and no
// extra link flags.
object Host:

  def pid: Long = unistd.getpid().toLong

  // ZoneId.systemDefault can't answer this here: scala-java-time hard-codes
  // TimeZone.getDefault to UTC on Scala Native, whatever TZ or /etc/localtime
  // say. So this asks them itself, in the order libc does — TZ if it is set,
  // otherwise the zone /etc/localtime links to — and looks the name up in
  // scala-java-time-tzdb, which build.sbt adds to this row only. The JVM row
  // has nothing to warn about, which is why only this one takes Verbosity.
  //
  // UTC with a warning, rather than a failure, when neither names a zone the
  // database knows: TZ can hold a POSIX rule (GMT0BST,M3.5.0/1,M10.5.0) rather
  // than a name, and the database is frozen at whatever its release shipped,
  // so a zone newer than that is unknown to it.
  def zone(using verbosity: Verbosity): IO[ZoneId] =
    IO.delay(machineZone)
      .flatMap:
        case Right(zone) =>
          IO.pure(zone)
        case Left(reason) =>
          warn(s"$reason, so calendar dates are taken in UTC.")
            .as(ZoneOffset.UTC)

  private def machineZone: Either[String, ZoneId] =
    sys.env.get("TZ") match
      // POSIX reads an empty TZ as UTC, so this is the machine's zone rather
      // than one to warn about.
      case Some("") =>
        Right(ZoneOffset.UTC)
      case Some(tz) =>
        region(tz.stripPrefix(":")).toRight:
          s"TZ is $tz, which isn't a time zone this build knows"
      case None =>
        Either
          .catchNonFatal(Files.readSymbolicLink(Paths.get("/etc/localtime")))
          .leftMap: e =>
            s"Can't read the link at /etc/localtime ($e)"
          .flatMap: target =>
            region(target.toString).toRight:
              s"/etc/localtime links to $target, which isn't a time zone this build knows"

  // A zone name, or a path to its file in a zoneinfo directory: macOS links
  // /etc/localtime to /var/db/timezone/zoneinfo/<region>, and TZ may name a
  // file the same way.
  private def region(spec: String): Option[ZoneId] =
    val name = spec.lastIndexOf("zoneinfo/") match
      case -1 => spec
      case i  => spec.substring(i + "zoneinfo/".length)
    Either.catchNonFatal(ZoneId.of(name)).toOption
