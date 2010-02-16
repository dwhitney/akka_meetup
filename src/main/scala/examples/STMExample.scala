package examples

import se.scalablesolutions.akka.actor._
import se.scalablesolutions.akka.actor.Actor._

import Actor.Sender.Self
import se.scalablesolutions.akka.state._
import se.scalablesolutions.akka.stm._
import se.scalablesolutions.akka.collection._
import se.scalablesolutions.akka.stm.Transaction._


object STMExample{
	
	def main(args: Array[String]){
		
		STMExampleActor.account.swap(HashTrie[String, Int]("checking" -> 500000, "savings" -> 500000))
		
		val random = new java.util.Random
		var myactors = List[Actor]()
		for(i <- 0 to 10000) myactors = (new STMExampleActor).start :: myactors
		val groupOne = myactors.slice(0, 1000)
		val groupTwo = myactors.slice(1000, 2000)
		val groupThree = myactors.slice(2000, 3000)
		val groupFour = myactors.slice(3000, 4000)
		val groupFive = myactors.slice(4000, 5000)
		val groupSix = myactors.slice(5000, 6000)
		val groupSeven = myactors.slice(6000, 7000)
		val groupEight = myactors.slice(7000, 8000)
		val groupNine = myactors.slice(8000, 9000)
		val groupTen = myactors.slice(9000, 10000)
		
		
		val threadOne = new Thread(){ override def run(){ makeTransfers(groupOne) } }
		val threadTwo = new Thread(){ override def run(){ makeTransfers(groupTwo) } }
		val threadThree = new Thread(){ override def run(){ makeTransfers(groupThree) } }
		val threadFour = new Thread(){ override def run(){ makeTransfers(groupFour) } }
		val threadFive = new Thread(){ override def run(){ makeTransfers(groupFive) } }
		val threadSix = new Thread(){ override def run(){ makeTransfers(groupSix) } }
		val threadSeven = new Thread(){ override def run(){ makeTransfers(groupSeven) } }
		val threadEight = new Thread(){ override def run(){ makeTransfers(groupEight) } }
		val threadNine = new Thread(){ override def run(){ makeTransfers(groupNine) } }
		val threadTen = new Thread(){ override def run(){ makeTransfers(groupTen) } }
		
		threadOne.start
		threadTwo.start
		threadThree.start
		threadFour.start
		threadFive.start
		threadSix.start
		threadSeven.start
		threadEight.start
		threadNine.start
		threadTen.start
		
		()
	}
	
	def makeTransfers(actors: List[Actor]){
		val random = new java.util.Random
		actors.foreach{ a: Actor =>
			for(i <- 1 to 25){
				if(random.nextBoolean) a ! CheckingToSavings(random.nextInt(1000))
				else a ! SavingsToChecking(random.nextInt(1000))
			}
		}
	}
}


case class CheckingToSavings(amount: Int){}
case class SavingsToChecking(amount: Int){}
case object GetAccounts{}

object STMExampleActor{
	val account = TransactionalState.newRef[HashTrie[String, Int]]
	val accumulator = (new Accumulator).start
}

class STMExampleActor extends Actor{
	
	val accumulator = (new Accumulator).start

	def receive = {
		case GetAccounts => STMExampleActor.account.get match {
			case Some(map: Map[String, Int]) => reply(map)
			case None => reply(Map[String, Int]())
		}
		case CheckingToSavings(amount: Int) =>{
			atomic{
				var account = STMExampleActor.account.get.get
				val checking = account("checking")
				val savings = account("savings")
				account = account.update("checking", checking - amount)
				account = account.update("savings", savings + amount)
				STMExampleActor.account.swap(account)
				STMExampleActor.accumulator send None
			}
		}
		case SavingsToChecking(amount: Int) =>{
			atomic{
				var account = STMExampleActor.account.get.get
				val checking = account("checking")
				val savings = account("savings")
				account = account.update("checking", checking + amount)
				account = account.update("savings", savings - amount)
				STMExampleActor.account.swap(account)
				
				STMExampleActor.accumulator send None
			}
		}
		case _ => ()
	}
}

class Accumulator extends Actor{
	var count = 0
	
	def receive = {
		case _ =>
			count = count + 1
			if((count % 1000) == 0) println(count)
			if(count == 250000){
				atomic{
					val accounts = STMExampleActor.account.get.get
					val checking = accounts("checking")
					val savings = accounts("savings")
				
					println("Checking: " + checking)
					println("savings: " + savings)
					println("Total: " + (checking + savings))
				}
			}
	}
}