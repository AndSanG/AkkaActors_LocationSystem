import LocationService.{SatelliteRequest, SatelliteResponse}
import TriangulationSystem.{GetLocationTriangulation, LocationTriangulationResult}
import akka.actor.Status.Failure
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.AskTimeoutException
import akka.util.Timeout

import scala.concurrent.duration._
import akka.pattern.ask
import akka.pattern.pipe

import scala.language.postfixOps
import scala.concurrent.duration.FiniteDuration

object DeviceActor {

  def props(MIN :String,towerTriangulation: ActorRef,service: ActorRef): Props =
    Props(new DeviceActor(MIN,towerTriangulation, service))

}

class DeviceActor(MIN: String,towerTriangulation: ActorRef, satelliteService: ActorRef) extends Actor with ActorLogging{

  import context.dispatcher

  // Max time the user will stick around waiting for a response
  private val userWaitTimeout = Timeout(3 seconds)

  // Send our first request
  // sendRequest() //this launches interaction with server at creation what i want its to call after receive the Towers response
  // on creation should be ask the Tower triangulation
  towerTriangulation ! GetLocationTriangulation(user = s"cell$MIN",replyTo = self)
  override def receive: Receive = {
    //Satellite
    case SatelliteResponse =>
      println("Got a quick response, I'm a happy actor")
      //sendRequest()

    // This timeout happens if the service does not respond after a while
    case Failure(ex: AskTimeoutException) =>
      println("Got bored of waiting, I'm outta here!")
      context.stop(self)

    // This failure happens quickly if the Circuit Breakers are enabled
    case Failure(ex: Exception) =>
      println("Something must be wrong with the server, let me try again in a few seconds")
      //sendRequest(5 seconds)

    //Triangulation Service
    case triangulationResult: LocationTriangulationResult =>
      print(triangulationResult.locations)
      //context.system.terminate() // part of aggregator
      if (triangulationResult.locations.size < 3){
        println("failed attempt, retrying later")
        Thread.sleep(200)
        towerTriangulation ! GetLocationTriangulation(user = "cell",replyTo = self)
      }else{
        sendRequest(SatelliteRequest(triangulationResult.locations))
      }


    case other => log.info(s"Got another message: $other")
  }

  private def sendRequest(satelliteRequest: SatelliteRequest,delay: FiniteDuration = 1 second) = {
    // Send a message, pipe response to ourselves
    context.system.scheduler.scheduleOnce(delay) {
      satelliteService.ask(satelliteRequest)(userWaitTimeout) pipeTo self
    }
  }


}
