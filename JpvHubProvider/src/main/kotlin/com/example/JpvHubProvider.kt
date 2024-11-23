package com.example


import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.M3u8Helper.Companion.generateM3u8
import com.lagradost.cloudstream3.utils.getQualityFromName
import com.lagradost.cloudstream3.utils.loadExtractor
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList


class JpvHubProvider : MainAPI() {
    companion object {
        fun getType(t: String): TvType {
            return if (t.contains("OVA") || t.contains("Especial")) TvType.OVA
            else if (t.contains("Pelicula")) TvType.AnimeMovie
            else TvType.Anime
        }
    }

    override var mainUrl = "https://www.jpvhub.com/"
    override var name = "JpvHub"
    override var lang = "es"
    override val hasMainPage = true
    override val hasChromecastSupport = true
    private val globalTvType = TvType.NSFW
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(
            TvType.NSFW
    )
    override val mainPage = mainPageOf(
            "$mainUrl/videos/censored/" to "Main Page",
    )
    val type = TvType.NSFW

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val urls = listOf(
                Pair(
                        "$mainUrl/videos/uncensored-leaked/",
                        "Sin Censura"
                ),
                Pair(
                        "$mainUrl/videos/mosaic-removed/",
                        "Censura eliminada"
                ),

                )
        val pagedLink = if (page > 0) "$mainUrl/videos/censored/" + page else "$mainUrl/videos/censored/"
        val items = ArrayList<HomePageList>()
        val lista = ArrayList<SearchResponse>()
        val listaurl = ArrayList<SearchResponse>()

        var z : String
        var gm : String
        var gm1 : String = ""
        val f = app.get(pagedLink).document.body()
        z = f.toString().substring(f.toString().indexOf("<script id=\"__NEXT_DATA__\" type=\"application/json\">")+51)
        gm = z.substring(0,z.indexOf("</script>"))
        val jsonObject = JSONObject(gm)
        val videoList = jsonObject
                .getJSONObject("props")
                .getJSONObject("pageProps")
                .getJSONArray("videoList")

            for (i in 0 until videoList.length()) {
                val video = videoList.getJSONObject(i)
                val url = "$mainUrl/video/" + video.getString("Id")
                val title = video.getJSONObject("title").getString("name")
                val views = video.getInt("views")
                val thumb = video.getString("thumbnailPath")

                lista.add(
                        MovieSearchResponse(
                                name = title,
                                url = url,
                                apiName = this.name,
                                type = globalTvType,
                                posterUrl = thumb,
                                year = null
                        ))
            }
            items.add(
                    HomePageList(
                            name = "Recientes",
                            list = lista,
                            isHorizontalImages = true
                    )
            )
        urls.apmap { (url, name) ->

            val pagedLink = if (page > 0) "$url/" + page else url
            var json : String
            var gmd : String
            val ff = app.get(pagedLink).document.body()
            json = ff.toString().substring(ff.toString().indexOf("<script id=\"__NEXT_DATA__\" type=\"application/json\">")+51)
            gmd = json.substring(0,json.indexOf("</script>"))

            val home = tryParseJson<VideoHomePage>(gmd).let { json ->

                json!!.props.pageProps.videoList.map {

                    val title = it.title.name
                    val thumb = it.thumb
                    val link = "$mainUrl/video/" + it.id

                    AnimeSearchResponse(
                            title!!,
                            fixUrl(link),
                            this.name,
                            TvType.NSFW,
                            fixUrl(thumb),
                            null
                    )

                }

            }

            items.add(HomePageList(name, home, isHorizontalImages = true))
            listaurl.clear()
        }


        /*val requestGet = app.get("https://www.jpvhub.com/videos/censored")
        val data = requestGet.text
        val jsonText = Regex("""window\.__NUXT__=(.*?);</script>""").find(data)?.destructured?.component1()
        items.add(HomePageList(
                "Recientes",
                tryParseJson<VideoHomePage>(jsonText).let { json ->
                    (json!!.props.pageProps.videoList.mapNotNull {
                        val url = mainUrl+ it.id
                        val poster = it.thumb
                        val title = it.title.name
                        newAnimeSearchResponse(title, url) {
                            this.posterUrl = poster
                        }
                    })
                },isHorizontalImages = true
        ))*/



        if (items.size <= 0) throw ErrorLoadingException()
        return HomePageResponse(items,hasNext = true)
    }
    private data class VideoHomePage (
            @JsonProperty("props") val props : HpProps
    )
    private data class HpProps (
            @JsonProperty("pageProps") val pageProps : HpPageProps
    )
    private data class HpPageProps (
            @JsonProperty("videoList") val videoList : List<HpjavVideos>
    )
    private data class HpjavVideos (
            @JsonProperty("Id") val id : String,
            @JsonProperty("title") val title : HpName,
            @JsonProperty("thumbnailPath") val thumb : String
    )
    private data class HpName (
            @JsonProperty("name") val name : String,
    )
    private fun getVideoByIdFromList(id: String, list: List<HpjavVideos>): HpjavVideos? {
        for (item in list) {
            if (item.id == id) {
                return item
            }
        }
        return null
    }
    override suspend fun search(query: String): List<SearchResponse> {

        val soup = app.get("$mainUrl//?s=$query").document

        return soup.select("#main").select("article").mapNotNull {
            val image = it.selectFirst(" div div img")?.attr("data-src")
            val title = it.selectFirst("header span")?.text().toString()
            val url = fixUrlNull(it.selectFirst("a")?.attr("href") ?: "") ?: return@mapNotNull null


            MovieSearchResponse(
                    title,
                    url,
                    this.name,
                    type,
                    image,
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
        var link: String
        var poster =""
        try {
            val f = app.get(url).document.body()
            val z = f.toString().substring(f.toString().indexOf("<script id=\"__NEXT_DATA__\" type=\"application/json\">")+51)
            val gm = z.substring(0,z.indexOf("</script>"))
            val jsonObject = JSONObject(gm)
            val details = jsonObject.getJSONObject("props").getJSONObject("pageProps").getJSONObject("details")
            val titleName = details.getJSONObject("title").getString("name")
            val thumb = details.getString("thumbnailPath")
            var starname = ArrayList<String>()
            var lista = ArrayList<Actor>()

            val modelsArray = details.getJSONArray("models")
            for (i in 0 until modelsArray.length()) {
                val model = modelsArray.getJSONObject(i)
                val modelName = model.getJSONObject("name").getString("name")
                starname.add(modelName)
            }


            if (starname.size>0) {

                for(i in 0 .. starname.size-1){

                    var r = starname[i].split(" ")
                    app.get("https://www.javdatabase.com/idols/" + r.reversed().joinToString("-")).document.select("#main ").mapNotNull {
                        var save = it.select(".entry-content .idol-portrait img").attr("src")
                        //var otro = "https://st4.depositphotos.com/9998432/23767/v/450/depositphotos_237679112-stock-illustration-person-gray-photo-placeholder-woman.jpg"
                        var otro = "https://tse1.mm.bing.net/th?id=OIP.6_wb2dVFWij-BlgOVLAvnQAAAA&pid=15.1"
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
            /*val recomm = doc.select(".under-video-block .loop-video").mapNotNull {
                val href = it.selectFirst("a")!!.attr("href")
                val posterUrl = it.selectFirst("img")?.attr("data-src") ?: ""
                val name = it.selectFirst("header span")?.text() ?: ""
                MovieSearchResponse(
                        name,
                        href,
                        this.name,
                        TvType.NSFW,
                        posterUrl
                )

            }*/
            //finaliza la parte de relleno de recomendados
            return newMovieLoadResponse(
                    titleName,
                    url,
                    TvType.NSFW,
                    url
            ) {
                posterUrl = fixUrlNull(thumb)
                this.plot = null
                this.recommendations = null
                this.duration = null
                addActors(lista)
            }
        }
        catch (e:Exception) {
            logError((e))
        }
        throw ErrorLoadingException()
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

    override suspend fun loadLinks(
            data: String,
            isCasting: Boolean,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ): Boolean {

        app.get(data).document.select(".entry-header .responsive-player iframe").mapNotNull{
            val videos = it.attr("src")
            fetchUrls(videos).map {
                it.replace("https://dooood.com", "https://dood.ws")
                        .replace("https://dood.sh", "https://dood.ws")
                        .replace("https://dood.la","https://dood.ws")
                        .replace("https://ds2play.com","https://dood.ws")
                        .replace("https://dood.to","https://dood.ws")
                        .replace("https://d0000d.com","https://dood.ws")

            }.apmap {
                loadExtractor(it, data, subtitleCallback, callback)
            }
        }
        return true
    }
}