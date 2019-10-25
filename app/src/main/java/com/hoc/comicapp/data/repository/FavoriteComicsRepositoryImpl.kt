package com.hoc.comicapp.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QueryDocumentSnapshot
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
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit

class FavoriteComicsRepositoryImpl(
  private val firebaseAuth: FirebaseAuth,
  private val firebaseFirestore: FirebaseFirestore,
  private val retrofit: Retrofit
) : FavoriteComicsRepository {
  @Suppress("ClassName")
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

  private fun FavoriteComic.toEntity(): _FavoriteComic {
    TODO()
  }

  private val favoriteCollectionForCurrentUserOrNull: CollectionReference?
    get() = firebaseAuth.currentUser?.uid?.let {
      firebaseFirestore.collection("users/${it}/favorite_comics")
    }

  override fun favoriteComics(): Observable<Either<ComicAppError, List<FavoriteComic>>> {
    val collection = favoriteCollectionForCurrentUserOrNull
      ?: return Observable.just(AuthError.Unauthenticated.left())

    return collection
      .orderBy("created_at", Query.Direction.DESCENDING)
      .snapshots()
      .map { querySnapshot ->
        querySnapshot
          .documents
          .mapNotNull { it.toObject(_FavoriteComic::class.java) }
          .map { it.toDomain() }
          .distinctBy { it.url }
          .right() as Either<ComicAppError, List<FavoriteComic>>
      }
      .onErrorReturn { t: Throwable -> t.toError(retrofit).left() }
  }

  override suspend fun addToFavorite(comic: FavoriteComic) {
    val snapshot = findQueryDocumentSnapshotByUrl(comic.url)
    if (snapshot != null) {
      error("snapshot is not null")
    }

    (favoriteCollectionForCurrentUserOrNull ?: throw AuthError.Unauthenticated)
      .add(comic.toEntity())
      .await()
  }

  override suspend fun removeFromFavorite(comic: FavoriteComic) {
    val snapshot = findQueryDocumentSnapshotByUrl(comic.url)
      ?: error("snapshot is null")
    snapshot.reference.delete().await()
  }

  override suspend fun toggle(comic: FavoriteComic) {
    val snapshot = findQueryDocumentSnapshotByUrl(comic.url)
    if (snapshot === null) {
      (favoriteCollectionForCurrentUserOrNull
        ?: throw AuthError.Unauthenticated).add(comic.toEntity()).await()
    } else {
      snapshot.reference.delete().await()
    }
  }

  /**
   * @return [QueryDocumentSnapshot] or null
   * @throws AuthError.Unauthenticated if not logged in
   */
  @Throws(AuthError.Unauthenticated::class)
  private suspend fun findQueryDocumentSnapshotByUrl(url: String): QueryDocumentSnapshot? {
    return (favoriteCollectionForCurrentUserOrNull ?: throw AuthError.Unauthenticated)
      .whereEqualTo("url", url)
      .limit(1)
      .get()
      .await()
      .firstOrNull()
  }
}
