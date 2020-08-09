
import akka.actor.{Actor, ActorLogging, Props}
import scala.concurrent.duration._
import akka.pattern.pipe
import LocationService._
import akka.pattern.CircuitBreaker

import scala.concurrent.Future
import scala.util.Random

object LocationService {
  case object Request
  case object Response
}

trait LocationService {
  self: ActorLogging =>

  // Max count and delays
  private val normalDelay = 100
  private val restartDelay = 3200  // Exercise: Test with < 3000 and > 3000

  protected def callLocationServiceSatellite(): Response.type = {
    //Here call satellite
    if(Random.nextDouble() <= 0.9 ) {
      Thread.sleep(normalDelay)
    } else {
      // Service shuts down, takes a while to come back up
      log.error("!! Service overloaded !! Restarting !!")
      Thread.sleep(restartDelay)
    }

    Response
  }
}

object LocationServiceWithCB {
  def props: Props =
    Props(new LocationServiceWithCB)
}

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
    log.warning(s"My CircuitBreaker is now $state")

  override def receive: Receive = {
    case Request =>
      breaker.withCircuitBreaker(Future(callLocationServiceSatellite())) pipeTo sender()
  }

}


