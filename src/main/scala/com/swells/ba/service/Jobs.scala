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
    val src = Path(srcPath, '/')
    val dest = Path(destPath, '/')

    src.copyTo(dest)
  }
}

class DownloadFileJob(srcUrl: String, destPath: String) extends Job {

  def description = "download %s -> %s".format(srcUrl, destPath)

  def process {
//    val src = new java.net.URL("http://www.scala-lang.org").asInput
//    val dest = Path(destPath, '/')
//
//    src.copyDataTo(dest)

    FileUtils.copyURLToFile(new URL(srcUrl), new File(destPath), 5000, 5000)
  }

}

class TestJob(srcPath: String, destPath: String) extends Job {

  def description = "test, pausing %s -> %s".format(srcPath, destPath)

  def process {
    Thread.sleep(Random.nextInt(5000))//TODO actual copy here...

  }
}