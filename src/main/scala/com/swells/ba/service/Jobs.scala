package com.swells.ba.service

import com.swells.ba.util.Logging
import java.util.Date

trait Job extends Logging {

  def id: String

  def process: Unit

  def execute {
    try {
      log.debug("starting job " + id)
      val started = new Date().getTime

      process
      JobManager.finished(id)

      log.debug("completed job %s in %s ms".format(id, new Date().getTime - started))
    } catch {
      case e => {
        log.info("job %s failed".format(id), e)
        JobManager.errored(id)
      }
    }
  }
}