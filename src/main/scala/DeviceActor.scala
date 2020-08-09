import akka.actor.Status.Failure
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.AskTimeoutException
import akka.util.Timeout
import faulttolerance.circuitbreaker.Service.{Request, Response}
import scala.concurrent.duration._
import akka.pattern.ask
import akka.pattern.pipe
import scala.language.postfixOps
import scala.concurrent.duration.FiniteDuration

object DeviceActor {

  def props(service: ActorRef): Props =
    Props(new DeviceActor(service))

}

class DeviceActor(service: ActorRef) extends Actor with ActorLogging{

  import context.dispatcher

  // Max time the user will stick around waiting for a response
  private val userWaitTimeout = Timeout(3 seconds)

  // Send our first request
  sendRequest()

  override def receive: Receive = {
    case Response =>
      println("Got a quick response, I'm a happy actor")
      sendRequest()

    // This timeout happens if the service does not respond after a while
    case Failure(ex: AskTimeoutException) =>
      println("Got bored of waiting, I'm outta here!")
      context.stop(self)

    // This failure happens quickly if the Circuit Breakers are enabled
    case Failure(ex: Exception) =>
      println("Something must be wrong with the server, let me try again in a few seconds")
      sendRequest(5 seconds)

    case other => log.info(s"Got another message: $other")
  }

  private def sendRequest(delay: FiniteDuration = 1 second) = {
    // Send a message, pipe response to ourselves
    context.system.scheduler.scheduleOnce(delay) {
      service.ask(Request)(userWaitTimeout) pipeTo self
    }
  }


}
