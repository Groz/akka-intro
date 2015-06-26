package info.fotm

import akka.actor.{Props, ActorSystem, ActorRef, Actor}
import akka.event.LoggingReceive

class LobbyClient(name: String) extends Actor {
  val lobby = context.actorSelection("akka.tcp://Main@127.0.0.1:57731/user/app")
  lobby ! Lobby.Announce(name)

  def receive: Receive = LoggingReceive {
    // from actors
    case LobbyClient.List(ids: Set[String]) =>
      ids.foreach(println)

    case LobbyClient.Whisper(msg) =>
      println(s"$sender whispers '$msg'")

    case LobbyClient.Ref(actor) =>
      context.become(bound(actor))

    // from console
    case LobbyClient.QueryList =>
      lobby ! Lobby.List

    case LobbyClient.Bind(id) =>
      lobby ! Lobby.Get(id)
  }

  def bound(actor: ActorRef) = LoggingReceive {
    case LobbyClient.Whisper(msg) =>
      println(s"$sender whispers '$msg'")

    case LobbyClient.Send(msg) =>
      actor ! LobbyClient.Whisper(msg)

    case LobbyClient.Unbind =>
      context.unbecome()

  }
}

case object LobbyClient {
  case class Joined(msg: String, ref: ActorRef)
  case class Whisper(msg: String)
  case class List(ids: Set[String])
  case class Ref(actor: ActorRef)

  // console messages
  case object QueryList
  case class Bind(id: String)
  case class Send(msg: String)
  case object Unbind
}

object LobbyClientApp extends App {
  val name = {
    print("Enter name: ")
    readLine()
  }
  println(s"Starting actor with name: $name")

  val actorSystem = ActorSystem("LobbyClientSystem")
  val lobbyClient = actorSystem.actorOf(Props(new LobbyClient(name)), name)

  println(
    """
      | Enter command:
      |   list - lists ids of all connected actors
      |   bind <id> - binds current actor to actor <id>
      |   send <msg> - sends message to bound actor
      |   unbind - unbinds from the bound actor
    """.stripMargin)

  while (true) {
    val ln = readLine()
    println(s"Command: $ln")

    val split = ln.split("\\s+")
    val cmd = split(0)

    cmd match {
      case "list" =>
        lobbyClient ! LobbyClient.QueryList
      case "bind"  =>
        lobbyClient ! LobbyClient.Bind( split(1) )
      case "send" =>
        lobbyClient ! LobbyClient.Send( split(1) )
      case "unbind"  =>
        lobbyClient ! LobbyClient.Unbind
      case _ =>
        println("Unknown command")
    }
  }
}
