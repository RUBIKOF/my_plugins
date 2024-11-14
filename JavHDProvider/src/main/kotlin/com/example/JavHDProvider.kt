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
        items.add(
                HomePageList(
                        "Recientes",
                        app.get(pagedLink).document.select(".videos li").map {
                            val hrefsave = it.selectFirst("a")?.attr("href").toString()
                            val url = if (hrefsave.contains("http")) hrefsave else mainUrl + hrefsave
                            val title = it.selectFirst(".video-thumb img")?.attr("alt").toString()
                            val img = it.selectFirst(".video-thumb img")?.attr("src").toString()
                            val poster = if (img.contains("http")) img else mainUrl + img

                            /*val dubstat = if (title!!.contains("Latino") || title.contains("Castellano"))
                                DubStatus.Dubbed else DubStatus.Subbed*/
                            //val poster = it.selectFirst("a div img")?.attr("src") ?: ""

                            newAnimeSearchResponse(title, url) {
                                this.posterUrl = poster
                                this.type = TvType.NSFW
                            }
                        }, isHorizontalImages = false)
        )
        urls.apmap { (url, name) ->
            var pagedLink = ""
            if (url.contains("mother")) {
                pagedLink = if (page > 1) "$mainUrl/mother/recent/" + page else "$mainUrl/mother/"
            } else if (url.contains("popular")) {
                pagedLink = if (page > 1) "$mainUrl/popular/recent/" + page else "$mainUrl/popular/"
            }
            val soup = app.get(pagedLink).document

            val home = soup.select(".videos li").map {
                val hrefsave = it.selectFirst("a")?.attr("href").toString()
                var url = if (hrefsave.contains("http")) hrefsave else mainUrl + hrefsave
                val title = it.selectFirst(".video-thumb img")?.attr("alt")
                val img = it.selectFirst(".video-thumb img")?.attr("src").toString()
                val poster = if (img.contains("http")) img else mainUrl + img
                AnimeSearchResponse(
                        title!!,
                        fixUrl(url),
                        this.name,
                        TvType.NSFW,
                        fixUrl(poster),
                        null
                )
            }
            items.add(HomePageList(name, home, isHorizontalImages = false))
        }

        if (items.size <= 0) throw ErrorLoadingException()
        return HomePageResponse(items, hasNext = true)

    }

    override suspend fun search(query: String): List<SearchResponse> {


        return app.get("$mainUrl/search/video/?s=$query").document
                .select(".panel-body.panel-padding").select(".videos li").mapNotNull {
                    val saveimage = it.selectFirst("img")?.attr("src").toString()
                    val image = if(saveimage.contains("http")) saveimage else mainUrl + saveimage
                    val title = it.selectFirst(".video .thumbnail")?.attr("title").toString()
                    val saveurl = fixUrlNull(it.selectFirst("a")?.attr("href") ?: "")
                            ?: return@mapNotNull null
                    val url = if(saveurl.contains("http")) saveurl else mainUrl + saveurl


                    MovieSearchResponse(
                            title,
                            url,
                            this.name,
                            TvType.NSFW,
                            image
                    )
                }

    }

    override suspend fun load(url: String): LoadResponse {


        val doc = app.get(url, timeout = 120).document
        val poster = doc.selectFirst(".col-xs-12.col-sm-12.col-md-12")?.attr("src")
        val title = doc.selectFirst("#video > div.left.content.content-video > div > h1")?.text() ?: ""
        val type = TvType.NSFW
        val des = doc.selectFirst(".description")?.text().toString()
        val description = if(des.contains("工")) des.substring(0,des.indexOf("工")) else des

        //test tmp

        val starname = ArrayList<String>()
        val lista = ArrayList<Actor>()

        doc.select("#video-actors a").mapNotNull {
            starname.add(it.attr("title"))
        }

        /////Fin espacio prueba


        //parte para rellenar la lista recomendados
            val recomm = doc.select(".videos.related li").mapNotNull {
                val hrefsave = mainUrl + it.selectFirst("a")!!.attr("href")
                val href = if(hrefsave.contains("http")) hrefsave else mainUrl + hrefsave
                val img = it.selectFirst("img")?.attr("src") ?: ""
                val posterUrl = if(img.contains("http")) img else mainUrl + img
                val name = it.selectFirst(".video-title")?.text() ?: ""
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