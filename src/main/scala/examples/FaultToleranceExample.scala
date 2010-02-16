package examples

import se.scalablesolutions.akka.actor._
import Actor.Sender.Self
import se.scalablesolutions.akka.config.ScalaConfig._
import se.scalablesolutions.akka.config._
import se.scalablesolutions.akka.remote.RemoteServer


/**
This shows off supervisor actors.  A worker attempts an HTTP GET request from Worker.productionHostOne, which results in a
java.net.ConnectException.  The supervisor sees this and restarts the worker, which then switches hosts to Worker.productionHostTwo,
which should work.

In order for this example to work your must have two entries in your hosts file.  prod-1 should point somewhere that won't accept a connection,
and prod-2 should point somewhere that will.  Here is my hosts file:

216.34.181.45	prod-2 #this is slashdot.org's ip
127.0.0.1	prod-1 # I have nothing running on port 80 on my local machine

**/
object FaultToleranceExample{
	
	def main(args: Array[String]){
		
		//start the supervisor
		val supervisor = new MySupervisor
		supervisor.start
		
		//start the worker
		val worker = new Worker
		worker.start
		
		//link the supervisor and worker
		supervisor ! SuperviseMe(worker)
		
		//send a Get message, which will fail
		worker ! Get
		
		//wait for the failure to pass and the host to change
		Thread.sleep(5000)
		
		//attempt the GET again, which will work
		worker ! Get

		//stop the actors
		worker.stop
		supervisor.stop
	
		()
	}
	
}

case object Get{}

//the companion class for the worker that has two hosts used by the worker
object Worker{
	val productionHostOne = "http://prod-1"
	val productionHostTwo = "http://prod-2"
}

//actor that receives only one message (Get), which prints out the results of an HTTP GET request to the console
class Worker extends Actor{
	
	private var host = Worker.productionHostOne
	
	//setup the lifeCycle, which is Permanent, meaning on any fault, it will be restarted
	lifeCycle = Some(LifeCycle(Permanent))
	
	def receive = {
		case Get => println(scala.io.Source.fromURL(host).getLines.mkString)
	}
	
	//this method is called before the supervisor restarts the actor.  In this method, we change the host of 
	//the worker to Worker.productionHostTwo, which will allow this actor to continue performing its job
	override def preRestart(reason: Throwable) = {
		println("Restarting after: " + reason.getMessage())
		reason match {
			case e: java.net.ConnectException => 
				println("ConnectionException encountered, changing host")
				host = Worker.productionHostTwo
			case _ => println("unknown exception")
		}
	}

	//this method is called after the actor is restarted - we are just printing out that fact here
	override def postRestart(reason: Throwable) {
	 	println("Worker Restarted")
	}
	
}

case class SuperviseMe(worker: Worker){}

//this class supervises actors.  It accepts the SuperviseMe message which will link the passed actor to the supervisor instance
class MySupervisor extends Actor{
	
	//this supervisor will trap all exceptions and restart the actor
	trapExit = List(classOf[Exception])
	
	//the OneForOneStrategy means that only the actor that threw the exception will be restarted
	//if the AllForOneStrategy had been selected then this supervisor would have restarted all of its actors
	faultHandler = Some(OneForOneStrategy(3, 1000))
		
	def receive = {
		case SuperviseMe(worker: Worker) => link(worker)
		case _ => ()
	}
	
}