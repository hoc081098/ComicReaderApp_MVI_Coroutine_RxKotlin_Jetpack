package com.hoc.comicapp.data.repository

import androidx.core.net.toUri
import com.chrynan.uri.core.Uri
import com.hoc.comicapp.data.ErrorMapper
import com.hoc.comicapp.data.firebase.user.FirebaseAuthUserDataSource
import com.hoc.comicapp.domain.DomainResult
import com.hoc.comicapp.domain.models.User
import com.hoc.comicapp.domain.repository.UserRepository
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.delay
import timber.log.Timber

class UserRepositoryImpl(
  private val errorMapper: ErrorMapper,
  private val userDataSource: FirebaseAuthUserDataSource,
) : UserRepository {

  override suspend fun signOut() = userDataSource.signOut()
    .tapLeft { Timber.e(it, "Failed to sign out") }
    .mapLeft(errorMapper)

  override fun userObservable(): Observable<DomainResult<User?>> =
    userDataSource.userObservable()
      .doOnNext { either ->
        either.tapLeft {
          Timber.e(it, "Failed to observe the user state")
        }
      }
      .map { either -> either.bimap(errorMapper) { it?.toDomain() } }

  override suspend fun register(
    email: String,
    password: String,
    fullName: String,
    avatar: Uri?,
  ) = userDataSource.register(
    email = email,
    password = password,
    fullName = fullName,
    avatar = avatar?.uriString?.toUri()
  )
    .tapLeft {
      Timber.e(it, "Failed to register user")
      delay(1_000)
    }
    .mapLeft(errorMapper)

  override suspend fun login(email: String, password: String) =
    userDataSource.login(email, password)
      .tapLeft {
        Timber.e(it, "Failed to login")
        delay(1_000)
      }
      .mapLeft(errorMapper)
}
