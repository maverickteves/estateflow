# Estateflow

## GROUP MEMBERS

- Lim, Ryan James M.
- Salazar, Lhord Donnie D.
- Sasing, Dan Joseph
- Teves, Maverick B.

Estateflow is a modern Android real-estate app built with Kotlin and Jetpack Compose. It is designed to help buyers browse listings, save favorite properties, book viewings, and message agents, while also giving agents a dashboard to manage listings, appointments, inquiries, and notifications.

The app blends a polished property marketplace UI with local persistence, backend sync, and role-based behavior so it can function as a demo property platform or a foundation for a production real-estate experience.

## App overview

Estateflow supports two primary user roles:

- Buyer: browse homes, filter by price and amenities, save listings, book viewings, and contact agents.
- Agent: manage property inventory, publish or edit listings, respond to inquiries, monitor appointments, and review profile activity.

The app starts with a welcome flow, then routes into either the home screen or the agent dashboard depending on the selected role. Most screens are powered by a central `EstateViewModel`, which coordinates live data from the repository, local Room database, and backend sync calls.

## Main features

### Property browsing
- Search and filter listings by category, price, bedrooms, bathrooms, square footage, year built, and amenities.
- Browse property cards with key stats, images, and listing status.
- Open detailed property pages to view descriptions, media, and agent information.
- Save favorite properties for later access.

### Buyer experience
- Book viewings for selected listings.
- Submit inquiries and message agents.
- View notifications and appointment updates.
- Toggle between buyer and agent perspectives for demonstration or testing.

### Agent experience
- Access an agent dashboard with overview metrics and listing management tools.
- Add, edit, and delete properties.
- Review appointments associated with their listings.
- Respond to buyer inquiries.
- Monitor notifications and account information.

### Backend and sync
- Uses a repository layer to combine local database storage with remote sync.
- Monitors backend connectivity and updates status indicators in the UI.
- Supports pull-to-refresh and sync-driven updates for listings, messaging, bookings, and notifications.

## Architecture

The project follows a straightforward MVVM pattern with Compose UI:

- `app/src/main/java/com/example/MainActivity.kt` boots the app and hosts the main navigation and layout.
- `ui/viewmodel/EstateViewModel.kt` manages application state, navigation, filters, and app-level events.
- `data/repository/EstateRepository.kt` coordinates the local database and remote/backend data access.
- `data/local/` contains Room database and DAO definitions.
- `data/remote/` contains backend API and sync helpers.
- `ui/screens/` contains the screens for listings, profile, chat, calendar, dashboard, and more.
- `ui/dialogs/` contains modal flows such as booking, auth, filters, and listing forms.

## Technology stack

- Kotlin
- Jetpack Compose
- Material 3
- Room database
- Retrofit + OkHttp + Moshi
- Coroutines + Flow
- Lifecycle + ViewModel
- Gradle Kotlin DSL

## Project structure

```text
estateflow/
├── app/
│   ├── build.gradle.kts
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/com/example/...
│   │   │   └── res/
│   │   └── test/java/com/example/...
│   └── proguard-rules.pro
├── backend/
├── gradle/
├── build.gradle.kts
├── gradlew
├── gradlew.bat
├── settings.gradle.kts
├── gradle.properties
└── readme.md
```

## Getting started

### Prerequisites
- Android Studio with the Android SDK installed
- JDK 11 or newer
- An Android emulator or a physical Android device

### Run the app
1. Open the project in Android Studio.
2. Let Gradle sync the project.
3. Select an emulator/device.
4. Run the app from the `app` module.

### Command line build

```bash
cd estateflow
./gradlew assembleDebug
```

## Notes for local backend/demo use

The app includes a backend configuration flow and is designed to work with a local or remote data source. In debug builds, cleartext traffic is enabled to support local development environments. This is useful for testing against a local backend or mock server while developing the app.

## Intended use

This application serves as a demonstration real-estate platform with a polished UI and realistic workflows. It is suitable for:

- product demos
- learning Jetpack Compose and Android app architecture
- UI prototyping for real-estate solutions
- extending into a production-ready marketplace app

## Summary

Estateflow is a full-featured property marketplace app that combines browsing, filtering, saved homes, bookings, messaging, agent workflows, and backend connectivity in one Android experience. It is a strong starting point for a real-estate product or a learning project focused on modern Android app design and architecture.
