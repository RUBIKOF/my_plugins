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
        val doc = app.get(url, timeout = 120).document
        val poster = doc.selectFirst(".entry-inner .g1-frame img")?.attr("src")
        val title = doc.selectFirst(".entry-inner h1")?.text() ?: ""

        return MovieLoadResponse(
                name = title,
                url = url,
                apiName = this.name,
                type = type,
                dataUrl = url,
                plot = "",
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