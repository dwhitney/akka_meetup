package examples

import se.scalablesolutions.akka.remote.RemoteServer
import se.scalablesolutions.akka.actor._
import Actor.Sender.Self

object RemoteActors{
	
	def main(args: Array[String]){
		//create a couple of servers
		val hollywoodServer = new RemoteServer
		hollywoodServer.start("localhost", 9991)
		
		val newYorkServer = new RemoteServer
		newYorkServer.start("localhost", 9992)
		
		//create a couple of actors
		val hollywoodActor = new HollywoodActor
		hollywoodActor.start
		
		val newYorkActor = new NewYorkActor
		newYorkActor.start
		
		//send the hollywoodActor her new script
		hollywoodActor ! "script"
		
	}
}

//this is the HollywoodActor running on port 9991, the HollywoodServer
//it receives a message called "script" then calls its friend in New York,
//waits for the response from the New York message and then prints it
class HollywoodActor extends RemoteActor("localhost", 9991){
	
	def receive = {
		case "script" => 
			println("I got a script! I'm calling my friend in New York!")
			val actors = ActorRegistry.actorsFor(classOf[NewYorkActor])
			(actors(0) !! ("call", 1000)) match {
				case Some(message) => println(message)
				case None => println("She didn't answer")
			}
		case "call" => println("Hello, New York Friend!")
		case _ => println("unknown message received")
	}
}

//this is the NewYorkActor that runs on port 9992, the NewYorkServer
//it waits for a "call" message and responds with a Hello!
class NewYorkActor extends RemoteActor("localhost", 9992){
	
	def receive = {
		case "call" => reply("Hello, Hollywood Friend!")
		case _ => println("unknown message received")
	}
}