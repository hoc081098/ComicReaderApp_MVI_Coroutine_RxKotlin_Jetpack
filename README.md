# ComicReaderApp_MVI_Coroutine_RxKotlin
Comic reader app 📘. Learning MVVM / MVI with RxKotlin, Retrofit, Kotlin Coroutines

# Project features 🚀
This project brings to the table set of best practices, tools, and solutions:

-   100% [Kotlin](https://kotlinlang.org/)
-   Kotlin Coroutines with Flow
-   Functional & Reactive programming with [RxKotlin](https://github.com/ReactiveX/RxKotlin), [RxJava3](https://github.com/ReactiveX/RxJava)
-   Clean Architecture with MVI (Uni-directional data flow)
-   [Λrrow - Functional companion to Kotlin's Standard Library](https://arrow-kt.io/)
       - [Either](https://arrow-kt.io/docs/apidocs/arrow-core/arrow.core/-either/)
       - [Option](https://arrow-kt.io/docs/apidocs/arrow-core/arrow.core/-option/)
       - [parZip](https://arrow-kt.io/docs/fx/async/#parzip)
-   Modern architecture (Clean Architecture, Model-View-ViewModel)
-   Navigation, single-activity architecture with [Jetpack Navigation](https://developer.android.com/guide/navigation)
-   Initialize components at application startup with [AndroidX Startup](https://developer.android.com/topic/libraries/app-startup)
-   Cache local data with [Room Persistence Library](https://developer.android.com/topic/libraries/architecture/room)
-   Schedule tasks with [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
-   ViewModel, LiveData, Lifecycle, ... with [Android Jetpack](https://developer.android.com/jetpack)
-   Dependency injection [Koin](https://insert-koin.io/)
-   Material Design [Material Components for Android](https://github.com/material-components/material-components-android)
-   Kotlin Gradle DSL
-   Firebase: Firestore, Authentication, Storage, Analytics, Crashlytics
-   Gradle Kotlin DSL, Custom plugin

## Download APK

[Download latest debug APK here](https://nightly.link/hoc081098/ComicReaderApp_MVI_Coroutine_RxKotlin_Jetpack/workflows/build/master/app-debug.zip)

# Develop
- You must use **Android Studio Arctic Fox (2020.3.1)** (**note: Java 11 is now the minimum version required**)
- Clone: `git clone https://github.com/hoc081098/ComicReaderApp_MVI_Coroutine_RxKotlin.git`
- _Optional: **Delete `.idea` folder** if cannot open project_
- Open project by `Android Studio` and run as usual

# Screenshots

|                         |                         |                         |                         |
|        :---:            |          :---:          |        :---:            |          :---:          |
| ![](screenshots/1.jpeg) | ![](screenshots/2.jpeg) | ![](screenshots/3.jpeg) | ![](screenshots/4.jpeg) |
| ![](screenshots/5.jpeg) | ![](screenshots/6.jpeg) | ![](screenshots/7.jpeg) | ![](screenshots/8.jpeg) |
| ![](screenshots/9.jpeg) | ![](screenshots/10.png) | ![](screenshots/11.png) | ![](screenshots/12.png) |
| ![](screenshots/13.png) | ![](screenshots/14.png) | ![](screenshots/15.png) |                         |

# LOC

```sh
--------------------------------------------------------------------------------
 Language             Files        Lines        Blank      Comment         Code
--------------------------------------------------------------------------------
 Kotlin                 159        15144         1759          636        12749
 XML                     95         5182          465           80         4637
 JSON                     3          117            0            0          117
 Prolog                   7          116           16            0          100
 Markdown                 1           74           12            0           62
 Batch                    1           84           23            0           61
--------------------------------------------------------------------------------
 Total                  266        20717         2275          716        17726
--------------------------------------------------------------------------------
```

# Server and API

Clone this repository: https://github.com/hoc081098/comic_app_server_nodejs

# License

    MIT License

    Copyright (c) 2019-2021 Petrus Nguyễn Thái Học
