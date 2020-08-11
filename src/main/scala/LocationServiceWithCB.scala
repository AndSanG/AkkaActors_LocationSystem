import LocationService.SatelliteRequest
import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.CircuitBreaker
import scala.concurrent.duration._
import scala.concurrent.Future
import akka.pattern.pipe
import scala.language.postfixOps

object LocationServiceWithCB {
  def props: Props =
    Props(new LocationServiceWithCB)
}
// This class implements a service that ask the satellite about the location, implements a circuit Breaker
// to prevent errors propagation
class LocationServiceWithCB extends Actor with ActorLogging with LocationService {

  import context.dispatcher

  val breaker =
    new CircuitBreaker(
      context.system.scheduler,
      maxFailures = 1,
      callTimeout = 2 seconds,
      resetTimeout = 10 seconds).
      onOpen(notifyMe("Open")).
      onClose(notifyMe("Closed")).
      onHalfOpen(notifyMe("Half Open"))

  private def notifyMe(state: String): Unit =
    println(s"My CircuitBreaker is now $state")

  override def receive: Receive = {
    case SatelliteRequest(device,inputPoints) =>
      breaker.withCircuitBreaker(Future(callLocationServiceSatellite(context,SatelliteRequest(device,inputPoints)))) pipeTo sender()
  }

}

