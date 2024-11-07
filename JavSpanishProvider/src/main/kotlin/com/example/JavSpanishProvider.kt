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
import org.jsoup.nodes.Element
import java.util.*


class JavSpanishProvider : MainAPI() {
    companion object {
        fun getType(t: String): TvType {
            return if (t.contains("OVA") || t.contains("Especial")) TvType.OVA
            else if (t.contains("Pelicula")) TvType.AnimeMovie
            else TvType.Anime
        }
    }

    private val globalTvType = TvType.NSFW
    override var mainUrl = "https://javenspanish.com/"
    override var name = "JavEnSpanish"
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


    override suspend fun getMainPage(page: Int, request : MainPageRequest): HomePageResponse {
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

        var texto: String
        var inicio: Int
        var ultimo: Int
        var link2: String
        var z: Int
        var poster =""

        val categoryData = request.data
        val categoryName = request.name
        val pagedLink = if (page > 0) categoryData + page else categoryData
        val soup = app.get(pagedLink).document
        val home = soup.select("#post-31 > div > div > div > div > section.elementor-section.elementor-top-section.elementor-element.elementor-element-543e45e.elementor-section-stretched.elementor-section-boxed.elementor-section-height-default.elementor-section-height-default > div > div > div > div > div > div.elementor-element.elementor-element-0034ef1.elementor-grid-4.elementor-posts--align-center.elementor-posts__hover-none.elementor-grid-tablet-2.elementor-grid-mobile-1.elementor-posts--thumbnail-top.elementor-card-shadow-yes.elementor-widget.elementor-widget-posts > div > div > article.elementor-post.elementor-grid-item").mapNotNull {
            if (it == null) { return@mapNotNull null }
            val title = it.selectFirst("div h3")?.text() ?: ""
            val link = fixUrlNull(it.selectFirst("a")?.attr("href")) ?: return@mapNotNull null
            //val img = fetchImgUrl(it.selectFirst("img"))
            texto = it.selectFirst("a div img").toString()
            inicio = texto.indexOf("data-lazy-srcset") + 18
            ultimo = texto.length
            link2 = texto.substring(inicio,ultimo).toString()
            z = link2.indexOf(" ")
            poster = link2.substring(0,z).toString()


            MovieSearchResponse(
                    name = title,
                    url = link,
                    apiName = this.name,
                    type = globalTvType,
                    posterUrl = poster
            )
        }

        val items = ArrayList<HomePageList>()

        urls.apmap { (url, name) ->
            val soup = app.get(url).document
            var texto: String
            var inicio: Int
            var ultimo: Int
            var link: String
            var z: Int
            var poster =""
            val home = soup.select(".elementor-post__card").map {
                val title = it.selectFirst(".elementor-post__title")?.text()
                texto = it.selectFirst(".elementor-post__thumbnail img").toString()
                inicio = texto.indexOf("data-lazy-srcset") + 18
                ultimo = texto.length
                link = texto.substring(inicio,ultimo).toString()
                z = link.indexOf(" ")
                poster = link.substring(0,z).toString()

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
        //return HomePageResponse(items)



        if (home.isNotEmpty()) {
            return newHomePageResponse(
                    list = HomePageList(
                            name = categoryName,
                            list = home,
                            isHorizontalImages = true
                    ),
                    hasNext = true
            )
        } else {
            throw ErrorLoadingException("No homepage data found!")
        }

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
        val url = "$mainUrl/video/search?search=${query}"
        val document = app.get(url).document
        return document.select("div.sectionWrapper div.wrap").mapNotNull {
            if (it == null) { return@mapNotNull null }
            val title = it.selectFirst("span.title a")?.text() ?: return@mapNotNull null
            val link = fixUrlNull(it.selectFirst("a")?.attr("href")) ?: return@mapNotNull null
            val image = fetchImgUrl(it.selectFirst("img"))
            MovieSearchResponse(
                    name = title,
                    url = link,
                    apiName = this.name,
                    type = globalTvType,
                    posterUrl = image
            )
        }.distinctBy { it.url }
    }
    data class EpsInfo (
            @JsonProperty("number" ) var number : String? = null,
            @JsonProperty("title"  ) var title  : String? = null,
            @JsonProperty("image"  ) var image  : String? = null
    )
    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url, timeout = 120).document
        //val poster = "https://javenspanish.com/wp-content/uploads/2022/01/JUFE-132.jpg"
        val title = doc.selectFirst("#content > div > div > div > div > section > div > div > div > div > div > div.elementor-element.elementor-element-7b148b4.elementor-widget.elementor-widget-heading > div > h1")?.text()?:""
        val type = "NFSW"
        val description = doc.selectFirst("#content > div > div > div > div > section > div > div > div > div > div > div.elementor-element.elementor-element-9da66e1.elementor-widget.elementor-widget-text-editor > div > div > div > div > div > div > div > div > div > div > div > div > div > div > div > div > div > div > span")?.text()
        var texto: String
        var inicio: Int
        var ultimo: Int
        var link: String

        texto = doc.selectFirst("#elementor-frontend-js-before").toString()
        ultimo = texto.length
        inicio = texto.indexOf("featuredImage\":\"") + 16
        link = texto.substring(inicio,ultimo).toString()
        val poster = link.substring(0,link.indexOf("\"")).replace("\\","")
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
        try {
            val x = app.get(data).document
            var texto: String
            var inicio: Int
            var ultimo: Int
            var link: String
            var z: Int
            var url =""
            //val url = x.selectFirst("#elementor-tab-content-7233 > div > iframe")?.attr("src")?:""
            //texto = x.selectFirst("#elementor-tab-content-7233 > div > iframe").toString()

            texto = x.selectFirst("body").toString()
            ultimo = texto.length
            if(texto.contains("dooood.com")){
                inicio = texto.indexOf("https://dooood.com")
                link = texto.substring(inicio,ultimo).toString()
                url = link.substring(0,link.indexOf("\"")).replace("dooood.com", "dood.ws")
            }
            else if(texto.contains("dood.ws")){
                inicio = texto.indexOf("https://dood.ws")
                link = texto.substring(inicio,ultimo).toString()
                url = link.substring(0,link.indexOf("\"")).replace("dood.ws", "dood.ws")
            }
            else if(texto.contains("dood.sh")){
                inicio = texto.indexOf("https://dood.sh")
                link = texto.substring(inicio,ultimo).toString()
                url = link.substring(0,link.indexOf("\"")).replace("dood.sh", "dood.ws")
            }
            else if(texto.contains("dood.la")){
                inicio = texto.indexOf("https://dood.la")
                link = texto.substring(inicio,ultimo).toString()
                url = link.substring(0,link.indexOf("\"")).replace("dood.la", "dood.ws")
            }
            //val url = "https://voe.sx/e/cc6lejcng05n"

                //Log.i(this.name, "ApiError => (link url) $linkUrl")
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
    }
    private fun fetchImgUrl(imgsrc: Element?): String? {
        return try { imgsrc?.attr("data-src")
                ?: imgsrc?.attr("data-mediabook")
                ?: imgsrc?.attr("alt")
                ?: imgsrc?.attr("data-mediumthumb")
                ?: imgsrc?.attr("data-thumb_url")
                ?: imgsrc?.attr("src")
        } catch (e:Exception) { null }
    }
}