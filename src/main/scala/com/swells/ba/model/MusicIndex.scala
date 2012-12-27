
package com.swells.ba.model

import scalax.file.{PathMatcher, Path}
import org.json4s._
import org.json4s.native.Serialization
import org.json4s.native.Serialization.{read, write}


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

  def subIndex(path: String) = {
    path match {
      case `root` => this
      case _ => MusicIndex(root, artists.flatMap{ _.subIndex(path)})
    }
  }

  def missingArtwork = MusicIndex(root, artists.flatMap(_.missingArtwork))

  def albums = artists.flatMap(_.albums)
  def files = artists.flatMap(_.files)
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

  def subIndex(path: String) = {
    path match {
      case `root` => Some(this)
      case _ => albums.flatMap {_.subIndex(path)} match {
        case Nil => None
        case as => Some(ArtistIndex(root, name, as))
      }
    }
  }

  def missingArtwork = {
    albums.filterNot(_.hasArtwork) match {
      case Nil => None
      case as => Some(ArtistIndex(root, name, as))
    }
  }

  def files = albums.flatMap(_.files)

}

object ArtistIndex{
  def apply(path: Path) = {
    new ArtistIndex(
      path.path,
      path.name,
      path.children(PathMatcher.IsDirectory).toList.map{ p => AlbumIndex(p) }.sortBy(_.name))
  }
}


case class AlbumIndex(root: String, name: String, songs: List[Song]) {

  def -(other: AlbumIndex) = {
    val diffSongs = songs.flatMap { song =>
      other.getSong(song.name) match {
        case Some(_) => None
        case None => Some(song)
      }
    }

    AlbumIndex(root, name, diffSongs)
  }

  def getSong(name: String) = songs.find(_.name == name)

  def subIndex(path: String) = {
    path match {
      case `root` => Some(this)
      case _ => {
        songs.filter{_.root == path} match {
          case Nil => None
          case s => Some(AlbumIndex(root, name, s))
        }
      }
    }
  }

  lazy val expectedFileNames = AlbumIndex.coverArtFormats.map{ ext => "folder.%s".format(ext) }

  def hasArtwork = songs.exists{ song => expectedFileNames.contains(song.name) }

  def files = songs.map(_.root)
}

object AlbumIndex {
  def apply(path: Path) = {

    val subFiles = path.children(PathMatcher.IsFile)
    val fileNames = subFiles.toList.map{ p => Song(p.path, p.name)}

    new AlbumIndex(
      path.path,
      path.name,
      fileNames.sortBy(_.name))
  }

  val coverArtFormats = List("jpg", "gif")
}

case class Song(root: String, name: String)
