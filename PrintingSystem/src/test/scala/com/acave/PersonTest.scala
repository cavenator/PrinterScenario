package com.acave

import akka.actor.ActorSystem
import akka.actor.Actor
import akka.actor.Props
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuiteLike
import akka.testkit.{TestProbe, TestKit, ImplicitSender}
import scala.concurrent.duration._
import scala.collection.immutable._

import Person._

class PersonTest(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with FunSuiteLike with BeforeAndAfterAll {

          def this() = this(ActorSystem("PersonTest"))

          override def afterAll():Unit = {
                system.shutdown
          }

          import akka.pattern.ask
          import akka.util.Timeout
          import system.dispatcher
          import scala.concurrent.Await
          implicit val timeout = Timeout(2.second)

          test("A person receives documents to print and does not print unless instructed to"){
            val probe = TestProbe()

            val actor = system.actorOf(Person.props(probe.ref))

            actor ! Documents(Document(3))
            
            val future = actor ? GetJobStatus
            assert(Await.result(future, 1 second).asInstanceOf[JobStatus] === JobStatus(Nil, List(Document(3))))
          }

          test("Person needs documents or else they cannot print"){
            val probe = TestProbe()

            val actor = system.actorOf(Person.props(probe.ref))

            actor ! "Print"

            probe.expectNoMsg()
          }

          test("When Person prints a document, it's \"in-progress\" and moved out of the \"remaining\" queue"){
            val probe = TestProbe()

            val actor = system.actorOf(Person.props(probe.ref))

            actor ! Documents(Document(3))

            actor ! "Print"

            probe.expectMsg(Document(3))
            val future1 = actor ? GetJobStatus
            assert(Await.result(future1, 2 second).asInstanceOf[JobStatus] === JobStatus(List(Document(3)),Nil))

            actor ! PrintedDoc

            val future = actor ? GetJobStatus
            assert(Await.result(future, 2 second).asInstanceOf[JobStatus] === JobStatus(Nil,Nil))
          }

          test("A Person can retrieve more documents to print while waiting for an \"In-progress\" job"){

            val probe = TestProbe()

            val actor = system.actorOf(Person.props(probe.ref))

            actor ! Documents(Document(3))

            actor ! "Print"

            probe.expectMsg(Document(3))

            actor ! Documents(Document(4), Document(5))

            val future = actor ? GetJobStatus
            assert(Await.result(future, 2 second).asInstanceOf[JobStatus] === JobStatus(List(Document(3)), List(Document(4), Document(5))))

            actor ! "Print"
            probe.expectMsg(Document(4))

            val future1 = actor ? GetJobStatus
            assert(Await.result(future1, 2 second).asInstanceOf[JobStatus] === JobStatus(List(Document(3),Document(4)), List(Document(5))))
          }

          test("Once a person gets remaining documents back from the printer, all remaining and in-progress jobs are no more"){
            val probe = TestProbe()

            val actor = system.actorOf(Person.props(probe.ref))

            actor ! Documents(Document(3), Document(4), Document(5))

            actor ! "Print"

            probe.expectMsg(Document(3))

            actor ! "Print"

            probe.expectMsg(Document(4))

            actor ! PrintedDoc

            actor ! RemainingDocuments(List(Document(4), Document(5)))

            val future = actor ? GetJobStatus
            assert(Await.result(future, 2 second).asInstanceOf[JobStatus] === JobStatus(Nil, Nil))

          }
   }
