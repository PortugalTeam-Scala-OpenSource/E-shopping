import infrastructure.actor.Actor.spawn
import infrastructure.actor.CommandHandler
import infrastructure.repository.InmemoryRepository

object Main extends App {

  object Counter {
    case class State(counter: Int) {
      def addOne = copy(counter = counter + 1)
    }
    object State {
      def empty = State(0)
    }
    case class Status(state: State)
    case class Increase(index: Int)
    val commandHandler: CommandHandler[Increase, State, Status] = {
      int => state =>
        int match {
          case Increase(index) if index <= 10 =>
            Right(state.addOne, Status)
          case other @ Increase(index) =>
            Right(state, Status)
        }
    }
  }
  import Counter._

  implicit val repository: InmemoryRepository[State] = InmemoryRepository[State]

  val actor = spawn("Counter", Counter.State.empty, Counter.commandHandler)

  import infrastructure.actor.Actor.With._

  withDispatcher { implicit dispatcher =>
    for {
      answerFromA <- actor("counter-a")(Increase(0))
      secondAnswerFromA <- actor("counter-a")(Increase(1))
      answerFromB <- actor("counter-a")(Increase(2))
    } yield {
      println(s"secondAnswerFromA ${secondAnswerFromA}")
      println(s"answerFromB ${answerFromB}")
    }
  }
}
