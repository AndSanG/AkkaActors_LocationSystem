/*
 * Adapted from example by Alejandro Lujan
 * https://github.com/alejandrolujan/AkkaCircuitBreakers
 */
package faulttolerance.circuitbreaker
import akka.actor.{Actor, ActorLogging, Props}
import Service._

object ServiceWithoutCB {
  def props: Props =
    Props(new ServiceWithoutCB)
}

class ServiceWithoutCB extends Actor with ActorLogging with Service {

  override def receive: Receive = {
    case Request =>
      sender() ! callWebService()
  }

}

