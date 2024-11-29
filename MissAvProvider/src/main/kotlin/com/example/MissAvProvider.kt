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
            mainUrl+"dm509/en/release?page=" to "Main Page",
            mainUrl + "dm36/en/genres/Incest?page=" to "Incest",
            mainUrl + "dm312/en/genres/Slut?page=" to "Slut",
            mainUrl + "dm783/en/genres/Sister?page=" to "Sister",
            mainUrl + "dm724/en/genres/Ntr?page=" to "NTR",
            mainUrl + "/dm44/en/genres/Black%20Male%20Actor?page=" to "Black Actor",

    )
    val saveImage = "";

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
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
        )

        val pagedLink = if (page > 0) request.data+page else request.data.replace("?page=","")


        val document = app.get(request.data + page).document
        val home = document.select(".thumbnail.group").map {
            val title = it.selectFirst(".my-2 a")?.text()
            val poster = it.selectFirst("img")?.attr("data-src").toString().replace("cover-t", "cover-n")

            val link = it.selectFirst(".my-2 a")?.attr("href") ?: ""
            newMovieSearchResponse(title.toString(), link, TvType.NSFW) { this.posterUrl = poster }
        }
        return newHomePageResponse(
                list = HomePageList(
                        name               = request.name,
                        list               = home,
                        isHorizontalImages = true
                ),
                hasNext = true
        )

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
        //val poster = "https://javenspanish.com/wp-content/uploads/2022/01/JUFE-132.jpg"
        val title = doc.selectFirst(".mt-4 h1")?.text()?:""
        val type = "NFSW"
        var description =""
        val code = url.substringAfter("/en/").substringBefore("/")
        val poster = "https://fivetiu.com/" + code + "/cover-n.jpg"

        val des = doc.selectFirst(".mb-8")?.text().toString()
        val regdes = """Magnet\s*(.*?)\s*Show more""".toRegex(RegexOption.DOT_MATCHES_ALL)
        val matchdes = regdes.find(des)

        if (matchdes != null) {
            description = matchdes.groups[1]?.value?.trim().toString()
        }




        val x = doc.selectFirst("head").toString()
        val regex = """<meta property="og:video:duration" content="(\d+)" ?/?>""".toRegex()
        val matchResult = regex.find(x)
        val seg = matchResult?.groups?.get(1)?.value?.toIntOrNull()

        var min =0
        if(seg !=null){
            min = seg / 60
        }



        var starname = ArrayList<String>()
        var lista = ArrayList<Actor>()

        doc.select("div.space-y-2 div").mapNotNull {
            if(it.text().contains("Actress")){
                val names = it.text().replace("Actress:", "").trim().split(", ")

                for (name in names){
                    val r = name.split(" ")
                    starname.add(r.reversed().joinToString(" "))
                }
            }
        }
        if (starname.size > 0) {

            for (i in 0..starname.size - 1) {

                var r = starname[i].split(" ")
                app.get("https://www.javdatabase.com/idols/" + r.reversed().joinToString("-")).document.select("#main ").mapNotNull {
                    var save = it.select(".entry-content .idol-portrait img").attr("src")
                    //var otro = "https://st4.depositphotos.com/9998432/23767/v/450/depositphotos_237679112-stock-illustration-person-gray-photo-placeholder-woman.jpg"
                    var otro = "https://tse1.mm.bing.net/th?id=OIP.6_wb2dVFWij-BlgOVLAvnQAAAA&pid=15.1"
                    if (save.contains("http")) {
                        lista.add(Actor(starname[i], save))
                    } else {
                        app.get("https://www.javdatabase.com/idols/" + r.joinToString("-")).document.select("#main ").mapNotNull {
                            var save = it.select(".entry-content .idol-portrait img").attr("src")
                            if(save.contains("http")){
                                lista.add(Actor(starname[i], save))
                            }else{
                                lista.add(Actor(starname[i], otro))
                            }
                        }
                    }

                }
            }
        }



            //Fin espacio prueba
        return newMovieLoadResponse(
                title,
                url,
                TvType.NSFW,
                url
        ) {
            posterUrl = fixUrlNull(poster)
            this.plot = description
            this.recommendations = null
            this.duration = min
            addActors(lista)
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