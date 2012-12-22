package com.swells.ba

import model.{Indexes, MusicIndex}
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
      case status: JobsStatus => log.info("Status: " + status); write(status)
      case o => log.info("o was: "+ o)
    }
  }

  def calculateDestination(destRoot: String, srcRoot: String, file: String) = file.replaceFirst(srcRoot, destRoot)
}
