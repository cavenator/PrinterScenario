package com.acave
import akka.actor._
import scala.util.Random

object Person {
 
    case class Document(numOfPages:Int=1)
    case class RemainingJobs(numOfJobs:Int=0)
    case object GetRemainingJobs
    case object Connect
    case object OutOfOrder
    case object Start
    case object Ready
    def props(ref: ActorRef, numPapers: Int, maxNrOfPapers: Int) = Props(new Person(ref, numPapers, maxNrOfPapers))
    def props(ref: ActorRef, numPapers: Int) = Props(new Person(ref, numPapers))
    def props(ref: ActorRef) = Props(new Person(ref))
}

class Person(printer: ActorRef, numOfPapers:Int=1, maxNrOfPapers:Int=1) extends Actor {
    import Person._

    val randomizer = new Random
    var remainingJobs = numOfPapers

    def idle:Receive = {
        case Start => context.become(behavior(numOfPapers))
                      self ! Ready
        case GetRemainingJobs => sender ! RemainingJobs(remainingJobs)
        case _ => println("Do not know how to process request")
    }

    def behavior(remaining:Int):Receive = {
        case Ready => if (remaining == 0){
                        context.become(idle)
                      } else {
                        context.become(behavior(remaining - 1))
                        printer ! Document(randomizer.nextInt(maxNrOfPapers)+1)
                      }
        case OutOfOrder => context.become(idle)
                           remainingJobs = remaining + 1
        case _ => println("Specify behavior here")

    }

    def receive:Receive = idle
    
}
