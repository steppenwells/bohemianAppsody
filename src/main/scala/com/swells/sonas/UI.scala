package com.swells.sonas

import model.{NamedMusicIndex, Indexes, MusicIndex}
import org.scalatra._
import scalax.file.{Path, FileSystem}
import service._
import akka.util.Timeout
import akka.util.duration._
import akka.pattern.ask
import org.json4s._
import org.json4s.native.Serialization
import org.json4s.native.Serialization.write
import util.Logging
import akka.dispatch.Await
import scala.util.Random._

class UI extends ScalatraServlet with Logging {

  implicit val formats = Serialization.formats(NoTypeHints)

  get("/") {
    redirect("/indexes")
  }

  get("/indexes") {
    html.indexList.render(Indexes.knownIndexes)
  }

  post("/refreshIndex") {
    val name = params("name")
    Indexes.refresh(name)

    redirect("/indexes")
  }

  post("/addIndex") {

    val name = params("name")
    val rootPath = params("path")

    val index = MusicIndex.apply(Path(rootPath, '/'))
    Indexes.registerIndex(name, index)

    redirect("/indexes")
  }

  get("/diffReport") {
    html.diffReport.render("", "", None, Indexes.knownIndexes, false)
  }

  post("/diffReport") {

    val inIndex = Indexes(params("inIndex"))
    val notInIndex = Indexes(params("notInIndex"))
    val showSongs = params.get("showSongs").map(_.toBoolean).getOrElse(false)

    val diffIndex = inIndex.index - notInIndex.index

    html.diffReport.render(inIndex.name, notInIndex.name, Option(diffIndex), Indexes.knownIndexes, showSongs)
  }

  post("/enqueueCopy") {

    val inIndex = Indexes(params("fromIndex"))
    val notInIndex = Indexes(params("toIndex"))
    val copyPath = params("path")

    val diffIndex = inIndex.index - notInIndex.index
    val copyIndex = diffIndex.subIndex(copyPath)

    val filesToCopy = copyIndex.files

    val jobs = filesToCopy.map{ file => new CopyJob(file, calculateDestination(notInIndex.index.root, inIndex.index.root, file))}
    jobs foreach { j => JobSystem.jobQueueActor ! Enqueue(j) }
    filesToCopy.mkString("[", ",", "]")

  }

  get("/jobProgress") {
    implicit val timeout = Timeout(1 seconds)

    val jobStatus = JobSystem.jobQueueActor ? Status
    Await.result(jobStatus, 1 seconds) match {
      case status: JobsStatus => write(status)
      case _ =>
    }
  }

  get("/imageFind") {
    html.artPicker.render("", "", "", "", None, Indexes.knownIndexes)
  }

  post("/imageFind") {
    val inIndex = Indexes(params("inIndex"))
    displaySingleImagePicker(inIndex)

  }

  post("/selectedImage") {
    val inIndex = Indexes(params("inIndex"))
    val albumRoot = params("albumRoot")
    val albumName = params("albumName")
    val imageUrl = params("imageUrl")

    val imageExtension = imageUrl.substring(imageUrl.lastIndexOf(".") + 1)
    val calculatedImagePath = "%s/folder".format(albumRoot)
//    val calculatedImagePath = "test"
    val job = new DownloadFileJob(imageUrl, imageExtension, calculatedImagePath)

    JobSystem.jobQueueActor ! Enqueue(job)

    displaySingleImagePicker(inIndex)
  }


  def displaySingleImagePicker(inIndex: NamedMusicIndex) = {
    val missingImages = inIndex.index.missingArtwork
    val artist = shuffle(missingImages.artists).head

    val album = shuffle(artist.albums).head

    val candidateArtwork = try {
     LastFmApi.getAlbumArtwork(artist.name, album.name)
    } catch {
      case e => {
        log.info("failed to get art for %s, %s".format(artist.name, album.name))
        None
      }
    }
    html.artPicker.render(inIndex.name, artist.name, album.name, album.root,  candidateArtwork, Indexes.knownIndexes)
  }

  def calculateDestination(destRoot: String, srcRoot: String, file: String) = file.replaceFirst(srcRoot, destRoot)
}
