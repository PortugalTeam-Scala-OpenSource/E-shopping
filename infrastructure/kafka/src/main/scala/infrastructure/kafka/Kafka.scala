package infrastructure.kafka
import scala.concurrent.Future

object Kafka {

  case class Config(
      parallelism: Int = 100,
      bufferSize: Int = 1000,
      bootstrapServers: String = "0.0.0.0:9092"
  )

  case class KeyValue(key: String, value: String)
  case class Error(message: String)
  case class Success()

  trait Consumer {
    def subscribe(
        topic: String,
        process: KeyValue => Future[Either[Error, Success]]
    ): Consumer.Start
  }
  object Consumer {
    trait Start { def start: Stop }
    trait Stop { def stop: Future[Unit] }
  }

  trait Producer {
    def publish(
        topic: String,
        keyValue: KeyValue
    ): Future[Unit]
  }

}
