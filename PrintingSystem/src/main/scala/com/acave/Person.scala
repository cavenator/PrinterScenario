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

    var inprogressDocs:List[Document] = Nil
    var remainingDocs:List[Document] = Nil
    var scheduledInterval = new Cancellable{
        override def cancel = true
        override def isCancelled = true
    }

    override def preStart = {
        context.watch(printer)
    }

    def receive:Receive = {
        case Documents(docs @ _*) => remainingDocs = remainingDocs ++ docs.toList
        case "Print" => if (!remainingDocs.isEmpty){
                            println(self.path + " is going to print a document")
                            val doc = remainingDocs.head
                            inprogressDocs = inprogressDocs :+ doc
                            remainingDocs = remainingDocs.tail
                            printer ! doc
                        } else {
                            if (!scheduledInterval.isCancelled) scheduledInterval.cancel
                        }
        case GetJobStatus => sender ! JobStatus(inprogressDocs, remainingDocs)
        case PrintedDoc => inprogressDocs = inprogressDocs.tail
        case RemainingDocuments(list) => inprogressDocs = Nil
                                         remainingDocs = remainingDocs ++ list
        case TimedMessage(interval) =>  if (!scheduledInterval.isCancelled) scheduledInterval.cancel
                                        scheduledInterval = context.system.scheduler.schedule(0 second, interval, self, "Print")
        case Terminated(child) => context.unwatch(printer); println(self.path + " has "+remainingDocs.size +" remaining documents."); context.stop(self);
        case _ => println(" Person retrieved a message they don't know how to process!")
    }
    
}
