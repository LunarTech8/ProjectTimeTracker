# Project Time Tracker - Python Version

The original desktop application for tracking time spent on projects and categories, built with Python and tkinter.

## Features

- Track time spent on projects and categories
- Start, pause, resume, and end time entries
- Configurable reminder intervals (0, 15, 30, 60, 120 minutes) with sound alerts
- Daily time pools for categories
- Persistent storage using text files
- View and manage history of all time entries
- Desktop notifications and audio alerts

## Requirements

- Python 3.8 or later
- tkinter (usually included with Python)
- Standard library modules (no additional installations required)

## Installation

No installation required - Python is a standalone script.

## Usage

Run the application:

```bash
python OpenProjectTimeTracker.pyw
```

Or double-click `OpenProjectTimeTracker.pyw` on Windows.

## Data Files

The application uses two text files for data persistence:

### MetaDataProjectTime.txt
Stores all time entries in the format:
```
PROJECT --- CATEGORY --- DURATION_SECONDS --- START_DATETIME
```

Example:
```
ProjectTimeTracker --- Programming --- 3606.020729 --- 2025-12-01 15:16:25.607679
```

### MetaDataDailyTimePools.txt
Stores daily time budgets for categories:
```
CATEGORY --- DAILY_MINUTES
```

Example:
```
Programming --- 60
Art --- 120
```

The `.txt.lnk` files in this directory are Windows shortcuts pointing to the actual data files (typically stored in a shared location like Dropbox).

## Project Structure

```
PythonVersion/
├── OpenProjectTimeTracker.pyw    # Main application
├── MetaDataProjectTime.py        # Time entry data management
├── MetaDataDailyTimePools.py     # Time pool data management
├── GridField.py                  # UI grid component
├── MetaDataProjectTime.txt.lnk   # Shortcut to time entries data file
└── MetaDataDailyTimePools.txt.lnk # Shortcut to time pools data file
```

## Key Features Explained

### Time Tracking
1. Select or enter a Project name
2. Select or enter a Category name
3. Click **Start** to begin tracking
4. Click **Pause** to pause (timer continues to show accumulated time)
5. Click **Resume** to continue
6. Click **End** to save the entry to file

### Reminder System
- Set reminder interval in minutes (0 to disable)
- When interval is reached:
  - Display flashes
  - Sound alert plays
  - Continues until timer is paused/ended

### Category Time Pools
- Set daily time budgets for categories
- View time remaining based on:
  - Daily budget × days since first entry in category
  - Minus total time spent in category
- Color coding: Green (remaining), Red (over budget)

### Time Entry Management
- View all recorded entries
- Delete entries if needed
- Entries persist across sessions

## File Compatibility

The data files are compatible with:
- **Android Version**: Can import/export using Load/Save buttons
- **Web Version**: Can load/save the same file format

All versions share the same text-based file format for easy data portability.

## Interface

The application uses a grid-based layout with:
- **Control Panel**: Start/Stop buttons, Project/Category selectors, Reminder settings
- **Time Display**: Current entry time, Total times, Pool time, Start date
- **Entries List**: History of all time entries with remove buttons
- **Pools Editor**: Manage daily time budgets for categories

## Customization

To modify default values, edit the constants in `OpenProjectTimeTracker.pyw`:
- `REMINDER_INTERVAL_CHOICES`: Available reminder intervals
- `UPDATE_INTERVAL_MS`: Timer update frequency
- `REMINDER_ALERT_DURATION_MS`: Length of reminder alert
- `REMINDER_BEEP_FREQUENCY_HZ`: Alert sound frequency

## License

Same license as the Project Time Tracker application.
