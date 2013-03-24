package com.swells.sonas.model

import scalax.file.{PathMatcher, Path}
import scalax.io._
import akka.actor.ActorSystem
import akka.agent.Agent
import akka.util.duration._

object Indexes {

  implicit val system = ActorSystem("sonas")

  var knownIndexes: List[NamedMusicIndex] = Nil

  def init = {
    val cacheFiles = Path("cache/index", '/').children(PathMatcher.IsFile).toList
    val jsonFiles = cacheFiles.filter(_.extension == Some("json"))

    val indexes = jsonFiles.map { f =>
      println("loading " + f.name)

      val json = f.inputStream().string
      NamedMusicIndex(f.simpleName, Agent(MusicIndex.fromJson(json)))
    }

    knownIndexes = indexes

    system.scheduler.schedule(1 minute, 1 minute) { flush }
  }

  def flush {
    knownIndexes.foreach { ni =>
      val json = ni.indexInternal.toJson
      val file = Path("cache/index", '/') / (ni.name + ".json")
      file.write(json)

      println("saved " + file.name)
    }
  }

  def apply(name: String) = knownIndexes.find(_.name == name).get

  def refresh(name: String) {
    val index = knownIndexes.find(_.name == name)

    index.foreach{ i =>
      i.indexAgent.close // shut down old agent, this will be replaced.
      val refreshed = NamedMusicIndex(name, Agent(i.indexInternal.refresh))
      knownIndexes = (refreshed :: knownIndexes.filterNot(_.name == name)).sortBy(_.name)
    }

    flush
  }

  def registerIndex(name: String, index: MusicIndex) {
    knownIndexes = (NamedMusicIndex(name, Agent(index)) :: knownIndexes).sortBy(_.name)
    flush
  }
}

case class NamedMusicIndex(name: String, indexAgent: Agent[MusicIndex]) {
  def fileAdded(addedPath: String) { indexAgent send (_.fileAdded(addedPath)) }
  def exclude(path: String) { indexAgent send (_.exclude(path)) }
  def include(path: String) { indexAgent send (_.include(path)) }

  def indexInternal = indexAgent.get

  def index = indexInternal.included
  def excludedIndex = indexInternal.excluded
}