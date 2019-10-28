package com.hoc.comicapp.data.repository

import android.net.Uri
import com.hoc.comicapp.data.firebase.user.FirebaseAuthUserDataSource
import com.hoc.comicapp.domain.models.ComicAppError
import com.hoc.comicapp.domain.models.User
import com.hoc.comicapp.domain.models.toError
import com.hoc.comicapp.domain.repository.UserRepository
import com.hoc.comicapp.utils.Either
import com.hoc.comicapp.utils.bimap
import com.hoc.comicapp.utils.left
import com.hoc.comicapp.utils.right
import io.reactivex.Observable
import kotlinx.coroutines.delay
import retrofit2.Retrofit
import timber.log.Timber

class UserRepositoryImpl(
  private val retrofit: Retrofit,
  private val userDataSource: FirebaseAuthUserDataSource
) : UserRepository {

  override suspend fun signOut(): Either<ComicAppError, Unit> {
    return try {
      userDataSource.signOut()
      Unit.right()
    } catch (e: Exception) {
      e.toError(retrofit).left()
    }
  }

  override fun userObservable(): Observable<Either<ComicAppError, User?>> {
    return userDataSource.userObservable().map { either ->
      either.bimap(
        { it.toError(retrofit) },
        { it?.toDomain() }
      )
    }
  }

  override suspend fun register(
    email: String,
    password: String,
    fullName: String,
    avatar: Uri?
  ): Either<ComicAppError, Unit> {
    return try {
      userDataSource.register(
        email = email,
        password = password,
        fullName = fullName,
        avatar = avatar
      )
      Unit.right()
    } catch (e: Exception) {
      Timber.d("register error $e")
      delay(1_000)
      e.toError(retrofit).left()
    }
  }

  override suspend fun login(email: String, password: String): Either<ComicAppError, Unit> {
    return try {
      userDataSource.login(email, password)
      Unit.right()
    } catch (e: Exception) {
      delay(1_000)
      e.toError(retrofit).left()
    }
  }
}
