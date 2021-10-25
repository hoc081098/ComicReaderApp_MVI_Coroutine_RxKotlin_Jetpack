package com.hoc.comicapp.data.firebase.favorite_comics

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.hoc.comicapp.data.firebase.entity._FavoriteComic
import com.hoc.comicapp.data.firebase.user.FirebaseAuthUserDataSource
import com.hoc.comicapp.domain.models.AuthError
import com.hoc.comicapp.domain.thread.CoroutinesDispatchersProvider
import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import com.hoc.comicapp.utils.snapshots
import com.hoc.comicapp.utils.unit
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@OptIn(ExperimentalTime::class, ObsoleteCoroutinesApi::class)
class FavoriteComicsDataSourceImpl(
  private val firebaseAuth: FirebaseAuth,
  private val firebaseFirestore: FirebaseFirestore,
  private val rxSchedulerProvider: RxSchedulerProvider,
  private val dispatchersProvider: CoroutinesDispatchersProvider,
  private val firebaseAuthUserDataSource: FirebaseAuthUserDataSource,
  appCoroutineScope: CoroutineScope,
) : FavoriteComicsDataSource {
  private val actor = appCoroutineScope.actor<List<_FavoriteComic>>(capacity = Channel.BUFFERED) {
    for (comics in this) {
      _updateComics(comics)
    }
  }

  @Suppress("FunctionName")
  private suspend fun _updateComics(comics: List<_FavoriteComic>) {
    measureTime {
      Timber.d("[UPDATE_COMICS] Start size=${comics.size}")

      val collection = favoriteCollectionForCurrentUserOrNull ?: return@measureTime
      val documents = collection.get().await().documents

      firebaseFirestore
        .runBatch { writeBatch ->
          documents.fold(writeBatch) { batch, doc ->
            when (val comic = comics.find { it.url == doc["url"] }) {
              null -> batch
              else -> batch.update(doc.reference, comic.asMap())
            }
          }
        }
        .await()
    }.let { Timber.d("[UPDATE_COMICS] Done all $it") }
  }

  override fun isFavorited(url: String): Observable<Either<Throwable, Boolean>> {
    return firebaseAuthUserDataSource
      .userObservable()
      .switchMap { either ->
        either.fold(
          { Observable.just(it.left()) },
          {
            val collection = favoriteCollectionForCurrentUserOrNull
              ?: return@fold Observable.just(AuthError.Unauthenticated.left())

            collection
              .whereEqualTo("url", url)
              .limit(1)
              .snapshots()
              .map<Either<Throwable, Boolean>> { it.documents.isNotEmpty().right() }
              .onErrorReturn { it.left() }
              .subscribeOn(rxSchedulerProvider.io)
          }
        )
      }
  }

  override fun favoriteComics(): Observable<Either<Throwable, List<_FavoriteComic>>> {
    return firebaseAuthUserDataSource
      .userObservable()
      .switchMap { either ->
        either.fold(
          { Observable.just(it.left()) },
          {
            val collection = favoriteCollectionForCurrentUserOrNull
              ?: return@fold Observable.just(AuthError.Unauthenticated.left())

            collection
              .orderBy("created_at", Query.Direction.DESCENDING)
              .snapshots()
              .map { querySnapshot ->
                @Suppress("USELESS_CAST")
                querySnapshot
                  .documents
                  .mapNotNull { it.toObject(_FavoriteComic::class.java) }
                  .distinctBy { it.url }
                  .right() as Either<Throwable, List<_FavoriteComic>>
              }
              .onErrorReturn { it.left() }
              .subscribeOn(rxSchedulerProvider.io)
          }
        )
      }
  }

  override suspend fun removeFromFavorite(comic: _FavoriteComic) = Either.catch {
    withContext(dispatchersProvider.io) {
      val snapshot = findQueryDocumentSnapshotByUrl(comic.url)
      if (snapshot?.exists() == true) {
        snapshot.reference.delete()
          .await()
          .unit
      } else {
        error("Comic is not exists")
      }
    }
  }

  override suspend fun toggle(comic: _FavoriteComic) = Either.catch {
    withContext(dispatchersProvider.io) {
      val snapshot = findQueryDocumentSnapshotByUrl(comic.url)
      if (snapshot?.exists() == true) {
        snapshot.reference.delete().await()
        Timber.d("Remove from favorites: $comic")
      } else {
        (
          favoriteCollectionForCurrentUserOrNull
            ?: throw AuthError.Unauthenticated
          ).add(comic).await()
        Timber.d("Insert to favorites: $comic")
      }
    }
  }

  override fun update(comics: List<_FavoriteComic>) = actor.trySend(comics).unit

  private val favoriteCollectionForCurrentUserOrNull: CollectionReference?
    get() = firebaseAuth.currentUser?.uid?.let {
      firebaseFirestore.collection("users/$it/favorite_comics")
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
