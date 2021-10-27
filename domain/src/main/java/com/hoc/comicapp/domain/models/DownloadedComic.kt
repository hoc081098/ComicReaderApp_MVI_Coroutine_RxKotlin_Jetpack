package com.hoc.comicapp.domain.models

data class DownloadedComic(
  val authors: List<Author>,

  val categories: List<Category>,

  val lastUpdated: String, // April 2019

  val comicLink: String, // https://ww2.mangafox.online/solo-leveling

  val shortenedContent: String, // Solo Leveling summary: 10 years ago, after “the Gate” that connected the real world with the monster world opened, some of the ordinary, everyday people received the power to hunt monsters within the Gate. They are known as "Hunters". However, not all Hunters are powerful. My name is Sung Jin-Woo, an E-rank Hunter. I'm someone who has to risk his life in the lowliest of dungeons, the "World's Weakest". Having no skills whatsoever to display, I barely earned the required money by fighting in low-leveled dungeons… at least until I found a hidden dungeon with the hardest difficulty within the D-rank dungeons! In the end, as I was accepting death, I suddenly received a strange power, a quest log that only I could see, a secret to leveling up that only I know about! If I trained in accordance with my quests and hunted monsters, my level would rise. Changing from the weakest Hunter to the strongest S-rank Hunter!

  val thumbnail: String, // https://cdn1.mangafox.online/900/018/013/989/430/solo-leveling.jpg

  val title: String, // Solo Leveling

  val view: String, // 76228

  val chapters: List<DownloadedChapter>,

  val remoteThumbnail: String,
) {
  data class Category(
    val link: String, // https://ww2.mangafox.online/category/webtoons
    val name: String, // Webtoons
  )

  data class Author(
    val link: String, // https://ww2.mangafox.online/author/sung-lak-jang
    val name: String, // Sung-Lak Jang
  )
}
