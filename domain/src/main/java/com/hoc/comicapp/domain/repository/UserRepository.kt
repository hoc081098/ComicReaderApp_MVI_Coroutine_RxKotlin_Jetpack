package com.hoc.comicapp.domain.repository

import com.chrynan.uri.core.Uri
import com.hoc.comicapp.domain.DomainResult
import com.hoc.comicapp.domain.models.User
import io.reactivex.rxjava3.core.Observable

interface UserRepository {
  suspend fun login(email: String, password: String): DomainResult<Unit>

  suspend fun signOut(): DomainResult<Unit>

  suspend fun register(
    email: String,
    password: String,
    fullName: String,
    avatar: Uri?,
  ): DomainResult<Unit>

  fun userObservable(): Observable<DomainResult<User?>>
}
