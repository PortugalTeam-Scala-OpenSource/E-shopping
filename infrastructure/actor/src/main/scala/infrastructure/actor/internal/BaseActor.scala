package infrastructure.actor.internal

import akka.actor.Actor
import infrastructure.actor.{CommandHandler, Error}
import infrastructure.repository.Repository

private[actor] class BaseActor[Command, State, Response](
    id: String,
    empty: State,
    commandHandler: CommandHandler[Command, State, Response]
)(implicit
    repository: Repository[State]
) extends Actor {

  var internal: State = empty
  implicit val ec = context.system.dispatcher

  override def preStart(): Unit = {
    super.preStart()
    repository.get(id) match {
      case Some(oldState) =>
        internal = oldState
      case None =>
        internal = empty
    }
  }

  override def receive: Receive = { case command: Command =>
    commandHandler(command)(internal) match {
      case Left(error) =>
        sender() ! Left(error)
      case Right((newState, reply)) =>
        repository.put(id, newState) match {
          case Left(value) =>
            sender() ! Left(Error(value.toString))
          case Right(value) =>
            internal = newState
            sender() ! Right(reply(newState))
        }

    }
  }
}
