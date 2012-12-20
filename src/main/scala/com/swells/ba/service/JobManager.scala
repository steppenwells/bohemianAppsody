
package com.swells.ba.service

import akka.actor.{ActorSystem, Props, Actor}
import akka.event.Logging
import akka.routing.RoundRobinRouter
import java.util.Date

object JobSystem {

  val system = ActorSystem("JobSystem")
  val jobQueueActor = system.actorOf(Props[JobQueueActor], name = "jobQueueActor")

}

class JobQueueActor extends Actor {

  lazy val workerPool = context.actorOf(
    Props[WorkerActor].withRouter(RoundRobinRouter(8)).withDispatcher("akka.actor.workerPoolDispatcher")
  )

  val log = Logging(context.system, this)
  val Retry_Threshold = 4

  var queuedJobCount = 0
  var successCount = 0

  def receive = {
    case Enqueue(job) => {
      if (queuedJobCount == successCount) {
        resetCounts
      }
      queuedJobCount = queuedJobCount + 1
      workerPool ! JobMessage(job)
    }

    case Success => successCount = successCount + 1

    case Failure(job, failCount) => {
      if (failCount < Retry_Threshold) {
        log.info("requeuing job %s".format(job.description))
        workerPool ! JobMessage(job, failCount + 1)
      }
    }

    case Status => sender ! JobsStatus(queuedJobCount, successCount)

  }

  def resetCounts {
    queuedJobCount = 0
    successCount = 0
  }
}

class WorkerActor extends Actor {

  val log = Logging(context.system, this)

  def receive = {
    case JobMessage(job, failureCount) => {
      try{
        log.debug("starting job " + job.description)
        val started = new Date().getTime

        job.process
        JobSystem.jobQueueActor ! Success

        log.debug("completed job %s in %s ms".format(job.description, new Date().getTime - started))
      } catch {
        case e => {
          log.info("job %s failed".format(job.description), e)
          JobSystem.jobQueueActor ! Failure(job, failureCount)
        }
      }
    }
  }
}

// messages
case class Enqueue(job: Job)
case class Success()
case class Failure(job: Job, failureCount: Int)
case class Status()
case class JobMessage(job: Job, failureCount: Int = 0)

//responses
case class JobsStatus(queuedJobs: Int, completedJobs: Int) {

  def percentDone = if (queuedJobs > 0) {
    (((queuedJobs - completedJobs) / queuedJobs) * 100).round
  } else 0

  def isRunning = queuedJobs != completedJobs
}

