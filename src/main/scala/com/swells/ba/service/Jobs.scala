package com.swells.ba.service

import com.swells.ba.util.Logging
import java.util.Date
import util.Random

trait Job extends Logging {

  def description: String

  def process: Unit

}

class CopyJob(srcPath: String, destPath: String) extends Job {

  def description = "copy %s -> %s".format(srcPath, destPath)

  def process {
    Thread.sleep(Random.nextInt(5000))//TODO actual copy here...

  }
}