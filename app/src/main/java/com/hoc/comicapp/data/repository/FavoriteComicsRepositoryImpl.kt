package com.hoc.comicapp.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.hoc.comicapp.domain.models.AuthError
import com.hoc.comicapp.domain.models.ComicAppError
import com.hoc.comicapp.domain.models.FavoriteComic
import com.hoc.comicapp.domain.models.toError
import com.hoc.comicapp.domain.repository.FavoriteComicsRepository
import com.hoc.comicapp.utils.Either
import com.hoc.comicapp.utils.left
import com.hoc.comicapp.utils.right
import com.hoc.comicapp.utils.snapshots
import io.reactivex.Observable
import retrofit2.Retrofit

class FavoriteComicsRepositoryImpl(
  private val firebaseAuth: FirebaseAuth,
  private val firebaseFirestore: FirebaseFirestore,
  private val retrofit: Retrofit
) : FavoriteComicsRepository {
  @IgnoreExtraProperties
  private data class _FavoriteComic(
    @PropertyName("url") val url: String,
    @PropertyName("title") val title: String,
    @PropertyName("thumbnail") val thumbnail: String,
    @PropertyName("created_at") val createdAt: Timestamp?
  ) {
    fun toDomain(): FavoriteComic {
      return FavoriteComic(
        title = title,
        thumbnail = thumbnail,
        createdAt = createdAt?.toDate(),
        url = url
      )
    }

    @Suppress("unused")
    constructor() : this(
      url = "",
      createdAt = null,
      thumbnail = "",
      title = ""
    )
  }

  override fun favoriteComics(): Observable<Either<ComicAppError, List<FavoriteComic>>> {
    val currentUser = firebaseAuth.currentUser
    return if (currentUser === null) {
      Observable.just(AuthError.Unauthenticated.left())
    } else {
      firebaseFirestore
        .collection("users/${currentUser.uid}/favorite_comics")
        .snapshots()
        .map { querySnapshot ->
          querySnapshot
            .documents
            .mapNotNull { it.toObject(_FavoriteComic::class.java) }
            .map { it.toDomain() }
            .right() as Either<ComicAppError, List<FavoriteComic>>
        }
        .onErrorReturn { t: Throwable -> t.toError(retrofit).left() }
    }
  }
}