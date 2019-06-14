package com.hoc.comicapp.domain.models

data class Category(
  val description: String, // Xuyên Không, Xuyên Việt là thể loại nhân vật chính vì một lý do nào đó mà bị đưa đến sinh sống ở một không gian hay một khoảng thời gian khác. Nhân vật chính có thể trực tiếp xuyên qua bằng thân xác mình hoặc sống lại bằng thân xác người khác.
  val link: String, // http://www.nettruyen.com/tim-truyen/xuyen-khong
  val name: String // Xuyên Không
)