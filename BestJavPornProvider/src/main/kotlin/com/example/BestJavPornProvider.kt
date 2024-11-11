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
import java.util.*


class BestJavPornProvider : MainAPI() {
    companion object {
        fun getType(t: String): TvType {
            return if (t.contains("OVA") || t.contains("Especial")) TvType.OVA
            else if (t.contains("Pelicula")) TvType.AnimeMovie
            else TvType.Anime
        }
    }

    override var mainUrl = "https://bestjavporn.me/"
    override var name = "BestJavPorn"
    override var lang = "es"
    override val hasMainPage = true
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(
            TvType.NSFW
    )
    //https://bestjavporn.me/page/1?filter=latest
    override val mainPage = mainPageOf(
            "$mainUrl/page/1?filter=latest" to "Main Page",
    )
    val saveImage = "";

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val urls = listOf(
                Pair(
                        "$mainUrl/category/uncensored/",
                        "Uncensored"
                ),
                Pair(
                        "$mainUrl/category/censored/",
                        "Censored"
                ),
        )

        val pagedLink = if (page > 0) "https://bestjavporn.me/page/" + page + "?filter=latest" else "https://bestjavporn.me/?filter=latest"
        val items = ArrayList<HomePageList>()
        var texto: String
        var inicio: Int
        var ultimo: Int
        var link: String
        items.add(
                HomePageList(
                        "Recientes",
                        app.get(pagedLink).document.select(".videos-list article").map {
                            val title = it.selectFirst("header span")?.text()
                            texto = it.selectFirst("a div div").toString()
                            inicio = texto.indexOf("data-src=") + 10
                            ultimo = texto.length
                            link = texto.substring(inicio, ultimo).toString()
                            val poster = link.substring(0, link.indexOf(" ")).replace("\"","")
                            val dubstat = if (title!!.contains("Latino") || title.contains("Castellano"))
                                DubStatus.Dubbed else DubStatus.Subbed
                            //val poster = it.selectFirst("a div img")?.attr("src") ?: ""

                            val url = it.selectFirst("a")?.attr("href") ?: ""


                            newAnimeSearchResponse(title, url) {
                                this.posterUrl = poster
                                //addDubStatus(dubstat)
                            }
                        },isHorizontalImages = true)
        )
        urls.apmap { (url, name) ->
            var pagedLink = ""
            if(url.contains("uncensored")){
                pagedLink = if (page > 0) "https://bestjavporn.me/category/uncensored/page/" + page else "https://bestjavporn.me/category/uncensored/"
            }else if(url.contains("censored")){
                pagedLink = if (page > 0) "https://bestjavporn.me/category/censored/page/" + page else "https://bestjavporn.me/category/censored/"
            }
            val soup = app.get(pagedLink).document
            var texto: String
            var inicio: Int
            var ultimo: Int
            var link: String
            var z: Int
            var poster = ""
            val home = soup.select(".videos-list article").map {
                val title = it.selectFirst("header span")?.text()
                texto = it.selectFirst("a div div").toString()
                inicio = texto.indexOf("data-src=") + 10
                ultimo = texto.length
                link = texto.substring(inicio, ultimo).toString()
                poster = link.substring(0, link.indexOf(" ")).replace("\"","")
                AnimeSearchResponse(
                        title!!,
                        fixUrl(it.selectFirst("a")?.attr("href") ?: ""),
                        this.name,
                        TvType.Anime,
                        fixUrl(poster),
                        null
                )
            }
            items.add(HomePageList(name, home,isHorizontalImages = true))
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
                    .select(".elementor-posts-container").select(".elementor-post__card").mapNotNull {
                        texto = it.selectFirst(".elementor-post__thumbnail img").toString()
                        inicio = texto.indexOf("srcset=") + 7
                        ultimo = texto.length
                        link = texto.substring(inicio, ultimo).toString()
                        z = link.indexOf(" ")
                        val image = link.substring(0, z).replace("\"","")
                        val title = it.selectFirst(".elementor-post__title > a")?.text().toString()
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
        //val poster = "https://javenspanish.com/wp-content/uploads/2022/01/JUFE-132.jpg"
        val title = doc.selectFirst("article h1")?.text()?:""
        val type = "NFSW"
        //val description = doc.selectFirst("article p")?.text()

        //test tmp
        var description=""

        /*var ss= doc.selectFirst("div.box-server > a ")?.attr("onclick").toString().replace("go('https://v.javmix.me/vod/player.php?","").replace("')","")

        if(ss.contains("ST=")){
            description = ss.replace("ST=","https://streamtape.com/e/")
        }else if(ss.contains("emt=")){
            description= ss.replace("emt=","https://dood.ws/e/")
        }else{
            description= ss.replace("","")
        }*/
        app.get(url).document.select("div.box-server > a ").mapNotNull {
            val videos = it.attr("onclick")
            fetchUrls(videos).map {
               description += it.replace("go('https://v.javmix.me/vod/player.php?", "")
                        .replace("')", "")
                        .replace("stp=", "https://streamtape.com/e/")
                        .replace("do=", "https://dood.ws/e/")

            }
        }

        ///////

        texto = doc.selectFirst(".video-player .responsive-player")?.attr("style").toString()
        inicio = texto.indexOf("http")
        ultimo = texto.length
        link = texto.substring(inicio, ultimo).toString()
        val poster = link.substring(0, link.indexOf("\"")).replace("\"","")
        //val poster =""
        //Fin espacio prueba
        return MovieLoadResponse(
                name = title,
                url = url,
                apiName = this.name,
                type = TvType.NSFW,
                dataUrl = url,
                posterUrl = poster,
                plot = description

        )

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

       app.get(data).document.select("div.box-server > a ").mapNotNull{
           val videos =it.attr("onclick")
           fetchUrls(videos).map {
               it.replace("go('https://v.javmix.me/vod/player.php?","")
                       .replace("')","")
                       .replace("stp=","https://streamtape.com/e/")
                       .replace("do=","https://dood.ws/e/")

           }.apmap {
               loadExtractor(it, data, subtitleCallback, callback)
           }
       }

       return true
   }
}