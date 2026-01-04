# Project Time Tracker - Android Version

An Android port of the Project Time Tracker desktop application, built with Java and Material Design components.

## Features

- **Time Tracking**: Start, pause, resume, and end time entries for projects and categories
- **Reminders**: Configurable reminder intervals (0, 15, 30, 60, 120 minutes) with vibration and sound alerts
- **Daily Time Pools**: Track daily time budgets for categories and view remaining pool time
- **Three-Section Interface**: Radio button selector for Control Panel, Time Entries, and Category Time Pools
- **File Import/Export**: Load and save data files compatible with Python/Web versions
- **Persistent Storage**: Uses SharedPreferences with JSON serialization
- **Material Design**: Modern Android UI with Material 3 components
- **Category Filtering**: Projects dropdown automatically filters based on selected category

## Data File Compatibility

The Android app can import/export data in the same text format as the Python and Web versions:

### Time Entries (MetaDataProjectTime.txt)
Format: `PROJECT --- CATEGORY --- DURATION_SECONDS --- START_DATETIME`

### Time Pools (MetaDataDailyTimePools.txt)
Format: `CATEGORY --- DAILY_MINUTES`

Use the **Load** and **Save** buttons in each section to:
- Import existing data from other versions
- Export data to share across platforms
- Backup your tracking data

## Project Structure

```
AndroidVersion/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/romanbrunner/apps/projecttimetracker/
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
│   │       │   │   ├── item_time_entry.xml
│   │       │   │   └── item_category_pool.xml
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

## User Interface

The app has three main sections accessible via radio buttons:

### 1. Control Panel
- **Start/End Buttons**: Begin/finish time tracking
- **Reminder Selector**: Choose reminder interval
- **Project/Category Dropdowns**: Select or enter project and category (category filters projects)
- **Time Displays**:
  - Current entry time (live counter)
  - Category pool time (green = remaining, red = over budget)
  - Total project time
  - Total category time
  - Start date

### 2. Time Entries
- **Load/Save Buttons**: Import/export MetaDataProjectTime.txt
- **Entries List**: Shows all recorded entries with:
  - Project name (bold)
  - Category name (below project)
  - Duration (formatted as H:MM:SS)
  - Start date/time
  - Remove button (delete icon)

### 3. Category Time Pools
- **Load/Save Buttons**: Import/export MetaDataDailyTimePools.txt
- **Pool Editor**: Table showing:
  - Category name
  - Daily minutes (editable)
  - Pool time remaining/exceeded (color-coded, with minus sign if negative)
  - Total time spent
- Changes save automatically

## Building

1. Open the project in Android Studio
2. Sync Gradle files
3. Build and run on an Android device or emulator (API 21+)

## Requirements

- Android Studio Arctic Fox or later
- Android SDK 21+ (Android 5.0 Lollipop)
- Java 11+
- Gradle 8.13
- Android Gradle Plugin 8.13.2

## Key Features Explained

### Category-Filtered Projects
When you select a category, the project dropdown automatically shows only projects that have entries in that category, making it easier to find the right project.

### Pool Time Calculation
Pool time = (Daily minutes × Days since first entry) - Total time spent
- Displayed in green if time remaining
- Displayed in red with minus sign if over budget

### File Transfer
To transfer files between PC and Android emulator:
1. Drag and drop files onto emulator screen
2. Files appear in `/sdcard/Download/`
3. Use Load buttons to import data

### Storage
- **Internal**: SharedPreferences stores data between sessions
- **Import/Export**: Text files for cross-platform compatibility

## Package Name

`com.romanbrunner.apps.projecttimetracker`

## License

Same license as the Project Time Tracker application.
