import TriangulationSystem.{GetTowerLocation, Point, TowerLocationResult}
import akka.actor.{Actor, ActorRef, Props}
import scala.util.Random

object TowerActor {
  def props(point: Point): Props =
    Props(new TowerActor(point))
}

class TowerActor(point: Point) extends Actor{
  val towerLocation = point
  val towerId = self.path.name
  def receive = {
    case GetTowerLocation(user,replyTo)=>{
      //triangulation system timeout is 1 second
      //Thread.sleep(Random.between(950,1200)) // big chance for failure
      Thread.sleep(Random.between(50,1100)) // very little chance for failure
      replyTo ! TowerLocationResult(user, towerLocation, towerId)
    }
  }
}