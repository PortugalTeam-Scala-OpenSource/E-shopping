package infrastructure.repository

trait Repository[State] {
  def get(id: String): Option[State]
  case object FailureToPersist
  def put(id: String, state: State): Either[FailureToPersist.type, Unit]
}
