package com.hoc.comicapp.domain.models

data class ComicDetail(
  val author: String, // ONE - Murata Yuusuke
  val categories: List<Category>,
  val chapters: List<Chapter>,
  val lastUpdated: String, // 16:27 04/06/2019
  val link: String, // http://www.nettruyen.com/truyen-tranh/cu-dam-huy-diet
  val otherName: String?, // Onepunch-Man; Saitama - Onepunch
  val shortenedContent: String, // Một Manga thể loại siêu anh hùng với đặc trưng phồng tôm đấm phát chết luôn... và mang đậm tính chất troll của tác giả.Onepunch-man là câu chuyện của 1 chàng thanh niên 23 tuổi, đang là một nhân viên văn phòng điển trai nghiêm túc và tất nhiên là ế. Không hiểu vì biến cố gì mà tự nhiên lông tóc trên người của anh trụi lủi, sau đó anh mang trong mình khả năng siêu đặc biệt "Đấm phát chết luôn" nhằm bảo vệ trái đất và thành phố nơi anh sinh sống khỏi các sinh vật ngoài không gian (nhưng phá hoại cũng không kém).
  val status: String, // Đang tiến hành
  val thumbnail: String, // https://3.bp.blogspot.com/-0RYOSGO6K5Q/Wy2QUUXiyrI/AAAAAAAAVwI/iqc4vmYsCN87Pgn823WpPRa7fZ9t2P6OACHMYCw/cu-dam-huy-diet
  val title: String, // Cú Đấm Hủy Diệt
  val view: String // 31.510.981
) {
  data class Category(
    val link: String, // http://www.nettruyen.com/tim-truyen/manga
    val name: String // Manga
  )

  data class Chapter(
    val chapterLink: String, // http://www.nettruyen.com/truyen-tranh/cu-dam-huy-diet/chap-0/168970
    val chapterName: String, // Chapter 0: Special chap
    val time: String, // 25/03/15
    val view: String // 253.478
  )
}