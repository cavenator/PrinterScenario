package com.acave

import akka.actor.ActorSystem
import akka.actor.Actor
import akka.actor.Terminated
import akka.actor.Props
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuiteLike
import akka.testkit.{TestProbe, TestKit, ImplicitSender}
import scala.concurrent.duration._
import scala.collection.immutable._

class PrinterTest(_system: ActorSystem) extends TestKit(_system) with FunSuiteLike with ImplicitSender with BeforeAndAfterAll {

    def this() = this(ActorSystem("PrinterTest"))

    override def afterAll():Unit = {
        system.shutdown
    }

    import akka.pattern.ask
    import akka.util.Timeout
    import system.dispatcher
    import scala.concurrent.Await
    import Person._
    import Printer._
    import PrinterHelper._
    import Tools._

    implicit val timeout = Timeout(2.second)

    test("Happy Path: A printer delegates print jobs to a helper and sends confirmation message back to requestor"){
        val probe = TestProbe()

        val actor = system.actorOf(Printer.props(probeProps(probe)))

        actor ! Document(3)
     
        probe.expectMsg(Page(1, 3))

        actor ! PrintedPage(1)

        probe.expectMsg(Page(2,3))

        actor ! PrintedPage(2)

        probe.expectMsg(Page(3,3))

        val future = actor ? GetRemainingJobs

        assert(Await.result(future, 2 second).asInstanceOf[RemainingJobs].actorToDocumentTuple.length === 1)

        actor ! PrintedPage(3)

        val future1 = actor ? GetRemainingJobs
        assert(Await.result(future1, 2 second).asInstanceOf[RemainingJobs].actorToDocumentTuple.length === 0)
    }

    test("Printer can only print new documents once the in-progress document is complete"){
        val probe = TestProbe()

        val actor = system.actorOf(Printer.props(probeProps(probe)))

        actor ! Document(1)
     
        probe.expectMsg(Page(1, 1))

        actor ! Document(2)

        probe.expectNoMsg(1 second)

        var future = actor ? GetRemainingJobs
        assert(Await.result(future, 2 second).asInstanceOf[RemainingJobs].actorToDocumentTuple.length === 2)

        actor ! PrintedPage(1)

        probe.expectMsg(Page(1,2))

        future = actor ? GetRemainingJobs
        assert(Await.result(future, 2 second).asInstanceOf[RemainingJobs].actorToDocumentTuple.length === 1)
    }

    test("In the event of UndergoMaintenanceException, printer stops all jobs and returns all remaining jobs to their owners (including the one currently being printed)"){
        val probe = TestProbe()

        val actor = system.actorOf(Printer.props(Props(new TestActorStub(probe, true, new UndergoMaintenanceException()))))

        actor ! Document(1)

        probe.expectMsgClass(classOf[Terminated])

        val future = actor ? GetRemainingJobs
        assert(Await.result(future, 2 second).asInstanceOf[RemainingJobs].actorToDocumentTuple.length === 0)
    }

    test("If paper jams up the printer too much within a specific time period, then printer stops"){
        import system.dispatcher

        val probe = TestProbe()

        val actor = system.actorOf(Printer.props(Props(new TestActorStub(probe, true, new PaperJamException()))))

        val cancellableJob = _system.scheduler.schedule(0 second, 1 second, actor, Document(1))
        probe.expectMsgClass(20 second, classOf[Terminated])
        cancellableJob.cancel
    }
}

object Tools {
    import Printer._
    import PrinterHelper._

    class TestActorStub(val probe: TestProbe, val flaky:Boolean, val ex: Exception) extends Actor {

        override def preStart = {
           if (flaky) probe watch self
        }

        override def preRestart(reason:Throwable, message: Option[Any]):Unit = {
            super.preRestart(reason, message)
            if (reason.isInstanceOf[PaperJamException]) self ! message
        }

        def receive = {
            case msg if !flaky => probe.ref forward msg
            case msg if flaky => throw ex
        }
    }

    def probeProps(probe: TestProbe): Props = Props(classOf[TestActorStub], probe, false, new Exception("Not the mamma"))
}

