package com.hoc.comicapp.data.remote.response

import com.squareup.moshi.Json

data class ComicDetailResponse(
  @Json(name = "author")
  val author: String, // ONE - Murata Yuusuke
  @Json(name = "categories")
  val categories: List<Category>,
  @Json(name = "chapters")
  val chapters: List<Chapter>,
  @Json(name = "last_updated")
  val lastUpdated: String, // 16:27 04/06/2019
  @Json(name = "link")
  val link: String, // http://www.nettruyen.com/truyen-tranh/cu-dam-huy-diet
  @Json(name = "other_name")
  val otherName: String?, // Onepunch-Man; Saitama - Onepunch
  @Json(name = "shortened_content")
  val shortenedContent: String, // Một Manga thể loại siêu anh hùng với đặc trưng phồng tôm đấm phát chết luôn... và mang đậm tính chất troll của tác giả.Onepunch-man là câu chuyện của 1 chàng thanh niên 23 tuổi, đang là một nhân viên văn phòng điển trai nghiêm túc và tất nhiên là ế. Không hiểu vì biến cố gì mà tự nhiên lông tóc trên người của anh trụi lủi, sau đó anh mang trong mình khả năng siêu đặc biệt "Đấm phát chết luôn" nhằm bảo vệ trái đất và thành phố nơi anh sinh sống khỏi các sinh vật ngoài không gian (nhưng phá hoại cũng không kém).
  @Json(name = "status")
  val status: String, // Đang tiến hành
  @Json(name = "thumbnail")
  val thumbnail: String, // https://3.bp.blogspot.com/-0RYOSGO6K5Q/Wy2QUUXiyrI/AAAAAAAAVwI/iqc4vmYsCN87Pgn823WpPRa7fZ9t2P6OACHMYCw/cu-dam-huy-diet
  @Json(name = "title")
  val title: String, // Cú Đấm Hủy Diệt
  @Json(name = "view")
  val view: String // 31.510.981
) {
  data class Category(
    @Json(name = "link")
    val link: String, // http://www.nettruyen.com/tim-truyen/manga
    @Json(name = "name")
    val name: String // Manga
  )

  data class Chapter(
    @Json(name = "chapter_link")
    val chapterLink: String, // http://www.nettruyen.com/truyen-tranh/cu-dam-huy-diet/chap-0/168970
    @Json(name = "chapter_name")
    val chapterName: String, // Chapter 0: Special chap
    @Json(name = "time")
    val time: String, // 25/03/15
    @Json(name = "view")
    val view: String // 253.478
  )
}