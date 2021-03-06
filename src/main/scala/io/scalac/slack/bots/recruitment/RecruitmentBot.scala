package io.scalac.slack.bots.recruitment

import io.scalac.slack.MessageEventBus
import io.scalac.slack.bots.AbstractBot
import io.scalac.slack.common.{Command, OutboundMessage}

import scala.util.Random
import com.typesafe.config.{Config, ConfigObject, ConfigFactory}

/**
 * Maintainer: Patryk
 */
class RecruitmentBot(repo: EmployeeRepository, override val bus: MessageEventBus) extends AbstractBot {
  import Measurable._

  override def help(channel: String): OutboundMessage = OutboundMessage(channel,
    s"*${name}* is a tool to help find reviewers for candidates \\n" +
      s"`match-candidate {link to candidate} {level, one of junior/medior/senior} {area of focus, one of backend/frontend/mobile }` - " +
        s"find a match for a given candidates among Scalac")

  override def act: Receive = {
    case Command("match-candidate", link :: level :: focus :: _, message) =>
      log.debug(s"Received request to match $link with $level/$focus")

      val result = (levelToDouble(level), focusToDouble(focus)) match {
        case (None, _) =>
          OutboundMessage(message.channel, s"No Level for $level. Use one of junior/medior/senior")
        case (_, None) =>
          OutboundMessage(message.channel, s"No focus for $focus. Use one of backend/frontend/mobile")
        case (Some(levelD), Some(focusD)) =>
          val url = link.replace("<", "").replace(">", "")

          val task = TaskData(url, levelD, focusD)
          val matching = repo.findClosest(task)

          OutboundMessage(message.channel, s"Bot matched ${matching.map(_.name).getOrElse("NO MATCH")} for task $url")
      }

      publish(result)
  }
}

trait Measurable {
  val level: Double
  val focus: Double
}
object Measurable {
  val Junior = 0.0D
  val Medior = 1.0D
  val Senior = 2.0D

  val Mobile = 0.0D
  val Backend = 1.0D
  val Frontend = 2.0D
  
  def levelToDouble(level: String) = level.toLowerCase match {
    case "junior" => Some(Junior)
    case "medior" => Some(Medior)
    case "senior" => Some(Senior)
    case _ => None 
  }

  def focusToDouble(focus: String) = focus.toLowerCase match {
    case "mobile" => Some(Mobile)
    case "backend" => Some(Backend)
    case "frontend" => Some(Frontend)
    case _ => None
  }
}

case class TaskData(
  url: String,
  override val level: Double,
  override val focus: Double
) extends Measurable

case class Scalac(
  name: String,
  override val level: Double,
  override val focus: Double) extends Measurable

class EmployeeRepository() {

  val conf = ConfigFactory.load()
  val people = conf.getConfigList("recruitment.reviewers").toArray.map{
    case c: Config =>
      val person = c.root().toConfig
      Scalac(person.getString("name"), person.getDouble("level"), person.getDouble("focus"))
  }.toList

  val threshold = 1.0D

  val rand = new Random()

  def findClosest(task: TaskData): Option[Scalac] = {
    val withDistnace = people.map(sc => {
      val distance = Math.abs(sc.focus - task.focus) + Math.abs(sc.level - task.level)
      (sc, distance)
    })
    val filtered = withDistnace.filter(_._2 <= threshold).map(_._1)
    rand.shuffle(filtered).headOption
  }
}