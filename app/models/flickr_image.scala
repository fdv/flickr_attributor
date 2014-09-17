package models

import play.api._
import play.api.libs.json._

import scalaj.http.Http
import scalaj.http.HttpOptions


class FlickrImage(id: String) {
  val flickREndpoint = "https://api.flickr.com/services/rest/"

  val key = Play.current.configuration.getString("flickr.key").get

  val rawTitle = (__ \ 'photo \ 'title \ '_content).json.pick[JsString]
  val rawLicense = (__ \ 'photo \ 'license ).json.pick[JsString]
  val rawUser = ( __ \ 'photo \ 'owner \ 'username).json.pick[JsString]
  val stat = ( __ \ 'stat ).json.pick[JsString]

  val baseRequest = Http(flickREndpoint)
      .param("api_key", key)
      .param("nojsoncallback", "1")
      .param("format", "json")
      .option(HttpOptions.connTimeout(2000))
      .option(HttpOptions.readTimeout(5000))

  lazy val (info: JsValue, status) = {
    val src = baseRequest
      .param("method","flickr.photos.getInfo")
      .param("photo_id", id).asString

    println(src)

    val ret = Json.parse(src)

    ret.transform(stat) match {
      case JsError(_) => (ret, None)
      case JsSuccess(v, p) => {
        if (v.value == "fail") {
          (ret, src)
        }
      }
    }
  }

  lazy val sizes = {
    val src = baseRequest
      .param("method","flickr.photos.getSizes")
      .param("photo_id", id).asString

    Json.parse(src)
  }

  lazy val title = info.transform(rawTitle).getOrElse(new JsString("")).value
  lazy val license = info.transform(rawLicense).get.value
  lazy val user = info.transform(rawUser).get.value

  lazy val images = {
    val picker = (__ \ 'sizes \ 'size ).json.pick[JsArray]
    sizes.transform(picker).get.value.map( sz =>
      (sz \ "label").asOpt[JsString].get.value -> (sz \ "source").asOpt[JsString].get.value
    ).toMap
  }


}