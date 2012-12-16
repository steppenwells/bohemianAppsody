
package com.swells.ba.model

import scalax.file.{PathMatcher, Path}
import org.json4s._
import org.json4s.native.Serialization
import org.json4s.native.Serialization.{read, write}


/*
import org.json4s._
scala> import org.json4s.native.Serialization
scala> import org.json4s.native.Serialization.{read, write}
scala> implicit val formats = Serialization.formats(NoTypeHints)
scala> val ser = write(Child("Mary", 5, None))
scala> read[Child](ser)
 */

case class MusicIndex(root: String, artists: List[ArtistIndex]) {

  def refresh = MusicIndex(Path(root, '/'))

  def toJson = MusicIndex.toJson(this)
}

object MusicIndex {

  implicit val formats = Serialization.formats(NoTypeHints)

  def apply(path: Path) = {
    new MusicIndex(
      path.path,
      path.children(PathMatcher.IsDirectory).toList.map{ArtistIndex(_)}.sortBy(_.name))
  }

  def toJson(index: MusicIndex) = write(index)

  def fromJson(j: String) = read[MusicIndex](j)
}

case class ArtistIndex(root: String, name: String, albums: List[AlbumIndex])

object ArtistIndex{
  def apply(path: Path) = {
    new ArtistIndex(
      path.path,
      path.simpleName,
      path.children(PathMatcher.IsDirectory).toList.map{ p => AlbumIndex(p) }.sortBy(_.name))
  }
}


case class AlbumIndex(root: String, name: String, songs: List[String])

object AlbumIndex{
  def apply(path: Path) = {

    val subFiles = path.children(PathMatcher.IsFile)
    val fileNames = subFiles.toList.map{ p => p.simpleName}

    new AlbumIndex(
      path.path,
      path.simpleName,
      fileNames.sorted)
  }
}
