import TriangulationSystem.{TowerTriangulation}
import akka.actor.{ActorRef, ActorSystem, Props}
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.Await
import scala.io.StdIn
import TowersManager.retrieveAvailableTowers

object LocationSystemMain extends App {
  implicit val locationSystem = ActorSystem("LocationSystem")
  val towers = retrieveAvailableTowers
  val towerTriangulation = locationSystem.actorOf(Props(new TowerTriangulation(towers)),"triangulation")
  val service = locationSystem.actorOf(LocationServiceWithCB.props, "SatelliteService")

  // Create the user actors
  val userCount = 1
  (1 to userCount).foreach(i=>createDevice(towerTriangulation,i))
  // gestion of more ask



  // Let this run until the user wishes to stop
  println("System running, press enter to shutdown")
  StdIn.readLine()

  // We're done, shutdown
  Await.result(locationSystem.terminate(), 3 seconds)

  private def createDevice(towerTriangulation:ActorRef,i: Int): Unit = {
    import locationSystem.dispatcher
    locationSystem.scheduler.scheduleOnce(i seconds) {
      println(i)
      locationSystem.actorOf(DeviceActor.props(s"$i",towerTriangulation,service), s"User$i")
    }
  }
}
