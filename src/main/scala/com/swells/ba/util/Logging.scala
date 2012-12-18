package com.swells.ba.util

import org.slf4j.Logger

trait Logging {
  val log = Logger.getLogger(getClass.getName)
}
