package plutus

import scala.scalanative.posix.unistd

// See the JVM row's Host.scala for why this is written twice. getpid() is
// already in Scala Native's POSIX bindings, so no @extern forwarder and no
// extra link flags.
object Host:

  def pid: Long = unistd.getpid().toLong
