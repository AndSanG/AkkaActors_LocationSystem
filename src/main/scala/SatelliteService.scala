import java.math.{MathContext, RoundingMode}

import TriangulationSystem.Point
import akka.actor.{Actor, ActorRef, Props}

import scala.collection.immutable.Queue

object SatelliteService {
  case class Job(id: Long, inputPoints: List[Point], replyTo: ActorRef)
  case class JobRejected(id: Long)
  case class JobResult(id: Long, location: Point)

  case class WorkRequest(worker: ActorRef, items: Int)
  case class DummyWork(count: Int)

  class SatelliteManager extends Actor {

    var workQueue = Queue.empty[Job] //External
    var requestQueue = Queue.empty[WorkRequest]  //Internal

    (1 to 8) foreach (_ => context.actorOf(Props(new Worker(self))))

    def receive = {
      case job @ Job(id, inputPoints, replyTo) =>
        if (requestQueue.isEmpty) {
          if (workQueue.size < 1000) workQueue :+= job
          else replyTo ! JobRejected(id)
        } else {
          val WorkRequest(worker, items) = requestQueue.head
          worker ! job
          if (items > 1) worker ! DummyWork(items - 1)
          requestQueue = requestQueue.drop(1)
        }
      case wr @ WorkRequest(worker, items) =>
        if (workQueue.isEmpty) {
          if (!requestQueue.contains(worker)) requestQueue :+= wr
        } else {
          workQueue.iterator.take(items).foreach(job => worker ! job)
          if (workQueue.size < items) worker ! DummyWork(items - workQueue.size)
          workQueue = workQueue.drop(items)
        }
    }
  }

  val mc = new MathContext(100, RoundingMode.HALF_EVEN)

  class Worker(manager: ActorRef) extends Actor {
    val plus = BigDecimal(1, mc)
    val minus = BigDecimal(-1, mc)

    var requested = 0
    def request(): Unit =
      if (requested < 5) {
        manager ! WorkRequest(self, 10)
        requested += 10
      }
    def calculateLocation(inputPoints: List[Point]): Point ={
      var x :Double = 0
      var y :Double = 0
      inputPoints.map(point =>{
        x += point.x
        y += point.y
      })
      Point(x/3,y/3)
    }
    request()

    def receive = {
      case Job(id, inputPoints, replyTo) =>
        requested -= 1
        request()
        val locationResult = calculateLocation(inputPoints)
        replyTo ! JobResult(id,locationResult)
      case DummyWork(count) =>
        requested -= count
        request()
    }
  }

}

