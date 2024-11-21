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
import java.util.*


class JavTsunamiProvider : MainAPI() {
    companion object {
        fun getType(t: String): TvType {
            return if (t.contains("OVA") || t.contains("Especial")) TvType.OVA
            else if (t.contains("Pelicula")) TvType.AnimeMovie
            else TvType.Anime
        }
    }

    override var mainUrl = "https://javtsunami.com/"
    override var name = "JavTsunami"
    override var lang = "es"
    override val hasMainPage = true
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(
            TvType.NSFW
    )
    override val mainPage = mainPageOf(
            "$mainUrl/page/1?filter=latest" to "Main Page",
    )
    val type = TvType.NSFW

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val urls = listOf(
                Pair(
                        "$mainUrl/category/featured/",
                        "Featured"
                ),
                Pair(
                        "$mainUrl/category/amateur/",
                        "Amateur"
                ),
                Pair(
                        "$mainUrl/category/japanese/",
                        "Japanese"
                ),
                Pair(
                        "$mainUrl/category/milf/",
                        "Milf"
                ),
                Pair(
                        "$mainUrl/category/jav-censored/",
                        "Censored"
                ),

        )
        val pagedLink = if (page > 0) "$mainUrl/page/" + page + "?filter=latest" else "$mainUrl/?filter=latest"
        val items = ArrayList<HomePageList>()
        var texto: String
        var inicio: Int
        var ultimo: Int
        var link: String
        var z: Int

        items.add(
                HomePageList(
                        "Recientes",
                        app.get(pagedLink).document.select("#primary .videos-list article").map {
                            val title = it.selectFirst("header span")?.text()
                            val dubstat = if (title!!.contains("Latino") || title.contains("Castellano"))
                                DubStatus.Dubbed else DubStatus.Subbed
                            //val poster = it.selectFirst("a div img")?.attr("src") ?: ""

                            val poster = it.selectFirst(".post-thumbnail img")?.attr("data-src")
                            val url = it.selectFirst("a")?.attr("href") ?: ""


                            newAnimeSearchResponse(title, url) {
                                this.posterUrl = poster
                                addDubStatus(dubstat)
                            }
                        },isHorizontalImages = true)
        )
        urls.apmap { (url, name) ->

            val pagedLink = if (page > 0) "$url/page/" + page else url

            val soup = app.get(pagedLink).document
            var texto: String
            var inicio: Int
            var ultimo: Int
            var link: String
            var z: Int
            var poster = ""
            val home = soup.select("#primary .videos-list article").map {
                val title = it.selectFirst("header span")?.text()
                val poster = it.selectFirst(".post-thumbnail img")?.attr("data-src").toString()
                val url = it.selectFirst("a")?.attr("href") ?: ""
                AnimeSearchResponse(
                        title!!,
                        fixUrl(url),
                        this.name,
                        TvType.NSFW,
                        fixUrl(poster),
                        null
                )
            }
            items.add(HomePageList(name, home,isHorizontalImages = true))
        }

        if (items.size <= 0) throw ErrorLoadingException()
        return HomePageResponse(items, hasNext = true)

    }

    override suspend fun search(query: String): List<SearchResponse> {

        val soup = app.get("$mainUrl//?s=$query").document

            return soup.select(".g1-collection-items").select("li").mapNotNull {
                        val image = it.selectFirst(".entry-featured-media a img")?.attr("src")
                        val title = it.selectFirst(" .entry-featured-media  a")?.attr(("title")).toString()
                        val url = fixUrlNull(it.selectFirst(".entry-featured-media a")?.attr("href") ?: "") ?: return@mapNotNull null


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
        var ultimo: Int
        var link: String

        try {
            val doc = app.get(url, timeout = 120).document
            val title = doc.selectFirst("article h1")?.text() ?: ""
            val type = "NFSW"
            val poster = doc.selectFirst("#video-about .video-description img")?.attr("data-lazy-src")
            //val poster =""
            //val description = doc.selectFirst("article p")?.text()

            //test tmp
            var description = ""
            app.get(url).document.select("div.box-server > a ").mapNotNull {
                val videos = it.attr("onclick")
                fetchUrls(videos).map {
                    description += it.replace("https://v.javmix.me/vod/player.php?", "")
                            .replace("')", "")
                            .replace("stp=", "https://streamtape.com/e/")
                            .replace("do=", "https://dood.ws/e/") + "\n"

                }
            }

            var starname = ArrayList<String>()
            var lista = ArrayList<Actor>()

            doc.select("#video-actors a").mapNotNull {
                starname.add(it.attr("title"))
            }
            if (starname.size>0) {

                for(i in 0 .. starname.size-2){
                    app.get("https://www.javdatabase.com/idols/" + starname[i].replace(" ","-")).document.select("#main ").mapNotNull {
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

            /*app.get(url).document.select("#video-actor a").mapNotNull {
               val nombre = it.text()
               //Actor(it.text().trim(), it.select("img").attr("src"))
               fetchUrls(nombre).map {
                    actors2 = app.get("https://www.javdatabase.com/idols/" + nombre.replace(" ", "-"))
                           .document.select(".entry-content").mapNotNull {
                               val imgstar = doc.selectFirst("img")?.attr("src")
                               Actor(nombre, imgstar)
                           }

               }
           }*/
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
                        TvType.NSFW,
                        posterUrl
                )

            }
            //finaliza la parte de relleno de recomendados
            return newMovieLoadResponse(
                    title,
                    url,
                    TvType.NSFW,
                    url
            ) {
                posterUrl = fixUrlNull(poster)
                this.plot = description
                this.recommendations = recomm
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

        app.get(data).document.select(".GTTabs_divs iframe").mapNotNull{
            val videos = it.attr("src")

            var vid =""
            var doc = app.get(videos, timeout = 120).document.body().toString()
            if(doc.contains("MDCore.ref")){
                val md = doc.indexOf("MDCore.ref =")
                val st = doc.substring(md+12)
                val final = st.indexOf(";")
                vid = "https://mixdrop.ps/e/" + st.substring(0,final).replace("\"", "").replace(" ", "")
            }else{
                vid = ""
            }
            loadExtractor(vid, data, subtitleCallback, callback)
        }

        return true
    }
}