package info.fotm

import akka.actor._
import akka.actor.Actor.Receive
import akka.event.LoggingReceive

object Lobby {
  case object Start
  case class Connect(client: ActorRef)
  case class Connected(client: ActorRef)
  case class Disconnect(client: ActorRef)
  case class Disconnected(client: ActorRef)
}

class Lobby extends Actor {
  def receive: Receive = LoggingReceive {
    case Lobby.Start =>
      context.become(work(Nil))
  }

  def work(connections: List[ActorRef]): Receive = LoggingReceive {
    case Lobby.Connect(client) =>
      println(s"$client connected")
      val updatedConnections = client :: connections
      context.become(work(updatedConnections))
      updatedConnections.foreach { _ ! Lobby.Connected(client) }

    case Lobby.Disconnect(client) =>
      println(s"$client disconnected")
      val updatedConnections = connections diff List(client)
      context.become(work(updatedConnections))
      connections.foreach {_ ! Lobby.Disconnected(client) }
  }
}

class LobbyClient extends Actor {
  def receive: Receive = LoggingReceive {
    case Lobby.Connected(client) =>
      println(s"$client connected...")
      if (client == self)
        println("That's me!")
    case Lobby.Disconnected(client) =>
      println(s"$client connected...")
  }
}

class LobbyApp extends Actor {

  val lobby = context.actorOf(Props[Lobby], "lobby")
  lobby ! Lobby.Start

  def receive: Receive = LoggingReceive {
    case msg => println(msg)
  }

}

class LobbyClientApp extends Actor {

  val lobby = context.actorSelection("akka.tcp://Main@127.0.0.1:2552/user/app/lobby")
  val client = context.actorOf(Props[LobbyClient])

  lobby ! Lobby.Connect(client)

  def receive: Receive = LoggingReceive {
    case msg => println(msg)
  }
  
}
