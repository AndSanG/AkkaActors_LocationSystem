import LocationSystem.{Runner, Tower, TowerTriangulation}
import akka.actor.{ActorSystem, Props}


import scala.concurrent.Await
import scala.io.StdIn

object LocationSystemMain extends App {
  val locationSystem = ActorSystem("TowerAggregation")
  val tower = locationSystem.actorOf(Props[Tower],"tower")
  val towerTriangulation = locationSystem.actorOf(Props(new TowerTriangulation(tower)),"trinagulator")
  val runner = locationSystem.actorOf(Props(new Runner(towerTriangulation)), "runner")

  //val service = system.actorOf(ServiceWithoutCB.props, "Service")
  val service = locationSystem.actorOf(ServiceWithCB.props, "Service")

  // Create the user actors
  val userCount = 10
  (1 to userCount).foreach(createUser)

  // Let this run until the user wishes to stop
  println("System running, press enter to shutdown")
  StdIn.readLine()

  // We're done, shutdown
  Await.result(system.terminate(), 3 seconds)

  private def createDevice(i: Int): Unit = {
    import system.dispatcher
    system.scheduler.scheduleOnce(i seconds) {
      system.actorOf(UserActor.props(service), s"User$i")
    }
  }
}
