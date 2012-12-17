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
    Indexes.flush

    redirect("/indexes")
  }

  post("/diffReport") {

    val inIndex = Indexes(params("inIndex"))
    val notInIndex = Indexes(params("notInIndex"))

    val diffIndex = inIndex.index - notInIndex.index

    redirect("/indexes")
  }
}
