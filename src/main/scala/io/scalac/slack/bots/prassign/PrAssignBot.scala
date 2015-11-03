package io.scalac.slack.bots.prassign

import com.typesafe.config.{Config, ConfigFactory}
import io.scalac.slack.MessageEventBus
import io.scalac.slack.bots.AbstractBot
import io.scalac.slack.bots.recruitment.Scalac
import io.scalac.slack.common._

import scala.util.Random

/**
 * Maintainer: jisung
 */
class PrAssignBot(repo: UserIdRepository, override val bus: MessageEventBus) extends AbstractBot {

  val pullRequestSubmitPattern = "(?s).*Pull request submitted by .*\\|(\\w+)>.*#(\\d+) .*".r
  val channelPattern = "(?s).*\"channel\":\"(\\w+)\".*".r

  //TODO: ability to set/list people per repositories

  override def act: Receive = {
    case UndefinedMessage(body) =>
      body match {
        case pullRequestSubmitPattern(requester, prId) =>
          val assignedTo = Random.shuffle(repo.people.filter(user => user.githubId != requester)).head.slackId
          val channelPattern(channel) = body
          //log.debug("channel: " + channel)
          //log.debug("assigned to " + assignedTo)
          publish(OutboundMessage(channel, s"""<@$assignedTo> is assigned to PR#$prId"""))
        case _ => //nothing to do!
          //log.debug("test doesn't match for PR");
      }

  }

  override def help(channel: String): OutboundMessage = OutboundMessage(channel,
    s"*${name}* will pick an assignee randomly for Pull-Request")
}

case class UserId (githubId: String, slackId: String)

class UserIdRepository() {
  val conf = ConfigFactory.load()
  val people = conf.getConfigList("git_slack_user_map").toArray.map{
    case c: Config =>
      val person = c.root().toConfig
      UserId(person.getString("git"), person.getString("slack"))
  }.toList
}