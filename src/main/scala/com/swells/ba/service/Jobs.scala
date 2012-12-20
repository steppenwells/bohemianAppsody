package com.swells.ba.service

import com.swells.ba.util.Logging
import java.util.Date

trait Job extends Logging {

  def description: String

  def process: Unit

}