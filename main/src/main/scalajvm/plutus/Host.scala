package plutus

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// What a run needs from the machine that Scala Native's javalib can't give the
// way the JVM's does, so it is written once per platform, in the style of the
// keychain and porcupine boundaries but without a module of its own:
// sbt-projectmatrix already puts src/main/scalajvm and src/main/scalanative on
// the respective rows' sources.
object Host:

  // The PID this run announces in the book's gnclock row. Scala Native's
  // javalib has no ProcessHandle — the gap only shows up at nativeLink, not at
  // compile.
  def pid: Long = ProcessHandle.current.pid

  // An instant's calendar date in the machine's zone, which post dates are
  // taken in (see neutralPostDate). On Scala Native java.time always answers in
  // UTC; see that row's Host.scala.
  def localDate(instant: Instant): LocalDate =
    LocalDate.ofInstant(instant, ZoneId.systemDefault)
