# ComicReaderApp_MVI_Coroutine_RxKotlin
<!-- ALL-CONTRIBUTORS-BADGE:START - Do not remove or modify this section -->
[![All Contributors](https://img.shields.io/badge/all_contributors-1-orange.svg?style=flat-square)](#contributors-)
<!-- ALL-CONTRIBUTORS-BADGE:END -->
Comic reader app 📘. Learning MVVM / MVI with RxKotlin, Retrofit, Kotlin Coroutines

[![Build CI](https://github.com/hoc081098/ComicReaderApp_MVI_Coroutine_RxKotlin_Jetpack/actions/workflows/build.yml/badge.svg)](https://github.com/hoc081098/ComicReaderApp_MVI_Coroutine_RxKotlin_Jetpack/actions/workflows/build.yml)
[![Build Release APK](https://github.com/hoc081098/ComicReaderApp_MVI_Coroutine_RxKotlin_Jetpack/actions/workflows/build-release.yml/badge.svg)](https://github.com/hoc081098/ComicReaderApp_MVI_Coroutine_RxKotlin_Jetpack/actions/workflows/build-release.yml)
[![Spotless check](https://github.com/hoc081098/ComicReaderApp_MVI_Coroutine_RxKotlin_Jetpack/actions/workflows/spotless.yml/badge.svg)](https://github.com/hoc081098/ComicReaderApp_MVI_Coroutine_RxKotlin_Jetpack/actions/workflows/spotless.yml)
[![Validate Gradle Wrapper](https://github.com/hoc081098/ComicReaderApp_MVI_Coroutine_RxKotlin_Jetpack/actions/workflows/gradle-wrapper-validation.yml/badge.svg)](https://github.com/hoc081098/ComicReaderApp_MVI_Coroutine_RxKotlin_Jetpack/actions/workflows/gradle-wrapper-validation.yml)
[![Hits](https://hits.seeyoufarm.com/api/count/incr/badge.svg?url=https%3A%2F%2Fgithub.com%2Fhoc081098%2FComicReaderApp_MVI_Coroutine_RxKotlin_Jetpack&count_bg=%2379C83D&title_bg=%23555555&icon=&icon_color=%23E7E7E7&title=hits&edge_flat=false)](https://hits.seeyoufarm.com)

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

- [Download latest debug APK here](https://nightly.link/hoc081098/ComicReaderApp_MVI_Coroutine_RxKotlin_Jetpack/workflows/build/master/app-debug.zip)
- [Download latest release APK here](https://nightly.link/hoc081098/ComicReaderApp_MVI_Coroutine_RxKotlin_Jetpack/actions/runs/1360316687/app-release.zip)

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

## Contributors ✨

Thanks goes to these wonderful people ([emoji key](https://allcontributors.org/docs/en/emoji-key)):

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
<!-- prettier-ignore-start -->
<!-- markdownlint-disable -->
<table>
  <tr>
    <td align="center"><a href="https://www.linkedin.com/in/hoc081098/"><img src="https://avatars.githubusercontent.com/u/36917223?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Petrus Nguyễn Thái Học</b></sub></a><br /><a href="https://github.com/hoc081098/ComicReaderApp_MVI_Coroutine_RxKotlin_Jetpack/commits?author=hoc081098" title="Code">💻</a> <a href="#maintenance-hoc081098" title="Maintenance">🚧</a> <a href="#ideas-hoc081098" title="Ideas, Planning, & Feedback">🤔</a> <a href="#design-hoc081098" title="Design">🎨</a> <a href="https://github.com/hoc081098/ComicReaderApp_MVI_Coroutine_RxKotlin_Jetpack/issues?q=author%3Ahoc081098" title="Bug reports">🐛</a></td>
  </tr>
</table>

<!-- markdownlint-restore -->
<!-- prettier-ignore-end -->

<!-- ALL-CONTRIBUTORS-LIST:END -->

This project follows the [all-contributors](https://github.com/all-contributors/all-contributors) specification. Contributions of any kind welcome!