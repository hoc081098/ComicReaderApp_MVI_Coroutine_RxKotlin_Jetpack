package com.hoc.comicapp.data.remote.response

import androidx.annotation.Keep
import com.squareup.moshi.Json

@Keep
data class ComicDetailResponse(
  @Json(name = "authors")
  val authors: List<Author>,
  @Json(name = "categories")
  val categories: List<Category>,
  @Json(name = "chapters")
  val chapters: List<Chapter>,
  @Json(name = "last_updated")
  val lastUpdated: String, // April 2019
  @Json(name = "link")
  val link: String, // https://ww2.mangafox.online/solo-leveling
  @Json(name = "related_comics")
  val relatedComics: List<ComicResponse>,
  @Json(name = "shortened_content")
  val shortenedContent: String, // Solo Leveling summary: 10 years ago, after “the Gate” that connected the real world with the monster world opened, some of the ordinary, everyday people received the power to hunt monsters within the Gate. They are known as "Hunters". However, not all Hunters are powerful. My name is Sung Jin-Woo, an E-rank Hunter. I'm someone who has to risk his life in the lowliest of dungeons, the "World's Weakest". Having no skills whatsoever to display, I barely earned the required money by fighting in low-leveled dungeons… at least until I found a hidden dungeon with the hardest difficulty within the D-rank dungeons! In the end, as I was accepting death, I suddenly received a strange power, a quest log that only I could see, a secret to leveling up that only I know about! If I trained in accordance with my quests and hunted monsters, my level would rise. Changing from the weakest Hunter to the strongest S-rank Hunter!
  @Json(name = "thumbnail")
  val thumbnail: String, // https://cdn1.mangafox.online/900/018/013/989/430/solo-leveling.jpg
  @Json(name = "title")
  val title: String, // Solo Leveling
  @Json(name = "view")
  val view: String, // 76228
) {
  @Keep
  data class Chapter(
    @Json(name = "chapter_link")
    val chapterLink: String, // https://ww2.mangafox.online/solo-leveling/chapter-0-275968490470920
    @Json(name = "chapter_name")
    val chapterName: String, // Chapter 0
    @Json(name = "time")
    val time: String, // December 2018
    @Json(name = "view")
    val view: String, // 9592
  )

  @Keep
  data class Category(
    @Json(name = "link")
    val link: String, // https://ww2.mangafox.online/category/webtoons
    @Json(name = "name")
    val name: String, // Webtoons
  )

  @Keep
  data class Author(
    @Json(name = "link")
    val link: String, // https://ww2.mangafox.online/author/sung-lak-jang
    @Json(name = "name")
    val name: String, // Sung-Lak Jang
  )
}
