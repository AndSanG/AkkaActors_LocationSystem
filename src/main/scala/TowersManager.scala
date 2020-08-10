import TriangulationSystem.Point
import LocationSystemMain.locationSystem
import akka.actor.ActorRef

import scala.collection.mutable.ListBuffer

object TowersManager {

  //map with the location of available towers
  //tower 1,2,3 form a congested zone perimeter < 10km
  val towerLocation = Map(
    1 -> Point(8.5,5.0),
    2 -> Point(11.5,5.0),
    3 -> Point(10.0,5.0),
    4 -> Point(2.0,6.0),
    5 -> Point(20.0,7.0),
  )

  def retrieveAvailableTowers: List[ActorRef] ={
    var towers = new ListBuffer[ActorRef]
    towerLocation.foreach{case(key, point)=>{
      val tower = locationSystem.actorOf(TowerActor.props(point),s"Tower$key")
      towers+= tower
    }}
    towers.toList
  }

}
