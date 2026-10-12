package plutus

import cats.effect.IO

import java.time.ZoneId

// The PID this run announces in the book's gnclock row. Scala Native's javalib
// has no ProcessHandle — the gap only shows up at nativeLink, not at compile —
// so this is the one thing the lock needs that has to be written once per
// platform, in the style of the keychain and porcupine boundaries but without
// a module of its own: sbt-projectmatrix already puts src/main/scalajvm and
// src/main/scalanative on the respective rows' sources.
//
// The machine's zone is the other: see the Scala Native row's Host.scala.
object Host:

  def pid: Long = ProcessHandle.current.pid

  // The JVM reads TZ and /etc/localtime itself, and ships a time zone
  // database, so there is nothing to resolve and nothing to warn about.
  def zone: IO[ZoneId] =
    IO.delay(ZoneId.systemDefault)
