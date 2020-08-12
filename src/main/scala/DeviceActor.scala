import LocationService.{SatelliteRequest, SatelliteResponse}
import LocationSystemMain.SearchLocation
import TriangulationSystem.{GetLocationTriangulation, LocationTriangulationResult, Point}
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
  private var outstandingRequest = List[Point]()
  val deviceMIN = MIN
  // on creation it asks the Tower triangulation
  def askTriangulationSystem() = towerTriangulation ! GetLocationTriangulation(device = s"device$deviceMIN",replyTo = self)
  //askTriangulationSystem()
  override def receive: Receive = {

    //start the location search
    case SearchLocation => askTriangulationSystem()

    //Satellite
    case SatelliteResponse(successful: Boolean, location: Point, request:SatelliteRequest) =>
      if (successful){
        println(s"Got a quick response in device $deviceMIN the location is $location")
        outstandingRequest = List[Point]()
      }else{
        println(s"Satellite rejected the petition for device ${request.device}, trying again in a few seconds")
        sendRequestToSatelliteService(request,5 seconds)
      }

    // This timeout happens if the service does not respond after a while
    case Failure(ex: AskTimeoutException) =>
      println(s" Device $deviceMIN got bored of waiting")
      outstandingRequest = List[Point]()
      context.stop(self)

    // This failure happens quickly if the Circuit Breakers are enabled
    case Failure(ex: Exception) =>
      println("Something must be wrong with the Location Service, let me try again in a few seconds")
      sendRequestToSatelliteService(SatelliteRequest(deviceMIN,outstandingRequest),5 seconds)


    //Triangulation Service
    case triangulationResult: LocationTriangulationResult =>
      outstandingRequest = triangulationResult.locations
      if (triangulationResult.locations.size < 3){
        println(s"failed attempt in device $deviceMIN, retrying later")
        Thread.sleep(200)
        towerTriangulation ! GetLocationTriangulation(device = s"device$deviceMIN",replyTo = self)
      }else{
        println(s"Triangulation complete for device $deviceMIN: ${triangulationResult.locations}")
        sendRequestToSatelliteService(SatelliteRequest(deviceMIN, triangulationResult.locations))
      }
    case other => println(s"Got another message: $other")
  }

  private def sendRequestToSatelliteService(satelliteRequest: SatelliteRequest,delay: FiniteDuration = 1 second) = {
    context.system.scheduler.scheduleOnce(delay) {
      satelliteService.ask(satelliteRequest)(userWaitTimeout) pipeTo self
    }
  }


}
