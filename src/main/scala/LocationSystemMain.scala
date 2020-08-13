import TriangulationSystem.TowerTriangulation
import akka.actor.{Actor, ActorRef, ActorSystem, Props}

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.Await
import scala.io.StdIn
import TowersManager.retrieveAvailableTowers
import akka.actor.AbstractActor.Receive

import scala.collection.mutable.ListBuffer

object LocationSystemMain extends App {
  // location search message
  object SearchLocation

  //actor declaration
  implicit val locationSystem = ActorSystem("LocationSystem")
  val towers = retrieveAvailableTowers
  val towerTriangulation = locationSystem.actorOf(Props(new TowerTriangulation(towers)),"triangulation")
  val sateliteLocationService = locationSystem.actorOf(LocationServiceWithCB.props, "SatelliteService")

  // Create the device actors
  val deviceCount = 100
  var devices = new ListBuffer[ActorRef]
  (1 to deviceCount).foreach(i=>{
    devices += createDevice(towerTriangulation,i)})

  // call location per device with a schedule
  devices.zipWithIndex.foreach{
    case(device,i)=>{
      import locationSystem.dispatcher
      locationSystem.scheduler.scheduleOnce(i seconds){
      device ! SearchLocation}}}

  println("System running, press enter to shutdown")
  StdIn.readLine()

  // We're done, shutdown
  Await.result(locationSystem.terminate(), 3 seconds)

  //device creation using the Triangulation service for tower fetching and satelliteLocationService
  private def createDevice(towerTriangulation:ActorRef,i: Int): ActorRef = {
    locationSystem.actorOf(DeviceActor.props(s"$i",towerTriangulation,sateliteLocationService), s"Device$i")
  }
}
