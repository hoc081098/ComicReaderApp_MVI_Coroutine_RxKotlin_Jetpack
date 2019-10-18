package com.hoc.comicapp.data

import com.google.firebase.auth.FirebaseAuth
import com.hoc.comicapp.domain.models.ComicAppError
import com.hoc.comicapp.domain.models.toError
import com.hoc.comicapp.domain.repository.UserRepository
import com.hoc.comicapp.utils.Either
import com.hoc.comicapp.utils.left
import com.hoc.comicapp.utils.right
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit

class UserRepositoryImpl(
  private val firebaseAuth: FirebaseAuth,
  private val retrofit: Retrofit
) : UserRepository {
  override suspend fun login(email: String, password: String): Either<ComicAppError, Unit> {
    return try {
      firebaseAuth.signInWithEmailAndPassword(email, password).await()
      Unit.right()
    } catch (e: Exception) {
      e.toError(retrofit).left()
    }
  }
}