package infrastructure.repository

import scala.util.Try

case class InmemoryRepository[State]() extends Repository[State] {
  import scala.collection.mutable
  val internal = mutable.Map.empty[String, State]
  override def get(id: String): Option[State] = internal.get(id)
  override def put(
      id: String,
      state: State
  ): Either[FailureToPersist.type, Unit] = {
    Try {
      internal.put(id, state)
    } match {
      case util.Failure(_) => Left(FailureToPersist)
      case util.Success(_) => Right(())
    }

  }
}

object InmemoryRepository {
  def apply[State] = new InmemoryRepository[State]()
}
