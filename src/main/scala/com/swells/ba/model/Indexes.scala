package com.swells.ba.model

import scalax.file.{PathMatcher, Path}
import scalax.io._

object Indexes {

  var knownIndexes: List[NamedMusicIndex] = Nil

  def init = {
    val cacheFiles = Path("cache/index", '/').children(PathMatcher.IsFile).toList
    val jsonFiles = cacheFiles.filter(_.extension == Some("json"))

    val indexes = jsonFiles.map { f =>
      println("loading " + f.name)

      val json = f.inputStream().string
      NamedMusicIndex(f.simpleName, MusicIndex.fromJson(json))
    }

    knownIndexes = indexes
  }

  def flush {
    knownIndexes.foreach { ni =>
      val json = ni.index.toJson
      val file = Path("cache/index", '/') / (ni.name + ".json")
      file.write(json)

      println("saved " + file.name)
    }
  }

  def refresh(name: String) {
    val index = knownIndexes.find(_.name == name)

    index.foreach{ i =>
      val refreshed = NamedMusicIndex(name, i.index.refresh)
      knownIndexes = (refreshed :: knownIndexes.filterNot(_.name == name)).sortBy(_.name)
    }
  }

  def registerIndex(name: String, index: MusicIndex) {
    knownIndexes = (NamedMusicIndex(name, index) :: knownIndexes).sortBy(_.name)
  }
}

case class NamedMusicIndex(name: String, index: MusicIndex)