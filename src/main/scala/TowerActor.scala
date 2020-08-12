import TriangulationSystem.Point
import akka.actor.{Actor, ActorRef, Props}
import scala.util.Random

case class GetTowerLocation(user:String, replyTo:ActorRef)
case class TowerLocationResult(user:String, point: Point, towerId:String)

object TowerActor {
  def props(point: Point): Props =
    Props(new TowerActor(point))
}


class TowerActor(point: Point) extends Actor{
  val towerLocation: Point = point
  val towerId: String = self.path.name
  def receive = {
    case GetTowerLocation(user,replyTo)=>
      //triangulation system timeout is 1 second
      //Thread.sleep(Random.between(950,1200)) // big chance for failure
      Thread.sleep(Random.between(50,1100)) // very little chance for failure
      replyTo ! TowerLocationResult(user, towerLocation, towerId)
  }
}