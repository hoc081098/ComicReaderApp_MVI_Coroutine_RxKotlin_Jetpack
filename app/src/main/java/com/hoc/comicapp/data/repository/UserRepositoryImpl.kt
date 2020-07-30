package com.hoc.comicapp.data.repository

import android.net.Uri
import com.hoc.comicapp.data.ErrorMapper
import com.hoc.comicapp.data.firebase.user.FirebaseAuthUserDataSource
import com.hoc.comicapp.domain.DomainResult
import com.hoc.comicapp.domain.models.User
import com.hoc.comicapp.domain.repository.UserRepository
import com.hoc.comicapp.utils.bimap
import com.hoc.comicapp.utils.right
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.delay
import timber.log.Timber

class UserRepositoryImpl(
  private val errorMapper: ErrorMapper,
  private val userDataSource: FirebaseAuthUserDataSource,
) : UserRepository {

  override suspend fun signOut(): DomainResult<Unit> {
    return try {
      userDataSource.signOut()
      Unit.right()
    } catch (e: Throwable) {
      errorMapper.mapAsLeft(e)
    }
  }

  override fun userObservable(): Observable<DomainResult<User?>> {
    return userDataSource.userObservable().map { either ->
      either.bimap(errorMapper::map) { it?.toDomain() }
    }
  }

  override suspend fun register(
    email: String,
    password: String,
    fullName: String,
    avatar: Uri?,
  ): DomainResult<Unit> {
    return try {
      userDataSource.register(
        email = email,
        password = password,
        fullName = fullName,
        avatar = avatar
      )
      Unit.right()
    } catch (e: Throwable) {
      Timber.d("register error $e")
      delay(1_000)
      errorMapper.mapAsLeft(e)
    }
  }

  override suspend fun login(email: String, password: String): DomainResult<Unit> {
    return try {
      userDataSource.login(email, password)
      Unit.right()
    } catch (e: Throwable) {
      delay(1_000)
      errorMapper.mapAsLeft(e)
    }
  }
}
