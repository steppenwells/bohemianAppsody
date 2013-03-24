package com.swells.sonas.service

import com.swells.sonas.util.Logging
import java.util.Date
import util.Random
import scalax.file.Path
import scalax.io.JavaConverters._
import org.apache.commons.io.FileUtils
import java.io.File
import java.net.URL
import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.awt.Color
import com.swells.sonas.model.NamedMusicIndex

trait Job extends Logging {

  def description: String

  def process: Unit

}

class CopyJob(srcPath: String, destPath: String, destIndex: NamedMusicIndex) extends Job {

  def description = "copy %s -> %s".format(srcPath, destPath)

  def process {
    FileUtils.copyFile(new File(srcPath), new File(destPath))
    destIndex.fileAdded(destPath)
  }
}

class DownloadFileJob(srcUrl: String, imageExtension: String, destPath: String) extends Job {

  def description = "download %s -> %s".format(srcUrl, destPath)

  def process {
    imageExtension match {
      case "jpg" => FileUtils.copyURLToFile(new URL(srcUrl), new File(destPath+".jpg"), 5000, 5000)
      case "jpeg" => FileUtils.copyURLToFile(new URL(srcUrl), new File(destPath+".jpg"), 5000, 5000)
      case "gif" => FileUtils.copyURLToFile(new URL(srcUrl), new File(destPath+".gif"), 5000, 5000)
      case ext => {
        log.debug("converting " + ext + " to jpg")
        val origImg = ImageIO.read(new URL(srcUrl))

        val rgbImg = new BufferedImage(origImg.getWidth(), origImg.getHeight(), BufferedImage.TYPE_INT_RGB);
        rgbImg.createGraphics().drawImage(origImg, 0, 0, Color.BLACK, null);

        ImageIO.write(rgbImg, "jpg", new File(destPath+".jpg"))
        log.debug("wrote converted file, width was " + rgbImg.getWidth)
      }
    }
  }

}

class TestJob(srcPath: String, destPath: String) extends Job {

  def description = "test, pausing %s -> %s".format(srcPath, destPath)

  def process {
    Thread.sleep(Random.nextInt(5000))//TODO actual copy here...

  }
}