
package com.swells.ba.service

object JobManager {

  var jobQueue: List[Job] = Nil
  var runningJobs: Map[String, Job] = Map()

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