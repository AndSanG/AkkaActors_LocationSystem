import TriangulationSystem.Point
import LocationSystemMain.locationSystem
import akka.actor.ActorRef

import scala.collection.mutable.ListBuffer

object TowersManager {

  //map with the location of available towers
  //tower 1,2,3 form a congested zone perimeter < 10km
  val towerLocation = Map(
    "A" -> Point(2.0,2.0),
    "B" -> Point(4.0,2.0),
    "C" -> Point(2.0,4.0),
    "D" -> Point(4.0,4.0),
    "E" -> Point(3.0,6.0),
  )

  //this create the predefined towers to use and its location
  def retrieveAvailableTowers: List[ActorRef] ={
    var towers = new ListBuffer[ActorRef]
    towerLocation.foreach{case(key, point)=>{
      val tower = locationSystem.actorOf(TowerActor.props(point),s"Tower$key")
      towers+= tower
    }}
    towers.toList
  }

}
