package com.hoc.comicapp.data.remote.response

import androidx.annotation.Keep
import com.squareup.moshi.Json

@Keep
data class CategoryResponse(
  @Json(name = "description")
  val description: String, // Isekai translates to "another world." This subgenre typically has a narrative where a protagonist somehow gets transported to a different world. The new world is more often than not in a fantasy setting, occasionally with traits pulled from JRPG games.This category of anime exploded during the 2010s and arguably dominated the decade. A good portion of isekai anime is adapted not from manga but rather from light novels. The most popular series in this subgenre may be Sword Art Online. While the world featured there was just virtual reality, it did feature a fantasy setting that would be mimicked in other anime.The type of anime has become so prevalent that there is actually a backlash against it now. Story contests in Japan have actually banned isekai stories from being submitted just because of how saturated the market has become with it.
  @Json(name = "link")
  val link: String, // https://ww2.mangafox.online/category/isekai
  @Json(name = "name")
  val name: String, // Isekai
  @Json(name = "thumbnail")
  val thumbnail: String, // https://cdn1.mangafox.online/542/954/495/674/357/the-duchess-50-tea-recipes.jpg
)
