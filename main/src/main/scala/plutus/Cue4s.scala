package plutus

import cue4s.*

extension [A](completion: Completion[A])
  // Completion#getOrThrow rethrows a cancelled prompt as CompletionError
  // .Interrupted, whose message is null — an opaque crash. Raise an Error with
  // a message so a cancelled prompt is reported like every other failure.
  def getOrRaise: A =
    completion match
      case Completion.Finished(value) =>
        value
      case Completion.Fail(CompletionError.Interrupted) =>
        throw Error("Cancelled.")
      case Completion.Fail(CompletionError.Error(message)) =>
        throw Error(message)
