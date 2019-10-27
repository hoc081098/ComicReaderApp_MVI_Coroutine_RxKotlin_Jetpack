package com.hoc.comicapp.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.ServerTimestamp
import com.hoc.comicapp.domain.models.AuthError
import com.hoc.comicapp.domain.models.ComicAppError
import com.hoc.comicapp.domain.models.FavoriteComic
import com.hoc.comicapp.domain.models.toError
import com.hoc.comicapp.domain.repository.FavoriteComicsRepository
import com.hoc.comicapp.domain.thread.CoroutinesDispatcherProvider
import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import com.hoc.comicapp.utils.Either
import com.hoc.comicapp.utils.left
import com.hoc.comicapp.utils.right
import com.hoc.comicapp.utils.snapshots
import io.reactivex.Observable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import retrofit2.Retrofit

class FavoriteComicsRepositoryImpl(
  private val firebaseAuth: FirebaseAuth,
  private val firebaseFirestore: FirebaseFirestore,
  private val retrofit: Retrofit,
  private val rxSchedulerProvider: RxSchedulerProvider,
  private val dispatcherProvider: CoroutinesDispatcherProvider
) : FavoriteComicsRepository {
  @Suppress("ClassName")
  @IgnoreExtraProperties
  private data class _FavoriteComic(
    @get:PropertyName("url") @set:PropertyName("url") var url: String,
    @get:PropertyName("title") @set:PropertyName("title") var title: String,
    @get:PropertyName("thumbnail") @set:PropertyName("thumbnail") var thumbnail: String,
    @get:PropertyName("view") @set:PropertyName("view") var view: String,
    @get:ServerTimestamp @get:PropertyName("created_at") @set:PropertyName("created_at") var createdAt: Timestamp?
  ) {
    fun toDomain(): FavoriteComic {
      return FavoriteComic(
        title = title,
        thumbnail = thumbnail,
        createdAt = createdAt?.toDate(),
        url = url,
        view = view
      )
    }

    @Suppress("unused")
    constructor() : this(
      url = "",
      createdAt = null,
      thumbnail = "",
      title = "",
      view = ""
    )
  }

  override fun isFavorited(url: String): Observable<Either<ComicAppError, Boolean>> {
    val collection = favoriteCollectionForCurrentUserOrNull
      ?: return Observable.just(AuthError.Unauthenticated.left())

    return collection
      .whereEqualTo("url", url)
      .limit(1)
      .snapshots()
      .map { it.documents.isNotEmpty().right() as Either<ComicAppError, Boolean> }
      .onErrorReturn { t: Throwable -> t.toError(retrofit).left() }
      .subscribeOn(rxSchedulerProvider.io)
  }

  private fun FavoriteComic.toEntity(): _FavoriteComic {
    return _FavoriteComic(
      url = url,
      title = title,
      thumbnail = thumbnail,
      view = view,
      createdAt = null
    )
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
      .subscribeOn(rxSchedulerProvider.io)
  }

  override suspend fun removeFromFavorite(comic: FavoriteComic): Either<ComicAppError, Unit> {
    return withContext(dispatcherProvider.io) {
      try {
        val snapshot = findQueryDocumentSnapshotByUrl(comic.url)
          ?: error("snapshot is null")
        if (!snapshot.exists()) {
          error("snapshot is not exists")
        }
        snapshot.reference.delete().await()
        Unit.right()
      } catch (e: Exception) {
        e.toError(retrofit).left()
      }
    }
  }

  override suspend fun toggle(comic: FavoriteComic) {
    return withContext(dispatcherProvider.io) {
      val snapshot = findQueryDocumentSnapshotByUrl(comic.url)
      if (snapshot?.exists() == true) {
        snapshot.reference.delete().await()
      } else {
        (favoriteCollectionForCurrentUserOrNull
          ?: throw AuthError.Unauthenticated).add(comic.toEntity()).await()
      }
      Unit
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
