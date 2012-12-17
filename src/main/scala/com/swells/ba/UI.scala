package com.swells.ba

import model.{Indexes, MusicIndex}
import org.scalatra._
import scalax.file.{Path, FileSystem}

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

  post("/diffReport") {

    val inIndex = Indexes(params("inIndex"))
    val notInIndex = Indexes(params("notInIndex"))
    val showSongs = params.get("showSongs").map(_.toBoolean).getOrElse(false)

    val diffIndex = inIndex.index - notInIndex.index

    html.diffReport.render(inIndex.name, notInIndex.name, diffIndex, Indexes.knownIndexes, showSongs)
  }

  post("/enqueueCopy") {

    val inIndex = Indexes(params("fromIndex"))
    val notInIndex = Indexes(params("toIndex"))
    val copyPath = params("path")

    val diffIndex = inIndex.index - notInIndex.index
    val copyIndex = diffIndex.subIndex(copyPath)

    copyIndex.toJson

  }
}
