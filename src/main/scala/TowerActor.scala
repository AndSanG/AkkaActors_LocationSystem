import TriangulationSystem.{GetTowerLocation, Point, TowerLocationResult}
import akka.actor.{Actor, ActorRef, Props}
import scala.util.Random

object TowerActor {
  def props(point: Point): Props =
    Props(new TowerActor(point))
}

class TowerActor(point: Point) extends Actor{
  val towerLocation = point
  def receive = {
    case GetTowerLocation(user,replyTo)=>{
      //triangulation system timeout is 1 second
      Thread.sleep(Random.between(100,2000))
      replyTo ! TowerLocationResult(user, towerLocation)
    }
  }
}