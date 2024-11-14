package com.example


import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.M3u8Helper.Companion.generateM3u8
import com.lagradost.cloudstream3.utils.getQualityFromName
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import kotlinx.coroutines.selects.select
import java.util.*
import kotlin.collections.ArrayList


class JavHDProvider : MainAPI() {
    companion object {
        fun getType(t: String): TvType {
            return if (t.contains("OVA") || t.contains("Especial")) TvType.OVA
            else if (t.contains("Pelicula")) TvType.AnimeMovie
            else TvType.Anime
        }
    }

    override var mainUrl = "https://javhd.today/"
    override var name = "JavHD"
    override var lang = "es"
    override val hasMainPage = true
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(
            TvType.NSFW
    )
    //https://bestjavporn.me/page/1?filter=latest
    override val mainPage = mainPageOf(
            "$mainUrl/recent/" to "Main Page",
    )
    val saveImage = "";

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val urls = listOf(
                Pair(
                        "$mainUrl/mother/",
                        "Mom"
                ),
                Pair(
                        "$mainUrl/popular/",
                        "Best AV"
                ),

        )

        val pagedLink = if (page > 1) "$mainUrl/recent/" + page else "$mainUrl/recent/"
        val items = ArrayList<HomePageList>()
        var texto: String
        var inicio: Int
        var ultimo: Int
        var link: String
        items.add(
                HomePageList(
                        "Recientes",
                        app.get(pagedLink).document.select(".videos li").map {
                            val url = it.selectFirst("a")?.attr("href").toString()
                            val title = it.selectFirst(".video-thumb img")?.attr("alt")
                            val img= it.selectFirst(".video-thumb img")?.attr("src").toString()
                            val poster = if(img.contains("http")) img else "$mainUrl" + img

                            val dubstat = if (title!!.contains("Latino") || title.contains("Castellano"))
                                DubStatus.Dubbed else DubStatus.Subbed
                            //val poster = it.selectFirst("a div img")?.attr("src") ?: ""

                            newAnimeSearchResponse(title, "$mainUrl" + url) {
                                this.posterUrl = poster
                                //addDubStatus(dubstat)
                            }
                        },isHorizontalImages = false)
        )
        urls.apmap { (url, name) ->
            var pagedLink = ""
            if(url.contains("mother")){
                pagedLink = if (page > 1) "$mainUrl/mother/" + page else "$mainUrl/mother/"
            }else if(url.contains("popular")){
                pagedLink = if (page > 1) "$mainUrl/popular/" + page else "$mainUrl/popular/"
            }
            val soup = app.get(pagedLink).document

            val home = soup.select(".videos li").map {
                val url = it.selectFirst("a")?.attr("href")
                val title = it.selectFirst(".video-thumb img")?.attr("alt")
                val poster = it.selectFirst(".video-thumb img")?.attr("src").toString()
                AnimeSearchResponse(
                        title!!,
                        fixUrl("$mainUrl"+ url ),
                        this.name,
                        TvType.Anime,
                        fixUrl(poster),
                        null
                )
            }
            items.add(HomePageList(name, home,isHorizontalImages = false))
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

            return app.get("$mainUrl//?s=$query").document
                    .select("#main").select("article").mapNotNull {
                        val image = it.selectFirst(" div div img")?.attr("data-src")
                        val title = it.selectFirst("header span")?.text().toString()
                        val url = fixUrlNull(it.selectFirst("a")?.attr("href") ?: "") ?: return@mapNotNull null


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
    override suspend fun load(url: String): LoadResponse {
        val texto: String
        var inicio: Int
        var ultimo: Int
        var link: String

        val doc = app.get(url, timeout = 120).document
        val poster = doc.selectFirst(".col-xs-12.col-sm-12.col-md-12")?.attr("src")
        val title = doc.selectFirst("#video > div.left.content.content-video > div > h1")?.text() ?: ""
        val type = TvType.NSFW
        val des = doc.selectFirst(".description")?.text().toString()
        val description = des.substring(0,des.indexOf("工"))

        //test tmp

        var starname = ArrayList<String>()
        var lista = ArrayList<Actor>()

        doc.select("#video-actors a").mapNotNull {
            starname.add(it.attr("title"))
        }
        if (starname.size>0) {

            for(i in 0 .. starname.size-2){
                app.get("https://www.javdatabase.com/idols/" + starname[i].replace(" ","-")).document.select("#main ").mapNotNull {
                   var save = it.select(".entry-content .idol-portrait img").attr("src")
                    var otro = "https://st4.depositphotos.com/9998432/23767/v/450/depositphotos_237679112-stock-illustration-person-gray-photo-placeholder-woman.jpg"
                    if(save.contains("http")){
                        lista.add(Actor(starname[i],save))
                    }else{
                        lista.add(Actor(starname[i],otro))
                    }

                }
            }
        }

        /////Fin espacio prueba


        //parte para rellenar la lista recomendados
            val recomm = doc.select(".loop-video").mapNotNull {
            val href = it.selectFirst("a")!!.attr("href")
            val posterUrl = it.selectFirst("img")?.attr("data-src") ?: ""
            val name = it.selectFirst("header span")?.text() ?: ""
            MovieSearchResponse(
                    name,
                    href,
                    this.name,
                    type,
                    posterUrl
            )

        }
        //finaliza la parte de relleno de recomendados
        return newMovieLoadResponse(
                title,
                url,
                type,
                url
        ) {
            posterUrl = fixUrlNull(poster)
            this.plot = description
            this.recommendations = recomm
            this.duration = null
            addActors(lista)
        }
       /* return MovieLoadResponse(
                name = title,
                url = url,
                apiName = this.name,
                type = TvType.NSFW,
                dataUrl = url,
                posterUrl = poster,
                plot = description,
                recommendations = recomm

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
   override suspend fun loadLinks(
           data: String,
           isCasting: Boolean,
           subtitleCallback: (SubtitleFile) -> Unit,
           callback: (ExtractorLink) -> Unit
   ): Boolean {
       //val f = listOf("https://streamtape.net/e/4zv4vA4y9rI284/","https://streamtape.com/e/4zv4vA4y9rI284/","https://ds2play.com/e/gli2qcwpmtvl")

       app.get(data).document.select(".button_style .button_choice_server").mapNotNull{
           val videos =it.attr("onclick")
           fetchUrls(videos).map {
               it.replace("playEmbed('","")
                       .replace("')","")
                       .replace("https://dooood.com","https://dood.ws")
                       .replace("https://dood.sh", "https://dood.ws")
                       .replace("https://dood.la","https://dood.ws")
                       .replace("https://ds2play.com","https://dood.ws")
                       .replace("https://dood.to","https://dood.ws")

           }.apmap {
               loadExtractor(it, data, subtitleCallback, callback)
           }
       }

       return true
   }
}