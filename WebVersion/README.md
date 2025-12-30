# Project Time Tracker - Web Version

A browser-based version of the Project Time Tracker application that works in Firefox, Chrome, and other modern browsers.

## Features

- **Time Tracking**: Start, pause, resume, and end time entries for projects and categories
- **Reminders**: Set interval reminders (15, 30, 60, or 120 minutes) with audio and visual alerts
- **Category Pools**: Track daily time budgets for categories and see remaining pool time
- **Local File Storage**: Uses local `.txt` files for data storage (same format as the desktop version)
- **Cross-Browser Support**: Works in Firefox, Chrome, Edge, and other modern browsers

## How to Use

### Getting Started

1. Open `index.html` in your web browser
2. Either:
   - Click "Create New Data Files" to create new empty data files
   - Click "Load" buttons to load existing data files from the desktop version

### File Compatibility

The web version uses the same file format as the desktop Python version:

- `MetaDataProjectTime.txt` - Stores time entries
- `MetaDataDailyTimePools.txt` - Stores daily time pool settings for categories

You can share data files between the web and desktop versions.

### Browser Differences

**Chrome/Edge (Chromium-based)**:
- Full File System Access API support
- Can save directly to your selected files without re-downloading
- Best experience for frequent use

**Firefox**:
- Uses file download/upload for data persistence
- When saving, files are downloaded to your Downloads folder
- To update your data, load the downloaded files after making changes

### Tracking Time

1. Enter or select a Project name
2. Enter or select a Category name
3. Click **Start** to begin tracking
4. Click **Pause** to pause (displays accumulated time)
5. Click **Resume** to continue tracking
6. Click **End** to save the entry

### Setting Reminders

1. Select a reminder interval from the dropdown (0, 15, 30, 60, or 120 minutes)
2. When the timer reaches the interval, you'll see a flashing display and hear a beep
3. Set to 0 to disable reminders

### Managing Category Pools

In the "Category Time Pools" section:

1. Categories appear automatically from your time entries
2. Set a daily time budget (in minutes) for each category
3. The "Pool time" shows remaining time based on:
   - Daily budget × days since first entry
   - Minus total time spent

Green = time remaining, Red = over budget

### Managing Entries

- The entries list shows all recorded time entries (newest first)
- Click **Remove** to delete an entry
- Changes are saved automatically (or downloaded in Firefox)

## Technical Details

### Data Format

**MetaDataProjectTime.txt**:
```
ProjectName --- CategoryName --- DurationInSeconds --- StartDateTime
```

Example:
```
ProjectTimeTracker --- Programming --- 3600.0 --- 2024-12-30 10:00:00.000000
```

**MetaDataDailyTimePools.txt**:
```
CategoryName --- DailyMinutes
```

Example:
```
Programming --- 60
```

### Local Development

To run locally, simply open `index.html` in a web browser. No server required.

For Chrome, if you encounter CORS issues with local files, you can:
1. Use a local server: `python -m http.server 8000`
2. Open `http://localhost:8000`

### Browser Requirements

- Modern browser with ES6+ support
- JavaScript enabled
- Web Audio API (for reminder beeps)
- File API / File System Access API

## Keyboard Shortcuts

Currently, the web version relies on mouse/touch input. Future versions may add keyboard shortcuts.

## Known Limitations

1. **Firefox**: Cannot save directly to files - uses download approach
2. **Audio**: First beep may require user interaction due to browser autoplay policies
3. **Background tabs**: Timer updates may slow down when tab is not active

## License

Same license as the main Project Time Tracker application.
