package infrastructure.kafka.plain

import akka.actor.ActorSystem
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.stream.{
  KillSwitches,
  OverflowStrategy,
  QueueCompletionResult,
  QueueOfferResult
}
import akka.stream.scaladsl.{Keep, Sink, Source}
import infrastructure.kafka.Kafka.{Config, Error, KeyValue, Success}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{
  StringDeserializer,
  StringSerializer
}
import akka.kafka.scaladsl._
import scala.concurrent.Future
import infrastructure.kafka.Kafka

case class PlainProducer()(implicit system: ActorSystem, config: Config)
    extends Kafka.Producer {

  private implicit val ec = system.dispatcher
  private val sink = Producer
    .plainSink(
      ProducerSettings
        .create(system, new StringSerializer, new StringSerializer)
        .withBootstrapServers(config.bootstrapServers)
    )
  private val sourceQueue = Source
    .queue(config.bufferSize, OverflowStrategy.backpressure)
    .toMat(sink)(Keep.left)
    .run()

  override def publish(
      topic: String,
      keyValue: KeyValue
  ): Future[Unit] = {
    sourceQueue
      .offer(
        new ProducerRecord[String, String](
          topic,
          keyValue.key,
          keyValue.value
        )
      )
      .flatMap {
        case QueueOfferResult.Enqueued =>
          Future.successful(())
        case r: QueueCompletionResult =>
          r match {
            case QueueOfferResult.Failure(cause) =>
              Future.failed(cause)
            case QueueOfferResult.QueueClosed =>
              Future.failed(new Exception("QueueClosed"))
          }
        case QueueOfferResult.Dropped =>
          Future.failed(new Exception("Dropped"))
      }

  }
}
