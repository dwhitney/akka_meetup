package examples

import se.scalablesolutions.akka.actor._
import scala.actors.Actor._
import Actor.Sender.Self
import se.scalablesolutions.akka.state._
import se.scalablesolutions.akka.stm.Transaction._

object STM{
	
	def main(args: Array[String]){
		lazy val ref = TransactionalState.newRef[Int]
		val stmActor = new STMActor
		stmActor.start
		
		val noStm = (stmActor !! ("no-stm", 60000)).getOrElse(0)
		val stm = (stmActor !! ("with-stm", 60000)).getOrElse(0)
		
		println("Should be 1000: " + noStm);
		println("Should be 1000: " + stm);
		
		stmActor.stop
		()
	}
}

class STMActor extends Actor{
	
	def receive = {
		case "no-stm" =>
			var i = 0
			for(_ <- 1 to 10000000){
				actor{
					i = i + 1
				}
			}
			Thread.sleep(60000)
			reply(i)
		case "with-stm" => 
			lazy val ref = TransactionalState.newRef[Int]
			ref.swap(0)
			for(_ <- 1 to 1000){
				actor{
					atomic{
						val i = ref.get.get
						println(i)
						ref.swap(i + 1)
					}
				}
			}
			Thread.sleep(15000)
			reply(ref.get.get)
			
			
		case _ => println("unknown message received")
	}
	
}