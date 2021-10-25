package com.hoc.comicapp.data.firebase.user

import android.net.Uri
import arrow.core.Either
import com.hoc.comicapp.data.firebase.entity._User
import io.reactivex.rxjava3.core.Observable

interface FirebaseAuthUserDataSource {
  fun userObservable(): Observable<Either<Throwable, _User?>>

  suspend fun signOut(): Either<Throwable, Unit>

  suspend fun register(
    email: String,
    password: String,
    fullName: String,
    avatar: Uri?,
  ): Either<Throwable, Unit>

  suspend fun login(email: String, password: String): Either<Throwable, Unit>
}
