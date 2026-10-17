package plutus

import java.time.Instant
import java.time.LocalDate
import scala.scalanative.posix.time
import scala.scalanative.posix.timeOps.tmOps
import scala.scalanative.posix.unistd
import scala.scalanative.unsafe.*

// See the JVM row's Host.scala for why this is written twice. getpid() and
// localtime_r() are already in Scala Native's POSIX bindings, so no @extern
// forwarder and no extra link flags.
object Host:

  def pid: Long = unistd.getpid().toLong

  // libc rather than java.time, which can't answer this on Scala Native:
  // scala-java-time hard-codes ZoneId.systemDefault to UTC there, whatever TZ
  // or /etc/localtime say. Its zone data is a separate artifact,
  // scala-java-time-tzdb, left out because it adds 4.4 MB and its data is
  // frozen at 2019c. localtime_r reads TZ, /etc/localtime and the machine's own
  // zoneinfo, the way every other program on it does.
  def localDate(instant: Instant): LocalDate =
    val seconds = stackalloc[time.time_t]()
    !seconds = instant.getEpochSecond.toSize
    val local = stackalloc[time.tm]()
    time.tzset()
    if time.localtime_r(seconds, local) == null then
      throw IllegalArgumentException(s"localtime_r can't convert $instant")
    LocalDate.of(local.tm_year + 1900, local.tm_mon + 1, local.tm_mday)
