package com.acave
import akka.actor._
import scala.util.Random
import scala.concurrent._
import scala.concurrent.duration._

object Person {
 
    case class Document(numOfPages:Int=1)
    case class Documents(docs:Document*)
    case class TimedMessage(duration: FiniteDuration)
    case class RemainingDocuments(docs: List[Document])
    case class JobStatus(inProgress:List[Document], remainingJobs:List[Document])
    case object PrintedDoc
    case object GetJobStatus
    def props(ref: ActorRef) = Props(new Person(ref))
}

//TODO:  Make "In-Progress" and "remaining" into Enums
class Person(printer: ActorRef) extends Actor {
    import Person._
    import context.dispatcher

    var jobsMaps = Map[String, List[Document]]().empty
    var scheduledInterval = new Cancellable{
        override def cancel = true
        override def isCancelled = true
    }

    override def preStart = {
        context.watch(printer)
    }

    def receive:Receive = {
        case Documents(docs @ _*) => jobsMaps = jobsMaps + ("remaining" -> docs.toList)
        case "Print" => val docs = jobsMaps.getOrElse("remaining", Nil)
                        if (!docs.isEmpty){
                            println(self.path + " is going to print a document")
                            val doc = docs.head
                            jobsMaps = jobsMaps.updated("In-Progress", jobsMaps.getOrElse("In-Progress", Nil) :+ doc)
                            jobsMaps = jobsMaps.updated("remaining", docs.tail)
                            printer ! doc
                        } else {
                            if (!scheduledInterval.isCancelled) scheduledInterval.cancel
                        }
        case GetJobStatus => sender ! JobStatus(jobsMaps.getOrElse("In-Progress", Nil), jobsMaps.getOrElse("remaining", Nil))
        case PrintedDoc => jobsMaps = jobsMaps.updated("In-Progress", jobsMaps("In-Progress").tail)
                            println(self.path + "has successfully got a message back")
        case RemainingDocuments(list) => jobsMaps = jobsMaps.updated("In-Progress", Nil)
                                         jobsMaps = jobsMaps.updated("remaining", jobsMaps.getOrElse("remaining", Nil) ++ list)
        case TimedMessage(interval) =>  if (!scheduledInterval.isCancelled) scheduledInterval.cancel
                                        scheduledInterval = context.system.scheduler.schedule(0 second, interval, self, "Print")
        case Terminated(child) => context.unwatch(printer); println(self.path + " has "+jobsMaps.getOrElse("remaining", Nil).size +" remaining documents."); context.stop(self);
        case _ => println(" Person retrieved a message they don't know how to process!")
    }
    
}
