package com.example


import android.annotation.TargetApi
import android.os.Build
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.M3u8Helper
import com.lagradost.cloudstream3.utils.M3u8Helper.Companion.generateM3u8
import com.lagradost.cloudstream3.utils.getQualityFromName
import com.lagradost.cloudstream3.utils.loadExtractor
import org.json.JSONObject
import java.util.Base64
import java.util.*
import kotlin.collections.ArrayList


class MissAvProvider : MainAPI() {
    companion object {
        fun getType(t: String): TvType {
            return if (t.contains("OVA") || t.contains("Especial")) TvType.OVA
            else if (t.contains("Pelicula")) TvType.AnimeMovie
            else TvType.Anime
        }
    }

    override var mainUrl = "https://missav.com/"
    override var name = "MissAv"
    override var lang = "es"
    override val hasMainPage = true
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(
            TvType.NSFW
    )
    override val mainPage = mainPageOf(
            "$mainUrl/page/" to "Main Page",
    )
    val saveImage = "";

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        var numero = page
        val urls = listOf(
                Pair(
                        mainUrl + "dm36/en/genres/Incest",
                        "Incest"
                ),
                Pair(
                        mainUrl + "dm312/en/genres/Slut",
                        "Slut"
                ),
                Pair(
                        mainUrl + "dm783/en/genres/Sister",
                        "Sister"
                ),
                Pair(
                        mainUrl + "dm724/en/genres/Ntr",
                        "NTR"
                ),
                Pair(
                        mainUrl + "dm312/en/genres/Slut",
                        "Slut"
                ),
                Pair(
                        mainUrl + "dm312/en/genres/Slut",
                        "Slut"
                ),

        )

        val pagedLink = if (page > 0) mainUrl+ "dm509/en/release?page=" + page else mainUrl + "dm509/en/release"
        val items = ArrayList<HomePageList>()
        items.add(
                HomePageList(
                        "Recientes",
                        app.get(pagedLink).document.select(".thumbnail.group").map {
                            val title = it.selectFirst(".my-2 a")?.text().toString()
                            val poster = it.selectFirst("img")?.attr("data-src")
                            val url = it.selectFirst(".my-2 a")?.attr("href") ?: ""


                            newAnimeSearchResponse(title, url) {
                                this.posterUrl = poster
                            }
                        }, isHorizontalImages = true)
        )
        urls.apmap { (url, name) ->
            val pagedLink = if (page > 0) url + "?page=" + numero else url
            val soup = app.get(pagedLink).document
            val home = soup.select(".thumbnail.group").map {
                val title = it.selectFirst(".my-2 a")?.text()
                val poster = it.selectFirst("img")?.attr("data-src").toString().replace("cover-t","cover-n")

                val link = it.selectFirst(".my-2 a")?.attr("href") ?: ""

                AnimeSearchResponse(
                        title!!,
                        fixUrl(link),
                        this.name,
                        TvType.Anime,
                        fixUrl(poster),
                        null,
                        if (title.contains("Latino") || title.contains("Castellano")) EnumSet.of(
                                DubStatus.Dubbed
                        ) else EnumSet.of(DubStatus.Subbed),
                )
            }
            items.add(HomePageList(name, home, isHorizontalImages = true))
        }

        if (items.size <= 0) throw ErrorLoadingException()
        return HomePageResponse(items, hasNext = true)

    }

    data class MainSearch(
            @JsonProperty("animes") val animes: List<Animes>,
            @JsonProperty("anime_types") val animeTypes: AnimeTypes
    )

    data class Animes(
            @JsonProperty("id") val id: String,
            @JsonProperty("slug") val slug: String,
            @JsonProperty("title") val title: String,
            @JsonProperty("image") val image: String,
            @JsonProperty("synopsis") val synopsis: String,
            @JsonProperty("type") val type: String,
            @JsonProperty("status") val status: String,
            @JsonProperty("thumbnail") val thumbnail: String
    )

    data class AnimeTypes(
            @JsonProperty("TV") val TV: String,
            @JsonProperty("OVA") val OVA: String,
            @JsonProperty("Movie") val Movie: String,
            @JsonProperty("Special") val Special: String,
            @JsonProperty("ONA") val ONA: String,
            @JsonProperty("Music") val Music: String
    )

    override suspend fun search(query: String): List<SearchResponse> {

        val soup = app.get("$mainUrl//?s=$query").document
        var texto: String
        var inicio: Int
        var ultimo: Int
        var link: String
        var z: Int
        var poster = ""

            return app.get("https://missav.com/en/search/$query").document
                    .select(".thumbnail.group").mapNotNull {
                        val image = it.selectFirst("img")?.attr("data-src").toString().replace("cover-t","cover-n")
                        val title = it.selectFirst(".my-2 a")?.text().toString()
                        val url = fixUrlNull(it.selectFirst(".my-2 a")?.attr("href") ?: "") ?: return@mapNotNull null

                        MovieSearchResponse(
                                title,
                                url,
                                this.name,
                                TvType.NSFW,
                                image
                        )
        }

    }
    data class EpsInfo (
            @JsonProperty("number" ) var number : String? = null,
            @JsonProperty("title"  ) var title  : String? = null,
            @JsonProperty("image"  ) var image  : String? = null
    )
    @TargetApi(Build.VERSION_CODES.O)
    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url, timeout = 120).document
        var test ="";
        //val poster = "https://javenspanish.com/wp-content/uploads/2022/01/JUFE-132.jpg"
        val title = doc.selectFirst(".mt-4 h1")?.text()?:""
        val type = "NFSW"
        val description = ""
        //val poster = doc.selectFirst(".inside-article img")?.attr("src")
        val code = url.substringAfter("/en/").substringBefore("/")
        val poster = "https://fivetiu.com/" + code + "/cover-n.jpg"



            //Fin espacio prueba
        return newMovieLoadResponse(
                title,
                url,
                TvType.NSFW,
                url
        ) {
            posterUrl = fixUrlNull(poster)
            this.plot = test
            this.recommendations = null
            this.duration = null
        }

    /* return MovieLoadResponse(
                name = title,
                url = url,
                apiName = this.name,
                type = TvType.NSFW,
                dataUrl = url,
                posterUrl = poster,
                plot = null,
                actors = lista
        )*/

    }

   /* override suspend fun loadLinks(
            data: String,
            isCasting: Boolean,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ): Boolean {
        try{
            val url = "https://ds2play.com/e/8vxqazdt0aej"
            loadExtractor(
                    url = url,
                    subtitleCallback = subtitleCallback,
                    callback = callback
            )
        } catch (e: Exception) {
            e.printStackTrace()
            logError(e)
        }
        return false
    }*/
   private fun cleanExtractor(
           source: String,
           name: String,
           url: String,
           callback: (ExtractorLink) -> Unit
   ): Boolean {
       callback(
               ExtractorLink(
                       source,
                       name,
                       url,
                       "",
                       Qualities.Unknown.value,
                       false
               )
       )
       return true
   }
   @TargetApi(Build.VERSION_CODES.O)
   override suspend fun loadLinks(
           data: String,
           isCasting: Boolean,
           subtitleCallback: (SubtitleFile) -> Unit,
           callback: (ExtractorLink) -> Unit
   ): Boolean {
       var value =""
       var check =""
       var text = app.get(data).document.selectFirst("body").toString()
       val pattern = "https:\\\\/\\\\/sixyik\\.com\\\\/([^\"]+)\\\\/seek".toRegex()
       val matchResult = pattern.find(text)
       if (matchResult != null) {
           value = matchResult.groupValues[1]
       }

       val m = text.substring(text.indexOf("eval(function(p,a,c,k,e,d)"))
       val regex = """(\d{3,4}x\d{3,4}|\d{3,4}p)""".toRegex()
       val resolutions = regex.find(m)
       if(resolutions != null){
           check = resolutions?.value.toString()
       }
       var lista =listOf<String>()
       if(check.contains("p")){
           lista = listOf("1080p", "720p", "480p", "360p")
       }else{
           lista = listOf("1920x1080", "1280x720", "842x480", "640x360")
       }
       val index = lista.indexOfFirst {
           it.contains(check, ignoreCase = true)
       }
       val resultado = if (index != -1) { lista.subList(index, lista.size) } else { emptyList() }

       resultado.forEach { item ->

           M3u8Helper().m3u8Generation(
                   M3u8Helper.M3u8Stream(
                           "https://surrit.com/$value/$item/video.m3u8",
                           headers = app.get(data).headers.toMap()
                   ), true
           ).map { stream ->
               val res = if(item.contains("x")) item.substring(item.indexOf("x")+1) + "p" else item
               callback(
                       ExtractorLink(
                               source = this.name,
                               name = "${this.name} " + res,
                               url = stream.streamUrl,
                               referer = data,
                               quality = getQualityFromName(stream.quality?.toString()),
                               isM3u8 = true
                       )
               )
           }
       }


/*
       var links =listOf<String>()
       var res = listOf("1080p","720p","480p","360p")
       if(check.contains("p")||check.contains("P")){
           links = listOf("https://surrit.com/" + value +"/1080p/video.m3u8","https://surrit.com/" + value +"/720p/video.m3u8","https://surrit.com/" + value +"/480p/video.m3u8","https://surrit.com/" + value +"/360p/video.m3u8")
       }
       if (check.contains("x")||check.contains("X")){
           links = listOf("https://surrit.com/" + value +"/1920x1080/video.m3u8","https://surrit.com/" + value +"/1280x720/video.m3u8","https://surrit.com/" + value +"/842x480/video.m3u8","https://surrit.com/" + value +"/640x360/video.m3u8")
       }

       val linkres = links.zip(res).toMap()

       linkres.forEach { (links, resolution) ->

           M3u8Helper().m3u8Generation(
                   M3u8Helper.M3u8Stream(
                           links,
                           headers = app.get(data).headers.toMap()
                   ), true
           ).map { stream ->
               callback(
                       ExtractorLink(
                               source = this.name,
                               name = "${this.name} " +resolution ,
                               url = stream.streamUrl,
                               referer = data,
                               quality = getQualityFromName(stream.quality?.toString()),
                               isM3u8 = true
                       )
               )
           }
       }
*/

       return true
   }
}