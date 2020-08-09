import akka.actor.{Actor, ActorRef, ActorSystem, Props, ReceiveTimeout}
import akka.util.Timeout
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.language.postfixOps

object LocationSystem extends App{
  case class Point(x: Int, y: Int)
  case class GetTowerLocation(user:String, replyTo:ActorRef)
  case class TowerLocationResult(user:String, point: Point)

  case class GetLocationTriangulation(user: String, replyTo: ActorRef)
  case class LocationTriangulationResult(user: String, locations: List[Point])

  class LocationTriangulationResultBuilder(user:String){
    println("6: LocationTriangulationResultBuilder Instantiated")
    private var locations = new ListBuffer[Point]
    def addTowerLocation(point:Point) : ListBuffer[Point] = {
      this.locations += point}
    def isComplete() : Boolean = {
      true//locations.size == 3
    }
    def timeout() ={}
    def result() :LocationTriangulationResult = {
      LocationTriangulationResult(user,locations.toList)
    }
  }

  class Tower extends Actor{
    println("1: Tower Instantiated")
    def receive = {
      case GetTowerLocation(user,replyTo)=>{
        println("7: Tower Receive Request")
        val point = Point(1,2)
        Thread.sleep(100)
        println("8: Tower Respond with point")
        replyTo ! TowerLocationResult(user, point)
      }
    }
  }

  class TowerTriangulation(tower:ActorRef) extends Actor {
    println("2: TowerTriangulation Instantiated")
    def receive : Receive = {
      case GetLocationTriangulation(user,replyTo)=>
        println("4: TowerTriangulation Receive Request")
        val aggregator = context.actorOf(Props(new TowerAggregator(user,replyTo)))
        //more instances ?
        tower ! GetTowerLocation(user,aggregator)
    }
  }

  class TowerAggregator(user: String, replyTo: ActorRef) extends Actor{
    println("5: TowerAggregator Instantiated")
    val builder = new LocationTriangulationResultBuilder(user)
    context.setReceiveTimeout(1 seconds)
    def receiveResult : Receive = {
      case TowerLocationResult(user,point) => {
        builder.addTowerLocation(point)}
      case ReceiveTimeout =>
        print("No location")
        builder.timeout()
    }
    def checkComplete:Receive = {
      case msg =>
        println("Aggregator received result")
        if(builder.isComplete()){
          println("Result Complete")
          replyTo ! builder.result
          context.stop(self)
        }
    }

    //this one is called and then it calls the receiveResult
    def receive: Receive = receiveResult andThen checkComplete

  }

  class Runner(towerTriangulation:ActorRef) extends  Actor{
    println("3: Runner Instantiated")
    towerTriangulation ! GetLocationTriangulation("cell",self)

    def receive: Receive = {
      case triRes: LocationTriangulationResult =>
        print(triRes.locations)
        context.system.terminate()
    }
  }

}
