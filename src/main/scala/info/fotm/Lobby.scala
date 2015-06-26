package info.fotm

import akka.actor._
import akka.event.LoggingReceive

object Lobby {
  case class Announce(id: String)
  case class Get(id: String)
  case object List
}

class Lobby extends Actor {
  context.become( work(Map.empty) )

  def receive: Receive = ???

  def work(connections: Map[String, ActorRef]): Receive = LoggingReceive {

    case Lobby.Announce(id) =>
      val updatedConnections = connections.updated(id, sender)

      context.become( work(updatedConnections) )

      updatedConnections.foreach { kv =>
        val (_, ref) = kv
        ref ! LobbyClient.Joined(id, sender)
      }

    case Lobby.Get(id) =>
      val actorOption = connections.get(id)
      actorOption.map { actor => sender ! LobbyClient.Ref(actor) }

    case Lobby.List =>
      sender ! LobbyClient.List(connections.keySet)

  }
}
