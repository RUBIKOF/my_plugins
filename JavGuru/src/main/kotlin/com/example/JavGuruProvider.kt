package com.example


import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.Qualities
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

        val pagedLink = if (page > 0) "https://jav.guru/page/" + page else "https://jav.guru/"
        val items = ArrayList<HomePageList>()
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
   private fun cleanExtractor(
           source: String,
           name: String,
           url: String,
           callback: (ExtractorLink) -> Unit
   ): Boolean {
       callback(
               ExtractorLink(
                       source,
                       name,
                       url,
                       "",
                       Qualities.Unknown.value,
                       false
               )
       )
       return true
   }
   override suspend fun loadLinks(
           data: String,
           isCasting: Boolean,
           subtitleCallback: (SubtitleFile) -> Unit,
           callback: (ExtractorLink) -> Unit
   ): Boolean {//https://dood.ws/e/aujxzir2eoim
       //val f = listOf("https://streamtape.net/e/4zv4vA4y9rI284/","https://streamtape.com/e/4zv4vA4y9rI284/","https://ds2play.com/e/gli2qcwpmtvl","https://v.javmix.me/vod/player.php?fl=w5qy7qg1xc6g")
       //val f = listOf("https://fastbrisk.com/e/pkbr5o5n7hvc","https://strwish.com/e/hlsubpw1u660")
       //val f = listOf("https://streamwish.top/e/k2hzsia9ltjp","https://sfastwish.com/e/ujcmxn1hw8at","https://flaswish.com/e/pkbr5o5n7hvc","https://flaswish.com/e/hlsubpw1u660")
       //val f = listOf("https://streamwish.top/e/k2hzsia9ltjp","https://flaswish.com/e/pkbr5o5n7hvc","https://streamtape.com/e/4zv4vA4y9rI284/","https://filemoon.wf/e/709h63gf9arj/PPPD-786_Seduce_al_novio_de_su_hermana_-_Yuria_Yoshine ")
       //SIIIval f = listOf("https://streamtape.net/e/4zv4vA4y9rI284/","https://mixdrop.to/e/4dkx80jmaqln1lj","https://mixdrop.ps/e/4dkx80jmaqln1lj")
       //val f = listOf("https://dood.ws/e/aujxzir2eoim","https://playerwish.com/e/k2hzsia9ltjp","https://wishfast.top/e/k2hzsia9ltjp","https://wishembed.pro/e/k2hzsia9ltjp","https://embedwish.com/e/k2hzsia9ltjp","https://dwish.pro/e/k2hzsia9ltjp","https://mwish.pro/e/k2hzsia9ltjp")
       val f = listOf("https://streamtape.net/e/4zv4vA4y9rI284/","https://watchsb.com/v/0eo0wpztwnkz","https://ww7.embedsito.com/e/673826059907","https://streamsb.net/e/0eo0wpztwnkz","https://fastbrisk.com/e/pkbr5o5n7hvc","https://flaswish.com/e/pkbr5o5n7hvc","https://embedwish.com/e/k2hzsia9ltjp","https://mixdrop.ps/f/4dkx80jmaqln1lj","https://fastbrisk.com/e/pkbr5o5n7hvc")
       //val f = listOf("https://vidhidevip.com/embed/lkbj6k7ipxrf","https://filemoon.wf/e/oytgjufcbzsd/SSNI-469_Stripers_de_fantasia_-_Yumi_Shion","https://moon-4uemks89-embed.com/ptsd/oytgjufcbzsd")
       //val x = listOf("https://wishembed.pro/e/tkxnrvvcmr7d","https://emturbovid.com/t/67153f54ab258","https://emturbovid.com/t/J4GYkYjO0QdxWSERr7dJ","https://streamcdn.info/play/4KFi5bH0uv44Dx4LOJhXmb4KH0KmjM2HV8goTBKj8NNZ.html")
       f.mapNotNull{videos ->
           fetchUrls(videos).map {
               it.replace("https://dooood.com", "https://dood.ws")
                       .replace("https://dood.sh", "https://dood.ws")
                       .replace("https://dood.la","https://dood.ws")
                       .replace("https://ds2play.com","https://dood.ws")
                       .replace("https://dood.to","https://dood.ws")
           }.apmap {
               loadExtractor(it, data, subtitleCallback, callback)
               /*if(it.contains("dood")){
                   for (i in 0 .. 3){
                       if (i ==0){
                           loadExtractor(it, data, subtitleCallback, callback)
                       }
                       if(i == 1){
                           loadExtractor(it.replace("ws","la"), data, subtitleCallback, callback)
                       }
                       if(i == 2){
                           loadExtractor(it.replace("ws","sh"), data, subtitleCallback, callback)
                       }
                       if(i == 3){
                           loadExtractor(it.replace("ws","to"), data, subtitleCallback, callback)
                       }

                   }
               }*/


             /*  if (it.contains("flaswish") == true) {
                   cleanExtractor(
                           "Fireload",
                           "Fireload ",
                           it,
                           callback
                   )
               }*/

           }
       }

       return true
   }
}