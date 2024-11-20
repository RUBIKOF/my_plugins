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

        //reducido
       /* val doc = app.get(mainUrl).document
        val all = ArrayList<HomePageList>()
        var elements2: List<SearchResponse>
        doc.getElementsByTag("body").select("#content .panel-default")
                .forEach { it2 ->
                    // Fetch row title
                    val savetitle = it2?.select(".panel-title a")?.text().toString()
                    val title = if (savetitle.contains("+")) savetitle.substring(0, savetitle.indexOf("+")) else savetitle
                    //val cate = it2?.select(".panel-title a")?.attr("href").toString()
                    // Fetch list of items and map
                     it2.select(".videos li")
                            .let { inner ->

                                val elements: List<SearchResponse> = inner.mapNotNull {

                                    val hrefsave = it.selectFirst("a")?.attr("href").toString()
                                    val url = if (hrefsave.contains("http")) hrefsave else mainUrl + hrefsave
                                    val title1 = it.selectFirst(".video-thumb img")?.attr("alt").toString()
                                    val img = it.selectFirst(".video-thumb img")?.attr("src").toString()

                                    val year = null

                                    MovieSearchResponse(
                                            name = title1,
                                            url = url,
                                            apiName = this.name,
                                            type = TvType.NSFW,
                                            posterUrl = img,
                                            year = year
                                    )
                                }

                                all.add(
                                        HomePageList(
                                                name = title,
                                                list = elements,
                                                isHorizontalImages = true
                                        )
                                )
                            }
                }
        return HomePageResponse(all)*/
        /////////////////////////////////////////////////////////////////
    //extrae_todo
    /* val document = app.get(mainUrl).document
        val all = ArrayList<HomePageList>()
        var elements2: List<SearchResponse>
        document.getElementsByTag("body").select(" #content .panel-default")
                .forEach { it2 ->
                    // Fetch row title
                    val savetitle = it2?.select(".panel-title a")?.text().toString()
                    val title = if(savetitle.contains("+")) savetitle.substring(0,savetitle.indexOf("+")) else savetitle
                    val cate = it2?.select(".panel-title a")?.attr("href").toString()
                    //val cate ="https://javhd.today/releaseday/"

                    var pagedLink = ""
                           if (page > 1) {
                               if (cate.contains("releaseday") || cate.contains("popular")|| cate.contains("recent")) {
                                   pagedLink = cate + "/" + page
                               } else {
                                   pagedLink = cate + "/recent/" + page
                               }

                           }else {
                        pagedLink = cate
                    }

                    elements2= app.get(pagedLink).document.select(".videos li").mapNotNull{

                        val hrefsave = it.selectFirst("a")?.attr("href").toString()
                        val url = if (hrefsave.contains("http")) hrefsave else mainUrl + hrefsave
                        val title1 = it.selectFirst(".video-thumb img")?.attr("alt").toString()
                        //val title1 = x
                        val img = it.selectFirst(".video-thumb img")?.attr("src").toString()
                        //val poster = if (img.contains("http")) img else mainUrl + img

                        MovieSearchResponse(
                                name = title1,
                                url = url,
                                apiName = this.name,
                                type = TvType.NSFW,
                                posterUrl = img,
                        )
                    }
                    all.add(
                            HomePageList(
                                    name = title,
                                    list = elements2,
                                    isHorizontalImages = false
                            )
                    )
                    // Fetch list of items and map

                }
        return HomePageResponse(all, hasNext = true)*/

////////////////////////////////////////////////////////


        //original
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
                pagedLink = if (page > 1) "$mainUrl/popular/" + page else "$mainUrl/popular/"
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
        val poster = doc.selectFirst("#contenedor > link")?.attr("href")
        val title = doc.selectFirst("#single h1")?.text() ?: ""
        val type = TvType.NSFW
        //val des = doc.selectFirst("#cover.sbox")?.text().toString()
        val des =""
        val description = if(des.contains("工")) des.substring(0,des.indexOf("工")) else des

        //test tmp

        val starname = ArrayList<String>()
        val lista = ArrayList<Actor>()

        doc.select("#video-actors a").mapNotNull {
            starname.add(it.attr("title"))
        }

        /////Fin espacio prueba

        return newMovieLoadResponse(
                title,
                url,
                type,
                url
        ) {
            posterUrl = fixUrlNull(poster)
            this.plot = description
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
                       .replace("https://turbovid.xyz","https://emturbovid.com")

           }.apmap {
               loadExtractor(it, data, subtitleCallback, callback)
           }
       }

       return true
   }
}