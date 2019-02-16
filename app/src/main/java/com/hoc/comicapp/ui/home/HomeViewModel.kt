package com.hoc.comicapp.ui.home

import androidx.lifecycle.MutableLiveData
import com.hoc.comicapp.CoroutinesDispatcherProvider
import com.hoc.comicapp.Event
import com.hoc.comicapp.base.BaseViewModel
import com.hoc.comicapp.base.Intent
import com.hoc.comicapp.base.SingleEvent
import com.hoc.comicapp.base.ViewState
import com.hoc.comicapp.data.ComicRepository
import com.hoc.comicapp.data.models.Comic
import com.hoc.comicapp.data.remote.ErrorResponseParser
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.Retrofit
import java.io.IOException

sealed class HomeViewState : ViewState {
  object Loading : HomeViewState()
  data class Data(
    val topMonthComics: List<Comic>
  ) : HomeViewState()

  data class Error(
    val message: String
  ) : HomeViewState()
}

sealed class HomeViewIntent : Intent {
  object GetTopMonth : HomeViewIntent()
}

sealed class HomeSingleEvent : SingleEvent {

}

class HomeViewModel(
  coroutinesDispatcherProvider: CoroutinesDispatcherProvider,
  private val comicRepository: ComicRepository,
  private val retrofit: Retrofit
) : BaseViewModel<HomeViewIntent, HomeViewState, HomeSingleEvent>(coroutinesDispatcherProvider) {

  @ObsoleteCoroutinesApi
  override fun processIntents(
    intents: ReceiveChannel<HomeViewIntent>,
    state: MutableLiveData<HomeViewState>,
    singleEvent: MutableLiveData<Event<HomeSingleEvent>>
  ) {
    launch {
      intents.consumeEach {
        when (it) {
          is HomeViewIntent.GetTopMonth -> {
            state.value = HomeViewState.Loading
            try {
              val topMonth = comicRepository.getTopMonth()
              state.value = HomeViewState.Data(topMonth)
            } catch (e: HttpException) {
              val (message) = ErrorResponseParser.getError(e.response(), retrofit)!!
              state.value = HomeViewState.Error(message)
            } catch (e: IOException) {
              state.value = HomeViewState.Error("Error network")
            } catch (t: Throwable) {
              state.value = HomeViewState.Error(t.message ?: "???")
            }
          }
        }
      }
    }
  }

  init {

  }
}