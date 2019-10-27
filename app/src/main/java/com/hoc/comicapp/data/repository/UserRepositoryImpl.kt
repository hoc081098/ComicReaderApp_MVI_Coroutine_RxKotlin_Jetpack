package com.hoc.comicapp.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.storage.FirebaseStorage
import com.hoc.comicapp.domain.models.ComicAppError
import com.hoc.comicapp.domain.models.User
import com.hoc.comicapp.domain.models.toError
import com.hoc.comicapp.domain.repository.UserRepository
import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import com.hoc.comicapp.utils.Either
import com.hoc.comicapp.utils.Optional
import com.hoc.comicapp.utils.fold
import com.hoc.comicapp.utils.left
import com.hoc.comicapp.utils.map
import com.hoc.comicapp.utils.right
import com.hoc.comicapp.utils.snapshots
import com.hoc.comicapp.utils.toOptional
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit
import timber.log.Timber

class UserRepositoryImpl(
  private val firebaseAuth: FirebaseAuth,
  private val firebaseStorage: FirebaseStorage,
  private val firebaseFirestore: FirebaseFirestore,
  private val retrofit: Retrofit,
  private val rxSchedulerProvider: RxSchedulerProvider
) : UserRepository {

  @Suppress("ClassName")
  @IgnoreExtraProperties
  private data class _User(
    @get:PropertyName("uid") @set:PropertyName("uid") var uid: String,
    @get:PropertyName("display_name") @set:PropertyName("display_name") var displayName: String,
    @get:PropertyName("email") @set:PropertyName("email") var email: String,
    @get:PropertyName("photo_url") @set:PropertyName("photo_url") var photoURL: String
  ) {
    @Suppress("unused")
    constructor() : this("", "", "", "")

    fun toDomain() = User(
      uid = uid,
      displayName = displayName,
      photoURL = photoURL,
      email = email
    )
  }

  override suspend fun signOut(): Either<ComicAppError, Unit> {
    return try {
      firebaseAuth.signOut()
      Unit.right()
    } catch (e: Exception) {
      e.toError(retrofit).left()
    }
  }

  override fun userObservable(): Observable<Either<ComicAppError, User?>> {
    val uidObservable = Observable
      .create { emitter: ObservableEmitter<Optional<String>> ->
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
          if (!emitter.isDisposed) {
            val uid = auth.currentUser.toOptional().map { it.uid }
            emitter.onNext(uid)
          }
        }

        firebaseAuth.addAuthStateListener(authStateListener)
        emitter.setCancellable {
          firebaseAuth.removeAuthStateListener(authStateListener)
          Timber.d("Remove auth state listener")
        }
      }

    return uidObservable
      .distinctUntilChanged()
      .switchMap { uidOptional ->
        uidOptional.fold(
          ifEmpty = { Observable.just(null.right()) },
          ifSome = { uid ->
            firebaseFirestore
              .document("users/$uid")
              .snapshots()
              .map {
                it.toObject(_User::class.java)
                  ?.toDomain()
                  .right() as Either<ComicAppError, User?>
              }
              .onErrorReturn { t: Throwable -> t.toError(retrofit).left() }
              .subscribeOn(rxSchedulerProvider.io)
          }
        )
      }
      .subscribeOn(rxSchedulerProvider.io)
      .doOnNext { Timber.d("User = $it") }
      .replay(1)
      .refCount()
  }

  override suspend fun register(
    email: String,
    password: String,
    fullName: String,
    avatar: Uri?
  ): Either<ComicAppError, Unit> {
    return try {

      val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
      val user = authResult.user ?: throw IllegalStateException("User is null")

      coroutineScope {
        val uploadPhotoDeferred = async {
          if (avatar != null) {
            Timber.d("Upload start")
            val reference = firebaseStorage.getReference("avatar_images/${user.uid}")
            reference.putFile(avatar).await()
            reference.downloadUrl.await().also { Timber.d("Upload done") }
          } else {
            null
          }
        }

        firebaseFirestore
          .document("users/${user.uid}")
          .set(
            _User(
              uid = user.uid,
              displayName = fullName,
              photoURL = "",
              email = email
            )
          )
          .await()

        val photoUri = uploadPhotoDeferred.await()

        awaitAll(
          async {
            user
              .updateProfile(
                UserProfileChangeRequest
                  .Builder()
                  .setDisplayName(fullName)
                  .setPhotoUri(photoUri)
                  .build()
              )
              .await()
          },
          async {
            firebaseFirestore
              .document("users/${user.uid}")
              .update("photo_url", photoUri?.toString() ?: "")
              .await()
          }
        )
      }

      Unit.right()
    } catch (e: Exception) {
      Timber.d("register error $e")
      delay(1_000)
      e.toError(retrofit).left()
    }
  }

  override suspend fun login(email: String, password: String): Either<ComicAppError, Unit> {
    return try {
      firebaseAuth.signInWithEmailAndPassword(email, password).await()
      Unit.right()
    } catch (e: Exception) {
      delay(1_000)
      e.toError(retrofit).left()
    }
  }
}
