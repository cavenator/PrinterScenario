package com.acave

import akka.actor.ActorSystem
import akka.actor.Actor
import akka.actor.Props
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuiteLike
import akka.testkit.{TestProbe, TestKit, ImplicitSender}
import scala.concurrent.duration._

import Person._

class PersonTest(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with FunSuiteLike with BeforeAndAfterAll {

          def this() = this(ActorSystem("PersonTest"))

          override def afterAll():Unit = {
                system.shutdown
          }

          test("A Person should be able to send multiple things for the printer to print"){
            val testPrinter = new TestProbe(_system){
                def expectMsgCount(x:Int, d: FiniteDuration) = {
                    within(d){
                       for (i <- 0 until x){
                          expectMsgPF(){
                            case Document(y) => sender ! Ready
                          }
                       }
                    }
                    expectNoMsg(200 milliseconds)
                }

            }
            val numPapers = 3
            val actor = system.actorOf(Person.props(testPrinter.ref, numPapers))

            actor ! Start

            testPrinter.expectMsgCount(numPapers, 1 second)
          }

          test("A Person should be notified if the printer fails and is out of order"){

            import akka.pattern.ask
            import akka.util.Timeout
            import system.dispatcher
            import scala.concurrent.Await

            implicit val timeout = Timeout(2.second)

            val testPrinter = new TestProbe(_system){
                def expectOutOfOrderMsgAfterXMsgs(x:Int) = {
                       for (i <- 0 to x ){
                          expectMsgPF(){
                            case Document(y) if i==x => sender ! OutOfOrder
                            case Document(y)  => sender ! Ready
                          }
                       }

                }

            }
            val numPapers = 3
            val actor = system.actorOf(Person.props(testPrinter.ref, numPapers))

            for (i <- 0 until numPapers){
                actor ! Start
                testPrinter.expectOutOfOrderMsgAfterXMsgs(i)
                val future = actor ? GetRemainingJobs
                val expectedRemainingJobs = numPapers - i
                assert(Await.result(future.map{case RemainingJobs(x) => x }, 1 second).asInstanceOf[Int] === expectedRemainingJobs)
            }
            
          }
   }
