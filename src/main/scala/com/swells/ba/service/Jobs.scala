package com.swells.ba.service

import com.swells.ba.util.Logging
import java.util.Date
import util.Random
import scalax.file.Path
import scalax.io.JavaConverters._
import org.apache.commons.io.FileUtils
import java.io.File
import java.net.URL

trait Job extends Logging {

  def description: String

  def process: Unit

}

class CopyJob(srcPath: String, destPath: String) extends Job {

  def description = "copy %s -> %s".format(srcPath, destPath)

  def process {
    FileUtils.copyFile(new File(srcPath), new File(destPath))
  }
}

class DownloadFileJob(srcUrl: String, destPath: String) extends Job {

  def description = "download %s -> %s".format(srcUrl, destPath)

  def process {
    FileUtils.copyURLToFile(new URL(srcUrl), new File(destPath), 5000, 5000)
  }

}

class TestJob(srcPath: String, destPath: String) extends Job {

  def description = "test, pausing %s -> %s".format(srcPath, destPath)

  def process {
    Thread.sleep(Random.nextInt(5000))//TODO actual copy here...

  }
}