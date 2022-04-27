package com.hoc.comicapp.data.firebase.user

import android.net.Uri
import arrow.core.Either
import arrow.core.Option
import arrow.core.left
import arrow.core.right
import arrow.core.toOption
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.hoc.comicapp.data.firebase.entity._User
import com.hoc.comicapp.domain.thread.CoroutinesDispatchersProvider
import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import com.hoc.comicapp.utils.snapshots
import com.hoc.comicapp.utils.unit
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber

class FirebaseAuthUserDataSourceImpl(
  private val firebaseAuth: FirebaseAuth,
  private val firebaseStorage: FirebaseStorage,
  private val firebaseFirestore: FirebaseFirestore,
  private val rxSchedulerProvider: RxSchedulerProvider,
  private val dispatchersProvider: CoroutinesDispatchersProvider,
) : FirebaseAuthUserDataSource {

  private val userObservable: Observable<Either<Throwable, _User?>> by lazy {
    Observable
      .create<Option<String>> { emitter ->
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
          if (!emitter.isDisposed) {
            val uid = auth.currentUser.toOption().map { it.uid }
            emitter.onNext(uid)
          }
        }

        firebaseAuth.addAuthStateListener(authStateListener)
        emitter.setCancellable {
          firebaseAuth.removeAuthStateListener(authStateListener)
          Timber.d("Remove auth state listener")
        }
      }
      .distinctUntilChanged()
      .switchMap { uidOptional ->
        uidOptional.fold(
          ifEmpty = { Observable.just(null.right()) },
          ifSome = { uid ->
            firebaseFirestore
              .document("users/$uid")
              .snapshots()
              .map<Either<Throwable, _User?>> { it.toObject(_User::class.java).right() }
              .onErrorReturn { it.left() }
              .subscribeOn(rxSchedulerProvider.io)
          }
        )
      }
      .subscribeOn(rxSchedulerProvider.io)
      .doOnNext { Timber.d("User[1] = $it") }
      .replay(1)
      .refCount()
      .doOnNext { Timber.d("User[2] = $it") }
  }

  override fun userObservable() = userObservable

  override suspend fun signOut() = Either.catch {
    withContext(dispatchersProvider.io) { firebaseAuth.signOut() }
  }

  override suspend fun register(email: String, password: String, fullName: String, avatar: Uri?) = Either.catch {
    withContext(dispatchersProvider.io) {
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

      Unit
    }
  }

  override suspend fun login(email: String, password: String) = Either.catch {
    withContext(dispatchersProvider.io) {
      firebaseAuth.signInWithEmailAndPassword(email, password).await().unit
    }
  }
}
