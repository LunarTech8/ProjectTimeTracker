# Project Time Tracker - Android Version

An Android port of the Project Time Tracker desktop application, built with Java and Material Design components.

## Features

- **Time Tracking**: Start, pause, resume, reset, and end time entries for projects and categories
- **Background Reminders**: Configurable reminder intervals with custom input support
  - Beeps and vibration work even when app is in background or phone is locked
  - Visual flashing of current time when app is active
  - Notifications for reminder alerts
  - Uses AlarmManager for precise timing in all power states
- **Daily Time Pools**: Track daily time budgets for categories and view remaining pool time
- **Time Overview Charts**: Visualize time tracking data with interactive line charts
  - Week mode: Shows daily hours for Monday-Sunday
  - Month mode: Shows average daily hours per 4-day period
  - Year mode: Shows average daily hours per month
  - Full mode: Shows average daily hours across entire tracking history
  - Smart navigation: Automatically jumps to nearest period with data
  - Empty period skipping: Previous/Next buttons skip periods without entries
- **Smart Project Selection**: When changing categories, automatically selects the project with the most time
- **State Persistence**: Category, project, and reminder interval selections persist across app restarts
- **Category Management**: Add new categories and remove existing ones (with cascade deletion of entries)
- **Three-Section Interface**: Radio button selector for Control Panel, Time Entries, and Category Time Pools
- **Dark Mode Support**: Automatic theme switching based on system settings with customizable color scheme
- **File Import/Export**: Load and save data files compatible with Python/Web versions
- **Persistent Storage**: Uses SharedPreferences with JSON serialization via centralized PreferencesManager
- **Material Design**: Modern Android UI with Material 3 components
- **Category Filtering**: Projects dropdown automatically filters based on selected category
- **State Persistence**: Category, project, and reminder selections persist across app restarts and when switching apps
- **No Autocorrect**: Category and project input fields don't show spell-check underlines

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
│   │       │       ├── PreferencesManager.java
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

The app has four main sections accessible via horizontal scrolling selector:

### 1. Control Panel
- **Start/Pause/Resume Buttons**: Begin, pause, or resume time tracking
- **Reset Button**: Stop tracking without saving (appears when timer is active)
- **End Button**: Stop tracking and save entry (appears when timer is active)
- **Reminder Selector**: Choose or enter custom reminder interval in minutes
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

### 3. Time Pools
- **Load/Save Buttons**: Import/export MetaDataDailyTimePools.txt
- **Add/Remove Buttons**: Manage categories (removal cascades to delete all associated entries)
- **Pool Editor**: Table showing:
  - Category name
  - Daily minutes (editable)
  - Pool time remaining/exceeded (color-coded, shows "-" when no budget set, minus sign if negative)
  - Total time spent
- Changes save automatically

### 4. Time Overview
- **Mode Buttons**: Switch between Week, Month, Year, and Full views
- **Navigation Arrows**: Move to previous/next period with data
- **Line Chart**: Multi-category visualization with:
  - Week: Total hours per day (Mon-Sun)
  - Month: Average daily hours per 4-day period
  - Year: Average daily hours per month
  - Full: Average daily hours across 10 periods spanning all data
- **Smart Navigation**:
  - Auto-jumps to nearest period with data when opening or switching modes
  - Previous/Next buttons skip empty periods
  - Searches up to 52 periods (weeks/months/years) in each direction
- **Legend**: Color-coded categories below chart

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

### Dark Mode
The app automatically switches between light and dark themes based on your device's system settings using Material 3's DayNight theme. All UI elements adapt their colors accordingly.

### Smart Button Visibility
- **Start**: Visible when no timer is active
- **Pause/Resume**: Visible while timer is running
- **Reset & End**: Only visible when timer is running or paused
  - Reset: Discards current tracking without saving
  - End: Saves the entry and stops tracking

### Custom Reminder Intervals
You can either select from predefined intervals (0, 15, 30, 60, 120 minutes) or type any custom value in minutes. Reminders work in all states:
- **Foreground**: Beeps, vibration, flashing text, and notification
- **Background**: Beeps, vibration, and notification
- **Phone Locked**: Beeps, vibration, and notification
- Reminders are cancelled automatically when pausing, resetting, or ending the timer

### Background Operation
The app uses AlarmManager with exact alarm permissions to ensure reminders fire reliably:
- Works in Doze mode and battery optimization
- Permissions required: SCHEDULE_EXACT_ALARM, USE_EXACT_ALARM
- Visual flashing only occurs when app is visible
- All alarms are properly cancelled when timer is paused or stopped

### Category Management
- **Add Category**: Enter a new category name and daily time budget. Duplicate names are prevented.
- **Remove Category**: Select from existing categories to remove. All time entries associated with the category are also deleted.
- If the removed category is currently selected, the selection is automatically reset.

### Category-Filtered Projects
When you select a category, the project dropdown automatically shows only projects that have entries in that category. During app startup, the last selected project is restored. When changing categories during runtime, the project with the most recorded time in that category is automatically selected.

### Pool Time Calculation
Pool time = (Daily minutes × Days since first entry) - Total time spent
- Displayed as "-" if no daily minutes are set for the category
- Displayed in green if time remaining
- Displayed in red with minus sign if over budget

### Time Overview Charts
The overview section provides visual analysis of time tracking data:
- **Automatic Navigation**: Opens to the most recent period with data (checks up to 52 periods back)
- **Smart Period Skipping**: Navigation buttons skip empty periods to show only relevant data
- **Multiple Time Scales**:
  - Week view for daily tracking patterns
  - Month view for weekly trends
  - Year view for monthly patterns
  - Full view for historical overview
- **Chart Interaction**: Touch-enabled with pinch-to-zoom and pan gestures
- **Index Correction**: Properly handles calendar calculations accounting for daysBetween offset

### File Transfer
To transfer files between PC and Android emulator:
1. Drag and drop files onto emulator screen
2. Files appear in `/sdcard/Download/`
3. Use Load buttons to import data

### Storage
- **Internal**: SharedPreferences stores data between sessions, managed centrally via PreferencesManager
  - App state preferences (category, project, reminder selections)
  - Time entries data
  - Daily time pools data
- **Import/Export**: Text files for cross-platform compatibility

## Package Name

`com.romanbrunner.apps.projecttimetracker`

## License

Same license as the Project Time Tracker application.
