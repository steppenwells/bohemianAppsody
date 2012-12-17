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

  def tree(root: Path, depth: Int = 0) {
    val padding = "".padTo(depth * 2, " ")
    if (root.isDirectory) {
      println(padding + root.simpleName + " (dir)")
      root.children().foreach( tree(_, depth + 1))
    } else {
      println(padding + root.simpleName + " (file)")
    }
  }

}
