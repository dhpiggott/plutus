package plutus

// The PID this run announces in the book's gnclock row. Scala Native's javalib
// has no ProcessHandle — the gap only shows up at nativeLink, not at compile —
// so this is the one thing the lock needs that has to be written once per
// platform, in the style of the keychain and porcupine boundaries but without
// a module of its own: sbt-projectmatrix already puts src/main/scalajvm and
// src/main/scalanative on the respective rows' sources.
object Host:

  def pid: Long = ProcessHandle.current.pid
