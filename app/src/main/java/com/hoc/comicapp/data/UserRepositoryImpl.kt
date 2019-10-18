package com.hoc.comicapp.data

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.storage.FirebaseStorage
import com.hoc.comicapp.domain.models.ComicAppError
import com.hoc.comicapp.domain.models.toError
import com.hoc.comicapp.domain.repository.UserRepository
import com.hoc.comicapp.utils.Either
import com.hoc.comicapp.utils.left
import com.hoc.comicapp.utils.right
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit
import timber.log.Timber

class UserRepositoryImpl(
  private val firebaseAuth: FirebaseAuth,
  private val firebaseStorage: FirebaseStorage,
  private val retrofit: Retrofit
) : UserRepository {
  override suspend fun register(
    email: String,
    password: String,
    fullName: String,
    avatar: Uri?
  ): Either<ComicAppError, Unit> {
    return try {

      val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
      val user = authResult.user ?: throw IllegalStateException("User is null")

      val photoUri = if (avatar != null) {
        Timber.d("Upload start")
        val reference = firebaseStorage.getReference("avatar_images/${user.uid}")
        reference.putFile(avatar).await()
        reference.downloadUrl.await().also { Timber.d("Upload done") }
      } else {
        null
      }

      user.updateProfile(
        UserProfileChangeRequest
          .Builder()
          .setDisplayName(fullName)
          .setPhotoUri(photoUri)
          .build()
      ).await()

      Unit.right()
    } catch (e: Exception) {
      Timber.d("register error $e")
      e.toError(retrofit).left()
    }
  }

  override suspend fun login(email: String, password: String): Either<ComicAppError, Unit> {
    return try {
      firebaseAuth.signInWithEmailAndPassword(email, password).await()
      Unit.right()
    } catch (e: Exception) {
      e.toError(retrofit).left()
    }
  }
}