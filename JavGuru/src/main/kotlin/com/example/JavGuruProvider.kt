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


class JavGuruProvider : MainAPI() {
    companion object {
        fun getType(t: String): TvType {
            return if (t.contains("OVA") || t.contains("Especial")) TvType.OVA
            else if (t.contains("Pelicula")) TvType.AnimeMovie
            else TvType.Anime
        }
    }

    override var mainUrl = "https://jav.guru/"
    override var name = "JavGuru    "
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
    val saveImage = "";

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val urls = listOf(
                Pair(
                        "$mainUrl/familia",
                        "Familia"
                ),
                Pair(
                        "$mainUrl/milf",
                        "Milf"
                ),
        )
        val categoryName = request.name
        val pagedLink = if (page > 0) categoryName + page else categoryName
        val items = ArrayList<HomePageList>()
        var texto: String
        var inicio: Int
        var ultimo: Int
        var link: String
        var z: Int
        var poster = ""
        items.add(
                HomePageList(
                        "Recientes",
                        app.get(pagedLink).document.select("#main > div").map {
                            val title = it.selectFirst("h2")?.text()
                            val poster = it.selectFirst("img")?.attr("src")
                            val dubstat = if (title!!.contains("Latino") || title.contains("Castellano"))
                                DubStatus.Dubbed else DubStatus.Subbed
                            //val poster = it.selectFirst("a div img")?.attr("src") ?: ""

                            val url = it.selectFirst("a")?.attr("href") ?: ""


                            newAnimeSearchResponse(title, url) {
                                this.posterUrl = poster
                                addDubStatus(dubstat)
                            }
                        })
        )
        urls.apmap { (url, name) ->
            val soup = app.get(url).document
            var texto: String
            var inicio: Int
            var ultimo: Int
            var link: String
            var z: Int
            var poster = ""
            val home = soup.select(".elementor-post__card").map {
                val title = it.selectFirst(".elementor-post__title")?.text()
                texto = it.selectFirst(".elementor-post__thumbnail img").toString()
                inicio = texto.indexOf("data-lazy-srcset") + 18
                ultimo = texto.length
                link = texto.substring(inicio, ultimo).toString()
                z = link.indexOf(" ")
                poster = link.substring(0, z).toString()

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
        val doc = app.get(url, timeout = 120).document
        //val poster = "https://javenspanish.com/wp-content/uploads/2022/01/JUFE-132.jpg"
        val title = doc.selectFirst(".inside-article h1")?.text()?:""
        val type = "NFSW"
        val description = doc.selectFirst("#content > div > div > div > div > section > div > div > div > div > div > div.elementor-element.elementor-element-9da66e1.elementor-widget.elementor-widget-text-editor > div > div > div > div > div > div > div > div > div > div > div > div > div > div > div > div > div > div > span")?.text()

        val poster = doc.selectFirst(".inside-article img")?.attr("src")
        //Fin espacio prueba
        return MovieLoadResponse(
                name = title,
                url = url,
                apiName = this.name,
                type = TvType.NSFW,
                dataUrl = url,
                posterUrl = poster
        )

    }

    override suspend fun loadLinks(
            data: String,
            isCasting: Boolean,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ): Boolean {
        app.get(data).document.select("#h-tabs > div > div > div.elementor-tabs-content-wrapper > div >div >iframe").mapNotNull{
                val videos = it.attr("src")
                fetchUrls(videos).map {
                    it.replace("https://dooood.com", "https://dood.ws")
                            .replace("https://dood.sh", "https://dood.ws")
                            .replace("https://dood.la","https://dood.ws")
                }.apmap {
                    loadExtractor(it, data, subtitleCallback, callback)
                }
            }

        return true
    }
}