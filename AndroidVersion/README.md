# Project Time Tracker - Android Version

An Android port of the Project Time Tracker desktop application, built with Java.

## Features

- Track time spent on projects and categories
- Start, pause, resume, and end time entries
- Configurable reminder intervals with sound alerts
- Daily time pools for categories
- Persistent storage using SharedPreferences and JSON files
- View history of all time entries

## Project Structure

```
AndroidVersion/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/projecttimetracker/
│   │       │   ├── MainActivity.java
│   │       │   ├── CategoryPoolsActivity.java
│   │       │   ├── model/
│   │       │   │   ├── TimeEntry.java
│   │       │   │   └── DailyTimePool.java
│   │       │   ├── data/
│   │       │   │   ├── TimeEntryRepository.java
│   │       │   │   └── DailyTimePoolRepository.java
│   │       │   └── util/
│   │       │       └── TimeUtils.java
│   │       ├── res/
│   │       │   ├── layout/
│   │       │   │   ├── activity_main.xml
│   │       │   │   ├── activity_category_pools.xml
│   │       │   │   └── item_time_entry.xml
│   │       │   ├── values/
│   │       │   │   ├── strings.xml
│   │       │   │   ├── colors.xml
│   │       │   │   └── styles.xml
│   │       │   └── drawable/
│   │       └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── gradle.properties
```

## Building

1. Open the project in Android Studio
2. Sync Gradle files
3. Build and run on an Android device or emulator

## Requirements

- Android Studio Arctic Fox or later
- Android SDK 21+ (Android 5.0 Lollipop)
- Java 11+
