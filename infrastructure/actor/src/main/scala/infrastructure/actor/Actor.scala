package infrastructure.actor

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.{Config, ConfigFactory}
import infrastructure.actor.internal.BaseActor
import infrastructure.repository.Repository

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.reflect.ClassTag

object Actor {

  private object System {
    val config = ConfigFactory.load()
    implicit lazy val system = akka.actor.ActorSystem("Example", config)
  }
  import System._

  object With {
    def withDispatcher[A](
        executeWith: ExecutionContext => Future[A]
    ): Future[A] =
      executeWith(system.dispatcher)
    def withConfig[A](executeWith: Config => A): A =
      executeWith(config)
    def withSystem[A](executeWith: ActorSystem => A): A =
      executeWith(system)
  }

  def spawn[Command, State, Response: ClassTag](
      name: String,
      empty: State,
      commandHandler: CommandHandler[Command, State, Response]
  )(implicit
      repository: Repository[State]
  ): String => Command => Future[Either[Error, Response]] = {

    def actorBuilder: String => Props = id =>
      Props(
        new BaseActor[Command, State, Response](
          id,
          empty,
          commandHandler
        )
      )

    import infrastructure.actor.internal.Sharded.ActorRefWithAsk
    val spawner =
      new ActorRefWithAsk(name, actorBuilder)
        .askk[Command, Either[Error, Response]] _

    spawner

  }

}
