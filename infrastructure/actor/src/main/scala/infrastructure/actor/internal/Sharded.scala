package infrastructure.actor.internal

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.cluster.sharding.{
  ClusterSharding,
  ClusterShardingSettings,
  ShardRegion
}
import akka.util.Timeout
import org.apache.kafka.common.utils.Utils

import java.nio.charset.StandardCharsets
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.reflect.ClassTag

private[actor] object Sharded {

  trait Sharded {
    def shardId: String
    def entityId: String = shardId
  }
  val extractEntityId: ShardRegion.ExtractEntityId = { case s: Sharded =>
    println("EXTRACT ENTITY ID")
    println((s.entityId, s))
    (s.entityId, s)
  }

  val nodes = 3
  val numberOfShards = 3 * 10
  val numberOfKafkaPartitions = numberOfShards

  val extractShardId: ShardRegion.ExtractShardId = { case s: Sharded =>
    println("EXTRACT SHARD ID")
    println(
      (Utils.toPositive(
        Utils.murmur2(s.shardId.getBytes(StandardCharsets.UTF_8))
      ) % numberOfKafkaPartitions).toString
    )
    (Utils.toPositive(
      Utils.murmur2(s.shardId.getBytes(StandardCharsets.UTF_8))
    ) % numberOfKafkaPartitions).toString
  }

  def apply(name: String, props: Props)(implicit
      system: ActorSystem
  ): ActorRef =
    ClusterSharding(system).start(
      typeName = name,
      entityProps = props,
      settings = ClusterShardingSettings(system),
      extractEntityId = extractEntityId,
      extractShardId = extractShardId
    )

  case class Message(id: String, msg: Any) extends Sharded {
    override val shardId: String = id
  }

  class ActorRefWithAsk(name: String, propsBuilder: String => Props)(implicit
      system: ActorSystem
  ) {
    import akka.pattern.ask
    implicit val timeout: Timeout = Timeout(10 seconds)
    class Intermediary(propsBuilder: String => Props) extends Actor {
      var child: Option[ActorRef] = None
      override def receive: Receive = {

        case Message(id, msg) =>
          println(s"Forwarding ${msg} to newChild")
          child match {
            case Some(child) => child.forward(msg)
            case None =>
              val newChild = context.actorOf(propsBuilder(id))
              child = Some(newChild)
              newChild.forward(msg)
          }
        case other => println(s"WTF ${other}")
      }
    }
    val intermediary = Sharded(
      name,
      Props(new Intermediary(propsBuilder))
    )
    def askk[Command, Response: ClassTag](id: String)(
        command: Command
    ): Future[Response] =
      (intermediary.ask(Message(id, command))(timeout)).mapTo[Response]
  }
}
