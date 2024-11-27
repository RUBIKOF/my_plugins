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
        val idPattern = "\"Id\":\"([^\"]+)\"".toRegex()
        val titlePattern = "\"title\"\\s*:\\s*\\{[^}]*\"name\"\\s*:\\s*\"([^\"]+)\"".toRegex()
        val thumbnailPattern = "\"thumbnailPath\"\\s*:\\s*\"([^\"]+)\"".toRegex()

        val idMatches = idPattern.findAll(gm)
        val titleMatches = titlePattern.findAll(gm)
        val thumbnailMatches = thumbnailPattern.findAll(gm)




        idMatches.forEachIndexed { index, matchResult ->
            val id = matchResult.groupValues[1]
            val title = titleMatches.elementAtOrNull(index)?.groupValues?.get(1)
            val thumb = thumbnailMatches.elementAtOrNull(index)?.groupValues?.get(1).toString()

            lista.add(
                    MovieSearchResponse(
                            name = title.toString(),
                            url = mainUrl + "video/" + id,
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

            val idPattern1 = "\"Id\":\"([^\"]+)\"".toRegex()
            val titlePattern1 = "\"title\"\\s*:\\s*\\{[^}]*\"name\"\\s*:\\s*\"([^\"]+)\"".toRegex()
            val thumbnailPattern1 = "\"thumbnailPath\"\\s*:\\s*\"([^\"]+)\"".toRegex()

            val idMatches = idPattern1.findAll(gmd)
            val titleMatches = titlePattern1.findAll(gmd)
            val thumbnailMatches = thumbnailPattern1.findAll(gmd)
            val videoList = mutableListOf<Video>()
            val x = idMatches.forEachIndexed { index, matchResult ->
                val id = matchResult.groupValues[1]
                val title = titleMatches.elementAtOrNull(index)?.groupValues?.get(1)
                val thumb = thumbnailMatches.elementAtOrNull(index)?.groupValues?.get(1).toString()
                videoList.add(Video(id,title,thumb))
            }

            val home = videoList.map { video ->
                AnimeSearchResponse(
                        video.titleName!!,
                        fixUrl(mainUrl + "video/" + video.id),
                        this.name,
                        TvType.NSFW,
                        fixUrl(video.thumbnailPath.toString()),
                        null
                )
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

    private data class VideoPageRec (
            @JsonProperty("list") val list: List<VideosRec>
    )
    private data class VideosRec (
            @JsonProperty("Id") val id : String,
            @JsonProperty("title") val title : NameRec,
            @JsonProperty("thumbnailPath") val thumb : String
    )
    private data class NameRec (
            @JsonProperty("name") val name : String,
    )
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

        var json : String
        var gmd : String
        val ff = app.get(mainUrl + "search/" + query).document.body()
        json = ff.toString().substring(ff.toString().indexOf("<script id=\"__NEXT_DATA__\" type=\"application/json\">")+51)
        gmd = json.substring(0,json.indexOf("</script>"))

        val idPattern = "\"Id\":\"([^\"]+)\"".toRegex()
        val titlePattern = "\"title\"\\s*:\\s*\\{[^}]*\"name\"\\s*:\\s*\"([^\"]+)\"".toRegex()
        val thumbnailPattern = "\"thumbnailPath\"\\s*:\\s*\"([^\"]+)\"".toRegex()

        val idMatches = idPattern.findAll(gmd)
        val titleMatches = titlePattern.findAll(gmd)
        val thumbnailMatches = thumbnailPattern.findAll(gmd)


        val videoList = mutableListOf<Video>()
         idMatches.forEachIndexed { index, matchResult ->
            val id = matchResult.groupValues[1]
            val title = titleMatches.elementAtOrNull(index)?.groupValues?.get(1)
            val thumb = thumbnailMatches.elementAtOrNull(index)?.groupValues?.get(1).toString()
            videoList.add(Video(id,title,thumb))
        }

        return videoList.map { video ->
            AnimeSearchResponse(
                    video.titleName!!,
                    fixUrl(mainUrl +  "video/" + video.id),
                    this.name,
                    TvType.NSFW,
                    fixUrl(video.thumbnailPath.toString()),
                    null
            )
        }

    }
    data class Video(val id: String, val titleName: String?, val thumbnailPath: String?)
    data class EpsInfo (
            @JsonProperty("number" ) var number : String? = null,
            @JsonProperty("title"  ) var title  : String? = null,
            @JsonProperty("image"  ) var image  : String? = null
    )
    override suspend fun load(url: String): LoadResponse {
        val texto: String
        var inicio: Int
        var link: String
        var poster = ""
        try {
            var starname = ArrayList<String>()
            var lista = ArrayList<Actor>()
            val f = app.get(url).document.body()
            val z = f.toString().substring(f.toString().indexOf("<script id=\"__NEXT_DATA__\" type=\"application/json\">") + 51)
            val gm = z.substring(0, z.indexOf("</script>"))
            val titlePattern = "\"title\":\\s*\\{[^}]*\"name\":\\s*\"([^\"]+)\"".toRegex()
            val titleName = titlePattern.find(gm)?.groupValues?.get(1).toString()

            val thumbnailPattern = "\"thumbnailPath\"\\s*:\\s*\"([^\"]+)\"".toRegex()
            val thumbnailMatch = thumbnailPattern.find(gm)
            val thumb = thumbnailMatch?.groupValues?.get(1)


            /*val modelPattern = "\"models\"\\s*:\\s*\\[\\s*\\{[^}]*\"name\"\\s*:\\s*\\{[^}]*\"name\"\\s*:\\s*\"([^\"]+)\"".toRegex()
            val modelMatch = modelPattern.findAll(gm)
            modelMatch.forEach { matchResult ->
                val modelName = matchResult.groupValues[1]
                starname.add(modelName)
            }*/
            val modelPattern = "\"models\"\\s*:\\s*\\[([^]]*)]".toRegex()
            val namePattern = "\"name\"\\s*:\\s*\\{[^}]*\"name\"\\s*:\\s*\"([^\"]+)\"".toRegex()

            val modelsBlock = modelPattern.find(gm)?.groupValues?.get(1)
            val matches = modelsBlock?.let {
                namePattern.findAll(it)
            }?.map {
                it.groupValues[1]
            }?.toList() ?: emptyList()
            matches.forEach {
                starname.add(it)
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

            /////Fin espacio prueba

            //parte para rellenar la lista recomendados
            val id = url.replace("https://www.jpvhub.com/video/", "")
            val res = app.get("https://api.jpvhub.com/api/video/video/related/$id").text

            val recomm = tryParseJson<VideoPageRec>(res).let { json ->

                json!!.list.map {

                    val titlerec = it.title.name
                    val thumbrec = it.thumb
                    val linkrec = mainUrl + "video/" + it.id

                    AnimeSearchResponse(
                            titlerec!!,
                            fixUrl(linkrec),
                            this.name,
                            TvType.NSFW,
                            fixUrl(thumbrec),
                            null
                    )

                }

            }


            //finaliza la parte de relleno de recomendados
            return newMovieLoadResponse(
                    titleName,
                    url,
                    TvType.NSFW,
                    url
            ) {
                posterUrl = fixUrlNull(thumb)
                this.plot = null
                this.recommendations = recomm
                this.duration = null
                addActors(lista)
            }
        } catch (e: Exception) {
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

        var videoLoad = ArrayList<String>()
        val f = app.get(data).document.body()
        val z = f.toString().substring(f.toString().indexOf("<script id=\"__NEXT_DATA__\" type=\"application/json\">")+51)
        val gm = z.substring(0,z.indexOf("</script>"))
        val jsonObject = JSONObject(gm)
        val details = jsonObject.getJSONObject("props").getJSONObject("pageProps").getJSONObject("details")

        val resources = details.getJSONObject("resources")
        val resourceKeys = resources.keys()
        while (resourceKeys.hasNext()) {
            val key = resourceKeys.next()
            val resourceArray = resources.getJSONArray(key)
            for (i in 0 until resourceArray.length()) {
                val resource = resourceArray.getJSONObject(i)
                val resourceUrl = resource.getString("url")
                println("Resource URL ($key): $resourceUrl")
                videoLoad.add(resourceUrl)
            }
        }

        videoLoad.map { videos ->
            fetchUrls(videos).map {
                it.replace("https://dooood.com", "https://dood.ws")
                        .replace("https://dood.sh", "https://dood.ws")
                        .replace("https://dood.la", "https://dood.ws")
                        .replace("https://ds2play.com", "https://dood.ws")
                        .replace("https://dood.to", "https://dood.ws")
                        .replace("https://d0000d.com","https://dood.ws")
            }.apmap {
                loadExtractor(it, data, subtitleCallback, callback)
            }
        }






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