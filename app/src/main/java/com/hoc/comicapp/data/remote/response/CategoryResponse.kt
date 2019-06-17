package com.hoc.comicapp.data.remote.response

import com.squareup.moshi.Json

data class CategoryResponse(
  @Json(name = "description")
  val description: String, // Xuyên Không, Xuyên Việt là thể loại nhân vật chính vì một lý do nào đó mà bị đưa đến sinh sống ở một không gian hay một khoảng thời gian khác. Nhân vật chính có thể trực tiếp xuyên qua bằng thân xác mình hoặc sống lại bằng thân xác người khác.
  @Json(name = "link")
  val link: String, // http://www.nettruyen.com/tim-truyen/xuyen-khong
  @Json(name = "name")
  val name: String, // Xuyên Không
  @Json(name = "thumbnail")
  val thumbnail: String // http://st.nettruyen.com/data/comics/166/ngao-thi-thien-dia.jpg
)