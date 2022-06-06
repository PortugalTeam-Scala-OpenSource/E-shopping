package infrastructure

package object actor {

  case class Error(message: String)

  type CommandHandler[Command, State, Response] = Command => State => Either[
    Error,
    (State, State => Response)
  ]

}
