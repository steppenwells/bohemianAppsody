package com.swells.sonas.service

import java.net.{URLEncoder, URL}
import io.Source
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization
import com.swells.sonas.util.Logging

object LastFmApi extends Logging {

  implicit val formats = Serialization.formats(NoTypeHints)

  val albumInfoEndpoint = "http://ws.audioscrobbler.com/2.0/?method=album.getinfo&api_key=9a188b68526c0492a9f5568b18347acb&artist=%s&album=%s&format=json"

  def getAlbumArtwork(artist: String, album: String) = {

    val query = albumInfoEndpoint.format(encode(artist), encode(album))
    log.debug(query)
    val connection = Source.fromURL(query, "UTF-8")
    val resp = connection.getLines().mkString("\n")

    val json = parse(resp).transformField{
      case ("#text", x) => ("imageUrl", x)
    }

    val lfmData = (json \ "album").extract[LfmAlbum]

    lfmData.image.find(_.size == "large").map(_.imageUrl)
  }

  def encode(s: String) = URLEncoder.encode(s, "UTF-8")
}

case class LfmAlbum(name: String, artist: String, image: List[LfmImage])
case class LfmImage(imageUrl: String, size: String)