
package com.swells.ba.service

object JobManager {

  var jobQueue: List[Job] = Nil
  var runningJobs: Map[String, Job] = Map()

  def enqueue(job: Job) {
    enqueue(List(job))
  }

  def enqueue(jobs: List[Job]) {
    synchronized {
      jobQueue = jobQueue ::: jobs
    }
  }

  def getJob = synchronized {
    jobQueue match {
      case j :: js => {
        jobQueue = js
        runningJobs = runningJobs + (j.id, j)
        Some(j)
      }
      case Nil => None
    }
  }

  def finished(id: String) {
    synchronized{
      runningJobs = runningJobs - id
    }
  }

  def errored(id: String) {
    synchronized {
      val failedJob = runningJobs(id)
      runningJobs = runningJobs - id
      jobQueue = jobQueue :: failedJob :: Nil
    }
  }
}