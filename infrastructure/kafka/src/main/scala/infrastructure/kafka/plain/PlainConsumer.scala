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
import akka.kafka.scaladsl.{Consumer => KafkaConsumerr}
import akka.kafka.scaladsl.{Producer => KafkaProducerr}
import scala.concurrent.Future
import infrastructure.kafka.Kafka

case class PlainConsumer()(implicit system: ActorSystem, config: Config)
    extends Kafka.Consumer {
  implicit val ec = system.dispatcher

  override def subscribe(
      topic: String,
      process: KeyValue => Future[Either[Error, Success]]
  ): Kafka.Consumer.Start = {
    val consumerSettings = ConsumerSettings
      .create(system, new StringDeserializer, new StringDeserializer)
      .withBootstrapServers(config.bootstrapServers)
    val stream = KafkaConsumerr
      .plainSource(consumerSettings, Subscriptions.topics(topic))
      .zipWithIndex
      .mapAsync(config.parallelism) { case (consumerRecord, index) =>
        val result =
          process(KeyValue(consumerRecord.key(), consumerRecord.value()))
        result.map {
          case Left(Error(message)) => system.log.error(message)
          case Right(Success()) =>
            system.log.debug(s"Processed ${topic}-${index}")
        }
        result
      }
      .viaMat(KillSwitches.single)(Keep.right)
      .toMat(Sink.ignore)(Keep.both)

    new Kafka.Consumer.Start {
      override def start: Kafka.Consumer.Stop = {
        val running = stream.run()
        new Kafka.Consumer.Stop {
          override def stop: Future[Unit] = {
            val (killswitch, done) = running
            killswitch.shutdown()
            done.map(_ => ())
          }
        }
      }
    }
  }
}
