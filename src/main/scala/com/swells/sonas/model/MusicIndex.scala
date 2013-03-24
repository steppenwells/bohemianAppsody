
package com.swells.sonas.model

import scalax.file.{PathMatcher, Path}
import org.json4s._
import org.json4s.native.Serialization
import org.json4s.native.Serialization.{read, write}

case class DiffSettings(ignoreArtwork: Boolean, fuzzyMatch: Boolean)

object DiffSettings {
  val FileCopyDiffSettings = DiffSettings(false, false)
}

case class MusicIndex(root: String, artists: List[ArtistIndex]) {

  lazy val Allow_Fuzzy_Threshold = 30

  def refresh = MusicIndex(rootPath)

  def rootPath = Path(root.replace("\\", "/"), '/')

  def toJson = MusicIndex.toJson(this)

  def included = copy(artists = artists.flatMap{_.included})
  def excluded = copy(artists = artists.flatMap{_.excluded})

  def -(other: MusicIndex)(implicit diffSettings: DiffSettings) = {
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

  def getArtist(name: String)(implicit diffSettings: DiffSettings) = {
    if (diffSettings.fuzzyMatch && name.length > Allow_Fuzzy_Threshold)
      artists.find(a => a.name.startsWith(name) || name.startsWith(a.name))
    else
      artists.find(_.name == name)
  }

  def fileAdded(pathString: String) = {
    implicit val diffSettings = DiffSettings.FileCopyDiffSettings

    val path = Path(pathString.replace("\\", "/"), '/')
    val relativePath = path.relativize(rootPath)

    getArtist(relativePath.segments.head) match {
      case Some(artist) => {
        val updatedArtist = artist.fileAdded(path)
        copy(artists = (updatedArtist :: artists.filterNot(_ == artist)).sortBy(_.name))
      }
      case None => {
        val a = ArtistIndex(rootPath / relativePath.segments.head)
        copy(artists = (a :: artists).sortBy(_.name))
      }
    }
  }

  def exclude(path: String) = {
    copy(artists = artists.map(_.exclude(path)))
  }

  def include(path : String) = {
    copy(artists = artists.map(_.include(path)))
  }

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
      path.children(PathMatcher.IsDirectory).toList.filterNot(_.name.startsWith(".")).map{ArtistIndex(_)}.sortBy(_.name))
  }

  def toJson(index: MusicIndex) = write(index)

  def fromJson(j: String) = read[MusicIndex](j)
}

case class ArtistIndex(root: String, name: String, albums: List[AlbumIndex], isExcluded: Boolean = false) {
  lazy val Allow_Fuzzy_Threshold = 30

  def rootPath = Path(root.replace("\\", "/"), '/')

  def included = if(isExcluded) None else Some(copy(albums = albums.flatMap(_.included)))
  def excluded = if(isExcluded) Some(this) else {
    albums.flatMap(_.excluded) match {
      case Nil => None
      case as => Some(copy(albums = as))
    }
  }

  def -(other: ArtistIndex)(implicit diffSettings: DiffSettings) = {
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

  def getAlbum(name: String)(implicit diffSettings: DiffSettings) = {
    if (diffSettings.fuzzyMatch && name.length > Allow_Fuzzy_Threshold)
      albums.find(a => a.name.startsWith(name) || name.startsWith(a.name))
    else
      albums.find(_.name == name)
  }

  def exclude(path: String) = {
    if (path == root)
      copy(isExcluded = true)
    else
      copy(albums = albums.map(_.exclude(path)))
  }

  def include(path : String) = {
    if (path == root)
      copy(isExcluded = false)
    else
      copy(albums = albums.map(_.include(path)))
  }

  def fileAdded(path: Path) = {
    implicit val diffSettings = DiffSettings.FileCopyDiffSettings
    val relativePath = path.relativize(rootPath)

    getAlbum(relativePath.segments.head) match {
      case Some(album) => {
        val updatedArtist = album.fileAdded(path)
        copy(albums = (updatedArtist :: albums.filterNot(_ == album)).sortBy(_.name))
      }
      case None => {
        val a = AlbumIndex(rootPath / relativePath.segments.head)
        copy(albums = (a :: albums).sortBy(_.name))
      }
    }
  }

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
      path.children(PathMatcher.IsDirectory).toList.filterNot(_.name.startsWith(".")).map{ p => AlbumIndex(p) }.sortBy(_.name))
  }
}


case class AlbumIndex(root: String, name: String, songs: List[Song], isExcluded: Boolean = false) {

  lazy val Allow_Fuzzy_Threshold = 30

  def rootPath = Path(root.replace("\\", "/"), '/')

  def included = if(isExcluded) None else Some(copy(songs = songs.filterNot(_.isExcluded)))
  def excluded = if(isExcluded) Some(this) else {
    songs.filter(_.isExcluded) match {
      case Nil => None
      case es => Some(copy(songs = es))
    }
  }

  def -(other: AlbumIndex)(implicit diffSettings: DiffSettings) = {
    val validSongs = if (diffSettings.ignoreArtwork) songs.filterNot(_.name.startsWith("folder.")) else songs
    val diffSongs = validSongs.flatMap { song =>
      other.getSong(song.name) match {
        case Some(_) => None
        case None => Some(song)
      }
    }

    AlbumIndex(root, name, diffSongs)
  }

  def getSong(name: String)(implicit diffSettings: DiffSettings) = {
    def title(s: String) = s.substring(0, s.lastIndexOf('.'))

    if (diffSettings.fuzzyMatch && name.length > Allow_Fuzzy_Threshold) {
      val nameWithoutExt = title(name)
      songs.find(a => title(a.name).startsWith(nameWithoutExt) || nameWithoutExt.startsWith(title(a.name)))
    } else if (diffSettings.fuzzyMatch) {
      songs.find {a => title(a.name) == title(name)}
    } else
      songs.find(_.name == name)
  }

  def exclude(path: String) = {
    if (path == root)
      copy(isExcluded = true)
    else
      copy(songs = songs.map(_.exclude(path)))
  }

  def include(path : String) = {
    if (path == root)
      copy(isExcluded = false)
    else
      copy(songs = songs.map(_.include(path)))
  }

  def fileAdded(path: Path) = {
    implicit val diffSettings = DiffSettings.FileCopyDiffSettings

    val relativePath = path.relativize(rootPath)

    getSong(relativePath.segments.head) match {
      case Some(song) => {
        println("file already in index")
        this
      }
      case None => {
        val s = Song(path.path, path.name)
        copy(songs = (s :: songs).sortBy(_.name))
      }
    }
  }


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
    val fileNames = subFiles.toList.filterNot(_.name.startsWith(".")).map{ p => Song(p.path, p.name)}

    new AlbumIndex(
      path.path,
      path.name,
      fileNames.sortBy(_.name))
  }

  val coverArtFormats = List("jpg", "gif")
}

case class Song(root: String, name: String, isExcluded: Boolean = false) {
  def exclude(path: String) = {
    if (path == root)
      copy(isExcluded = true)
    else
      this
  }

  def include(path : String) = {
    if (path == root)
      copy(isExcluded = false)
    else
      this
  }
}
