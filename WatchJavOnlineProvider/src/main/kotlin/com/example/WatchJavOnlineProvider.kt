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


class WatchJavOnlineProvider : MainAPI() {
    companion object {
        fun getType(t: String): TvType {
            return if (t.contains("OVA") || t.contains("Especial")) TvType.OVA
            else if (t.contains("Pelicula")) TvType.AnimeMovie
            else TvType.Anime
        }
    }

    override var mainUrl = "https://watchjavonline.com/"
    override var name = "WatchJavOnline"
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
    val type = TvType.NSFW

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {

        val pagedLink = if (page > 0) "$mainUrl/page/" + page  else mainUrl
        val items = ArrayList<HomePageList>()
        var texto: String
        var inicio: Int
        var ultimo: Int
        var link: String
        var z: Int

        items.add(
                HomePageList(
                        "Recientes",
                        app.get(pagedLink).document.select(".g1-collection-items .g1-collection-item").map {
                            val title = it.selectFirst("h3 a")?.text()
                            val dubstat = if (title!!.contains("Latino") || title.contains("Castellano"))
                                DubStatus.Dubbed else DubStatus.Subbed
                            //val poster = it.selectFirst("a div img")?.attr("src") ?: ""

                            val poster = it.selectFirst(".g1-frame-inner img")?.attr("src")
                            val url = it.selectFirst(".entry-featured-media a")?.attr("href") ?: ""


                            newAnimeSearchResponse(title, url) {
                                this.posterUrl = poster
                                addDubStatus(dubstat)
                            }
                        },isHorizontalImages = true)
        )

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
                    .select(".search-page").select(".result-item").mapNotNull {
                        val image = it.selectFirst(".image img")?.attr("src")
                        val title = it.selectFirst(".title a")?.text().toString()
                        val url = fixUrlNull(it.selectFirst(".image a")?.attr("href") ?: "") ?: return@mapNotNull null


                        MovieSearchResponse(
                                title,
                                url,
                                this.name,
                                type,
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
        val doc = app.get(url, timeout = 120).document
        val poster = doc.selectFirst(".entry-inner .g1-frame img")?.attr("src")
        val title = doc.selectFirst(".entry-inner h1")?.text()?:""


        ///espacio prueba





            var x =""
            val xx = doc.selectFirst(".GTTabs_divs iframe")?.attr("src").toString()
            app.get(xx).document.select("body script").mapNotNull {
                val video = it.text()
                if (video.contains("MDCore.ref")) {
                    val i = video.indexOf(";")
                    x = "https://mixdrop.ps/e/" + video.substring(0, i).replace("\nMDCore.ref = ", "")
                            .replace("\"", "").replace(" ", "")
                }
            }





        //Fin espacio prueba
        return MovieLoadResponse(
                name = title,
                url = url,
                apiName = this.name,
                type = type,
                dataUrl = url,
                plot = x,
                posterUrl = poster
        )

    }

    override suspend fun loadLinks(
            data: String,
            isCasting: Boolean,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ): Boolean {
        app.get(data).document.select(".GTTabs_divs iframe").mapNotNull{

            var x =""
            val url = it.attr("src")
            app.get(url).document.select("body script").mapNotNull {
                val video = it.text()
                if(video.contains("MDCore.ref")){
                    val i = video.indexOf(";")
                    x = "https://mixdrop.ps/e/" + video.substring(0,i).replace("\nMDCore.ref = ", "")
                            .replace("\"","").replace(" ","")
                }
            }.apmap {
                    loadExtractor(x, data, subtitleCallback, callback)
                }
            }

        return true
    }
}