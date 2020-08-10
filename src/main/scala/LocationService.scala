
import akka.actor.{Actor, ActorContext, ActorLogging, Props}
import akka.pattern.extended.ask

import scala.language.postfixOps
import LocationService._
import SatelliteService.{Job, JobResult, SatelliteManager}
import TriangulationSystem.Point
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Random

object LocationService {
  case class SatelliteRequest(inputPoints: List[Point])
  case class SatelliteResponse(location: Point)
}

trait LocationService {
  self: ActorLogging =>
  // Max count and delays
  private val normalDelay = 100
  private val restartDelay = 32000  // Exercise: Test with < 3000 and > 3000

  protected def callLocationServiceSatellite(context:ActorContext, inputPoints: List[Point]): Future[SatelliteResponse.type] = {
    val satellite = context.actorOf(Props(new SatelliteManager))
    implicit val timeout = Timeout(10.seconds)
    val future = satellite ? (Job(1,inputPoints,_))
    val result = Await.result(future,timeout.duration)
    result match {
      case JobResult(_,location) => SatelliteResponse(location)
    }
    //Here call satellite
    """if(Random.nextDouble() <= 0.9 ) {
      Thread.sleep(normalDelay)
    } else {
      // Service shuts down, takes a while to come back up
      println("!! Service overloaded !! Restarting !!")
      Thread.sleep(restartDelay)
    }"""


  }
}

