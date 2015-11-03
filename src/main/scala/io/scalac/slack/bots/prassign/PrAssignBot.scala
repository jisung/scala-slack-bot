package io.scalac.slack.bots.prassign

import io.scalac.slack.MessageEventBus
import io.scalac.slack.bots.AbstractBot
import io.scalac.slack.common._

import scala.util.Random

/**
 * Maintainer: jisung
 */
class PrAssignBot(override val bus: MessageEventBus) extends AbstractBot {
  val welcomes = List("what's up?", "how's going?", "ready for work?", "nice to see you")

  val PrPattern = "(?s).*Pull request submitted by (\\w+)\\n#(\\d+).*".r

  def welcome = Random.shuffle(welcomes).head

  //TODO: ability to set/list people per repositories

  override def act: Receive = {
//    case Command("hello", _, message) =>
//      publish(OutboundMessage(message.channel, s"hello <@${message.user}>,\\n $welcome"))
    case BaseMessage(text, channel, user, _, _) =>
      text match {
        case PrPattern(requester, prId) =>
          val people = List("jisung", "kai4th", "suckgamony", "juyeong")
          val assignedTo = Random.shuffle(people.filter(person => person != requester)).head
          log.debug("assigned to " + assignedTo)
          publish(OutboundMessage(channel, s"""<@$assignedTo>, I think you are the best guy to review PR#$prId"""))
        case _ => //nothing to do!
          log.debug("test doesn't match for PR");
      }

  }

  override def help(channel: String): OutboundMessage = OutboundMessage(channel,
    s"*${name}* will recommend an assignee for Pull-Request when it's been submitted.")
}
