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


class JavGGProvider : MainAPI() {
    companion object {
        fun getType(t: String): TvType {
            return if (t.contains("OVA") || t.contains("Especial")) TvType.OVA
            else if (t.contains("Pelicula")) TvType.AnimeMovie
            else TvType.Anime
        }
    }

    override var mainUrl = "https://javgg.net/"
    override var name = "JavGG"
    override var lang = "es"
    override val hasMainPage = true
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(
            TvType.NSFW
    )
    override val mainPage = mainPageOf(
            "$mainUrl/featured/page/" to "Main Page",
    )
    val type = TvType.NSFW

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val urls = listOf(
                Pair(
                        "$mainUrl/random",
                        "Random"
                ),
                Pair(
                        "$mainUrl/genre/nasty/",
                        "Nasty"
                ),
        )
        val pagedLink = if (page > 0) "$mainUrl/featured/page/" + page  else mainUrl+"/featured/"
        val items = ArrayList<HomePageList>()
        var texto: String
        var inicio: Int
        var ultimo: Int
        var link: String
        var z: Int

        items.add(
                HomePageList(
                        "Recientes",
                        app.get(mainUrl+"/featured/").document.select("div.items article").map {
                            val title = it.selectFirst("h3 a")?.text()
                            val dubstat = if (title!!.contains("Latino") || title.contains("Castellano"))
                                DubStatus.Dubbed else DubStatus.Subbed
                            //val poster = it.selectFirst("a div img")?.attr("src") ?: ""

                            val poster = it.selectFirst("img")?.attr("src")
                            val url = it.selectFirst("h3 a")?.attr("href") ?: ""


                            newAnimeSearchResponse(title, url) {
                                this.posterUrl = poster
                                addDubStatus(dubstat)
                            }
                        })
        )
        urls.apmap { (url, name) ->
            val soup = app.get(url).document

            val home = soup.select("div.items article").map {
                val title = it.selectFirst("h3 a")?.text()
                val poster = it.selectFirst("img")?.attr("src").toString()
                AnimeSearchResponse(
                        title!!,
                        fixUrl(it.selectFirst("a")?.attr("href") ?: ""),
                        this.name,
                        TvType.Anime,
                        fixUrl(poster),
                        null,
                        if (title.contains("Latino") || title.contains("Castellano")) EnumSet.of(
                                DubStatus.Dubbed
                        ) else EnumSet.of(DubStatus.Subbed),
                )
            }
            items.add(HomePageList(name, home))
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
        val poster = doc.selectFirst("#contenedor link")?.attr("href")
        val title = doc.selectFirst(".sheader h1")?.text()?:""
        val des= doc.selectFirst("#cover").toString()
        val final = des.indexOf("<br>")
        val dess = des.substring(final+4)
        val descri = if(dess.contains("<br>")) dess.substring(0,dess.indexOf("<br>")) else dess.substring(0,dess.indexOf("</p>"))


        //Fin espacio prueba
        return MovieLoadResponse(
                name = title,
                url = url,
                apiName = this.name,
                type = type,
                dataUrl = url,
                plot = descri,
                posterUrl = poster
        )

    }

    override suspend fun loadLinks(
            data: String,
            isCasting: Boolean,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ): Boolean {
        app.get(data).document.select("#playaa").mapNotNull{
                val videos = it.attr("src")
                fetchUrls(videos).map {
                    it.replace("https://dooood.com", "https://dood.ws")
                            .replace("https://dood.sh", "https://dood.ws")
                            .replace("https://dood.la","https://dood.ws")
                            .replace("https://javggvideo.xyz/t","https://emturbovid.com/t")
                            .replace("https://javlion.xyz/v","https://vidhidevip.com/embed")
                }.apmap {
                    loadExtractor(it, data, subtitleCallback, callback)
                }
            }

        return true
    }
}