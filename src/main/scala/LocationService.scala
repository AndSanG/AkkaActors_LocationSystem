
import akka.actor.{Actor, ActorContext, ActorLogging, Props}
import akka.pattern.extended.ask

import scala.language.postfixOps
import LocationService._
import SatelliteService.{Job, JobRejected, JobResult, SatelliteManager}
import TriangulationSystem.Point
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Random

object LocationService {
  case class SatelliteRequest(device: String,inputPoints: List[Point])
  case class SatelliteResponse(successful: Boolean, location: Point, request:SatelliteRequest)
}

trait LocationService {
  self: ActorLogging =>

  private val normalDelay = 100
  private val restartDelay = 3200
  protected def callLocationServiceSatellite(context:ActorContext, request: SatelliteRequest): SatelliteResponse = {
    println("Location service connecting with satellite")
    val satellite = context.actorOf(Props(new SatelliteManager))
    implicit val timeout = Timeout(10.seconds)
    val future = satellite ? (Job(1,request.inputPoints,_))
    val result = Await.result(future,timeout.duration)
    result match {
      case JobResult(_,location) => SatelliteResponse(true,location,request)
      case JobRejected(_) => SatelliteResponse(false,Point(0,0),request)
    }

  }
}

