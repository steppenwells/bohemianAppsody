package com.swells.ba

import model.MusicIndex
import org.scalatra._
import scalax.file.{Path, FileSystem}

class UI extends ScalatraServlet {

  get("/") {
    val p = Path("/Users/steppenwells/Music/iTunes/iTunes Music", '/')
//    val p = Path("/Volumes/Public/Shared Music", '/')
    tree( p )

    <html>
      <body>
        <h1>Hello, world!</h1>
      </body>
    </html>
  }

  post("addIndex") {

    val name = params("name")
    val rootPath = params("path")

    val index = MusicIndex.apply(Path(rootPath, '/'))

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
