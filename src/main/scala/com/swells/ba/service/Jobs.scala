package com.swells.ba.service

import com.swells.ba.util.Logging
import java.util.Date
import util.Random
import scalax.file.Path

trait Job extends Logging {

  def description: String

  def process: Unit

}

class CopyJob(srcPath: String, destPath: String) extends Job {

  def description = "copy %s -> %s".format(srcPath, destPath)

  def process {
    val src = Path(srcPath, '/')
    val dest = Path(destPath, '/')

    src.copyTo(dest)
  }
}

class TestJob(srcPath: String, destPath: String) extends Job {

  def description = "test, pausing".format(srcPath, destPath)

  def process {
    Thread.sleep(Random.nextInt(5000))//TODO actual copy here...

  }
}