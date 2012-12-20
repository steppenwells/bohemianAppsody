package com.swells.ba.util

import org.slf4j.LoggerFactory

trait Logging {
  val log = LoggerFactory.getLogger(getClass.getName)
}
