import VouchersExample.Voucher.{AddVoucher, RemoveVoucher}
import infrastructure.actor.CommandHandler
import infrastructure.actor.Error
import infrastructure.repository.Repository
import infrastructure.repository.InmemoryRepository

object VouchersExample extends App {

  object Voucher {
    case class VoucherId(id: String)
    case class State(voucherList: Set[VoucherId]) {
      def addVoucher(voucherId: VoucherId) =
        copy(voucherList = voucherList + voucherId)
      def removeVoucher(voucherId: VoucherId) =
        copy(voucherList = voucherList - voucherId)
    }
    object State {
      def empty = State(voucherList = Set.empty)
    }

    sealed trait Commands
    case class AddVoucher(voucherId: VoucherId) extends Commands
    case class RemoveVoucher(voucherId: VoucherId) extends Commands

    sealed trait Responses
    case class Vouchers(voucherList: Set[VoucherId]) extends Responses

    val commandHandler: CommandHandler[Commands, State, Responses] =
      command =>
        state =>
          command match {
            case AddVoucher(voucherId) =>
              Right(
                state.addVoucher(voucherId),
                state => Vouchers(state.voucherList)
              )
            case RemoveVoucher(voucherId)
                if state.voucherList.contains(voucherId) =>
              Right(
                state.removeVoucher(voucherId),
                state => Vouchers(state.voucherList)
              )
            case RemoveVoucher(voucherId) =>
              Left(Error("No such voucher exists"))

          }
  }

  import Voucher._
  import infrastructure.actor.Actor.spawn
  import infrastructure.actor.Actor.With.withDispatcher

  implicit val repository: Repository[Voucher.State] =
    InmemoryRepository[Voucher.State]
  val vouchersActor =
    spawn("Vouchers", Voucher.State.empty, Voucher.commandHandler)

  withDispatcher(implicit dispatcher =>
    for {
      response1stFromA <- vouchersActor("Bussiness-A")(
        AddVoucher(VoucherId("Voucher-A"))
      )
      response1stFromB <- vouchersActor("Bussiness-B")(
        AddVoucher(VoucherId("Voucher-B"))
      )
      response2ndFromA <- vouchersActor("Bussiness-A")(
        RemoveVoucher(VoucherId("Voucher-A"))
      )
      response2ndFromB <- vouchersActor("Bussiness-B")(
        RemoveVoucher(VoucherId("Voucher-B"))
      )
      problem <- vouchersActor("Bussiness-A")(
        RemoveVoucher(VoucherId("Voucher-A"))
      )
    } yield {
      println(s"response1stFromA ${response1stFromA}")
      println(s"response1stFromB ${response1stFromB}")
      println(s"response2ndFromA ${response2ndFromA}")
      println(s"response2ndFromB ${response2ndFromB}")
      println(s"problem ${problem}")
    }
  )
}
