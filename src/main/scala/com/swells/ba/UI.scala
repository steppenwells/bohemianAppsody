package com.swells.ba

import model.{Indexes, MusicIndex}
import org.scalatra._
import scalax.file.{Path, FileSystem}
import service.{Status, Enqueue, JobSystem, CopyJob}
import akka.util.Timeout
import akka.util.duration._
import akka.pattern.ask

class UI extends ScalatraServlet {

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

  get("jobProgress") {
    implicit val timeout = Timeout(1 seconds)

    val jobStatus = JobSystem.jobQueueActor ? Status
    jobStatus
  }

  def calculateDestination(destRoot: String, srcRoot: String, file: String) = file.replaceFirst(srcRoot, destRoot)
}
