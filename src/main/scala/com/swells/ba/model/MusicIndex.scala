
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

  def -(other: MusicIndex) = {
    val diffArtists = artists.flatMap { a =>
      other.getArtist(a.name) match {
        case Some(otherArtist) => {
          val diffArtist = a - otherArtist
          if (diffArtist.albums.size > 0) Some(diffArtist) else None
        }
        case None => Some(a)
      }
    }

    MusicIndex(root, diffArtists)
  }

  def getArtist(name: String) = artists.find(_.name == name)
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

case class ArtistIndex(root: String, name: String, albums: List[AlbumIndex]) {

  def -(other: ArtistIndex) = {
    val diffAlbums = albums.flatMap { a =>
      other.getAlbum(a.name) match {
        case Some(otherAlbum) => {
          val diffAlbum = a - otherAlbum
          if (diffAlbum.songs.size > 0) Some(diffAlbum) else None
        }
        case None => Some(a)
      }
    }

    ArtistIndex(root, name, diffAlbums)
  }

  def getAlbum(name: String) = albums.find(_.name == name)

}

object ArtistIndex{
  def apply(path: Path) = {
    new ArtistIndex(
      path.path,
      path.simpleName,
      path.children(PathMatcher.IsDirectory).toList.map{ p => AlbumIndex(p) }.sortBy(_.name))
  }
}


case class AlbumIndex(root: String, name: String, songs: List[String]) {

  def -(other: AlbumIndex) = {
    val diffSongs = songs.flatMap { song =>
      other.getSong(song) match {
        case Some(_) => None
        case None => Some(song)
      }
    }

    AlbumIndex(root, name, diffSongs)
  }

  def getSong(name: String) = songs.find(_ == name)
}

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
