// ============================================================================
// Project Time Tracker - Web Version
// ============================================================================

// Configuration
const CONFIG = {
    DAILY_TIME_POOL_CHOICES: [5, 10, 15, 30, 45, 60, 90, 120],
    REMINDER_INTERVAL_CHOICES: [0, 15, 30, 60, 120],
    UPDATE_INTERVAL: 1000,
    REMINDER_ALERT_DURATION: 3000,
    REMINDER_BEEP_INTERVAL: 250,
    REMINDER_BEEP_FREQUENCY: 3000,
    DATETIME_SAVE_FORMAT: 'YYYY-MM-DD HH:mm:ss.SSSSSS',
    DATETIME_DISPLAY_FORMAT: 'HH:mm DD.MM.YYYY',
    FIELD_SEPARATOR: ' --- ',
    DEFAULT_PROJECT: 'ProjectTimeTracker',
    DEFAULT_CATEGORY: 'Programming',
    PROJECT_TIME_FILE: 'MetaDataProjectTime.txt',
    DAILY_POOLS_FILE: 'MetaDataDailyTimePools.txt',
    STORAGE_KEY_PROJECT_TIME: 'projectTimeTracker_projectTime',
    STORAGE_KEY_DAILY_POOLS: 'projectTimeTracker_dailyPools',
    HEADER_COLUMN_WIDTHS: [5, 5, 7, 14, 14, 11, 11, 11, 11, 11],
    ENTRIES_COLUMN_WIDTHS: [10, 7, 14, 14, 11, 11, 11, 11, 11],
    CATEGORY_POOLS_COLUMN_WIDTHS: [30, 20, 25, 25]
};

// ============================================================================
// Data Models
// ============================================================================

class MetaDataProjectTime {
    constructor() {
        this.entries = [];
        this.load(); // Auto-load from localStorage
    }

    load() {
        try {
            const stored = localStorage.getItem(CONFIG.STORAGE_KEY_PROJECT_TIME);
            if (stored) {
                this.parseContent(stored);
            }
        } catch (err) {
            console.error('Error loading from localStorage:', err);
        }
    }

    save() {
        try {
            localStorage.setItem(CONFIG.STORAGE_KEY_PROJECT_TIME, this.toContent());
        } catch (err) {
            console.error('Error saving to localStorage:', err);
        }
    }

    static formatDatetime(date) {
        const pad = (n, len = 2) => String(n).padStart(len, '0');
        return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ` +
               `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}.` +
               `${pad(date.getMilliseconds(), 3)}000`;
    }

    static parseDatetime(str) {
        try {
            const [datePart, timePart] = str.split(' ');
            const [year, month, day] = datePart.split('-').map(Number);
            const [hours, minutes, secondsMs] = timePart.split(':');
            const [seconds, ms] = secondsMs.split('.');
            return new Date(year, month - 1, day, Number(hours), Number(minutes), Number(seconds), Number(ms.slice(0, 3)));
        } catch {
            return null;
        }
    }

    static formatDisplayDatetime(date) {
        const pad = (n) => String(n).padStart(2, '0');
        return `${pad(date.getHours())}:${pad(date.getMinutes())} ` +
               `${pad(date.getDate())}.${pad(date.getMonth() + 1)}.${date.getFullYear()}`;
    }

    static durationToStr(totalSeconds) {
        totalSeconds = Math.floor(Math.abs(totalSeconds));
        const hours = Math.floor(totalSeconds / 3600);
        const minutes = Math.floor((totalSeconds % 3600) / 60);
        const seconds = totalSeconds % 60;
        if (hours > 0) {
            return `${hours}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
        }
        return `${minutes}:${String(seconds).padStart(2, '0')}`;
    }

    parseContent(content) {
        this.entries = [];
        const lines = content.trim().split('\n').filter(line => line.trim());
        for (const line of lines) {
            const fields = line.split(CONFIG.FIELD_SEPARATOR);
            if (fields.length >= 4) {
                this.entries.push({
                    project: fields[0].trim(),
                    category: fields[1].trim(),
                    duration: parseFloat(fields[2]),
                    startTime: fields[3].trim()
                });
            }
        }
    }

    toContent() {
        return this.entries.map(entry =>
            [entry.project, entry.category, String(entry.duration), entry.startTime].join(CONFIG.FIELD_SEPARATOR)
        ).join('\n');
    }

    addEntry(project, category, duration, startTime) {
        this.entries.push({ project, category, duration, startTime });
        this.save(); // Auto-save
    }

    removeEntry(index) {
        this.entries.splice(index, 1);
        this.save(); // Auto-save
    }

    getProjects() {
        return [...new Set(this.entries.map(e => e.project))].sort();
    }

    getCategories() {
        return [...new Set(this.entries.map(e => e.category))].sort();
    }

    getProjectsForCategory(category) {
        return [...new Set(this.entries.filter(e => e.category === category).map(e => e.project))].sort();
    }

    getTotalDuration(project, category) {
        let projectTotal = 0;
        let categoryTotal = 0;
        for (const entry of this.entries) {
            if (entry.project === project) {
                projectTotal += entry.duration;
            }
            if (entry.category === category) {
                categoryTotal += entry.duration;
            }
        }
        return { projectTotal, categoryTotal };
    }
}

class MetaDataDailyTimePools {
    constructor() {
        this.pools = {};
        this.load(); // Auto-load from localStorage
    }

    load() {
        try {
            const stored = localStorage.getItem(CONFIG.STORAGE_KEY_DAILY_POOLS);
            if (stored) {
                this.parseContent(stored);
            }
        } catch (err) {
            console.error('Error loading from localStorage:', err);
        }
    }

    save() {
        try {
            localStorage.setItem(CONFIG.STORAGE_KEY_DAILY_POOLS, this.toContent());
        } catch (err) {
            console.error('Error saving to localStorage:', err);
        }
    }

    parseContent(content) {
        this.pools = {};
        const lines = content.trim().split('\n').filter(line => line.trim());
        for (const line of lines) {
            const [category, minutes] = line.split(CONFIG.FIELD_SEPARATOR);
            if (category && minutes) {
                this.pools[category.trim()] = parseInt(minutes, 10);
            }
        }
    }

    toContent() {
        return Object.entries(this.pools)
            .sort(([a], [b]) => a.localeCompare(b))
            .map(([category, minutes]) => `${category}${CONFIG.FIELD_SEPARATOR}${minutes}`)
            .join('\n');
    }

    getDailyMinutes(category) {
        return this.pools[category] || 0;
    }

    setDailyMinutes(category, minutes) {
        this.pools[category] = Math.max(0, minutes);
        this.save(); // Auto-save
    }

    getCategories() {
        return Object.keys(this.pools).sort();
    }
}

// ============================================================================
// File Handler (load/save txt files via file picker / download)
// ============================================================================

class FileHandler {
    static loadFromFilePicker() {
        return new Promise((resolve) => {
            const input = document.createElement('input');
            input.type = 'file';
            input.accept = '.txt';
            input.multiple = true;
            input.style.display = 'none';
            document.body.appendChild(input);

            input.addEventListener('change', async (e) => {
                const files = e.target.files;
                const results = {};

                if (files && files.length > 0) {
                    for (const file of files) {
                        const content = await file.text();
                        results[file.name] = content;
                    }
                }

                document.body.removeChild(input);
                resolve(Object.keys(results).length > 0 ? results : null);
            });

            // Handle cancel (user closes dialog without selecting)
            input.addEventListener('cancel', () => {
                document.body.removeChild(input);
                resolve(null);
            });

            // Fallback for browsers that don't fire cancel event
            window.addEventListener('focus', function onFocus() {
                setTimeout(() => {
                    if (input.parentNode && (!input.files || input.files.length === 0)) {
                        document.body.removeChild(input);
                        resolve(null);
                    }
                    window.removeEventListener('focus', onFocus);
                }, 300);
            }, { once: true });

            input.click();
        });
    }

    static async saveToFile(content, filename) {
        // Try File System Access API (Chrome/Edge) - lets user pick location
        if ('showSaveFilePicker' in window) {
            try {
                const handle = await window.showSaveFilePicker({
                    suggestedName: filename,
                    types: [{
                        description: 'Text Files',
                        accept: { 'text/plain': ['.txt'] }
                    }]
                });
                const writable = await handle.createWritable();
                await writable.write(content);
                await writable.close();
                return true;
            } catch (err) {
                if (err.name === 'AbortError') {
                    return false; // User cancelled
                }
                // Fall back to download
                console.warn('Save picker failed, falling back to download:', err);
            }
        }

        // Fallback: regular download (Firefox and others)
        this.downloadFile(content, filename);
        return true;
    }

    static downloadFile(content, filename) {
        const blob = new Blob([content], { type: 'text/plain' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        a.style.display = 'none';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    }
}

// ============================================================================
// Controller
// ============================================================================

class Controller {
    constructor() {
        this.projectTimeData = new MetaDataProjectTime();
        this.dailyPoolsData = new MetaDataDailyTimePools();

        // Timer state
        this.firstStartDatetime = null;
        this.currentStartDatetime = null;
        this.accumulatedDuration = 0; // in seconds
        this.reminderInterval = 0; // in seconds
        this.nextReminder = 0; // in seconds
        this.alertUntilTime = 0;
        this.updateTimer = null;
        this.alertTimer = null;

        // Audio context for beeps
        this.audioContext = null;

        this.initElements();
        this.initEventListeners();

        // Show initial data status
        this.showDataStatus();
    }

    showDataStatus() {
        const entries = this.projectTimeData.entries.length;
        const pools = Object.keys(this.dailyPoolsData.pools).length;
        if (entries > 0 || pools > 0) {
            showToast(`Loaded from local storage: ${entries} entries, ${pools} pools`, 'success');
        }
        this.updateUI();
    }

    initElements() {
        // Buttons
        this.startStopBtn = document.getElementById('startStopBtn');
        this.endBtn = document.getElementById('endBtn');

        // Combobox inputs
        this.reminderInput = document.getElementById('reminderInput');
        this.reminderDropdown = document.getElementById('reminderDropdown');
        this.reminderCombobox = document.getElementById('reminderCombobox');

        this.projectInput = document.getElementById('projectInput');
        this.projectDropdown = document.getElementById('projectDropdown');
        this.projectCombobox = document.getElementById('projectCombobox');

        this.categoryInput = document.getElementById('categoryInput');
        this.categoryDropdown = document.getElementById('categoryDropdown');
        this.categoryCombobox = document.getElementById('categoryCombobox');

        // Displays
        this.currentEntryTimeEl = document.getElementById('currentEntryTime');
        this.totalProjectTimeEl = document.getElementById('totalProjectTime');
        this.totalCategoryTimeEl = document.getElementById('totalCategoryTime');
        this.poolTimeEl = document.getElementById('poolTime');
        this.startDateEl = document.getElementById('startDate');

        // Tables
        this.entriesBody = document.getElementById('entriesBody');
        this.poolsBody = document.getElementById('poolsBody');

        // Import/Export controls
        this.importBtn = document.getElementById('importBtn');
        this.exportBtn = document.getElementById('exportBtn');
    }

    initEventListeners() {
        this.startStopBtn.addEventListener('click', () => this.startStopEntry());
        this.endBtn.addEventListener('click', () => this.endEntry());

        // Setup comboboxes
        this.setupCombobox(this.reminderCombobox, this.reminderInput, this.reminderDropdown,
            () => CONFIG.REMINDER_INTERVAL_CHOICES.map(String),
            () => this.updateReminderChoice());

        this.setupCombobox(this.projectCombobox, this.projectInput, this.projectDropdown,
            () => {
                return this.categoryInput.value ? this.projectTimeData.getProjectsForCategory(this.categoryInput.value) : this.projectTimeData.getProjects();
            },
            () => this.updateTotalDurations());

        this.setupCombobox(this.categoryCombobox, this.categoryInput, this.categoryDropdown,
            () => this.projectTimeData.getCategories(),
            () => {
                this.updateProjectsForCategory();
                this.updateTotalDurations();
                this.updatePoolTime();
            });

        // Load/Save controls
        this.importBtn.addEventListener('click', () => this.loadData());
        this.exportBtn.addEventListener('click', () => this.saveData());

        // Close dropdowns when clicking outside
        document.addEventListener('click', (e) => {
            if (!e.target.closest('.combobox')) {
                this.closeAllDropdowns();
            }
        });
    }

    setupCombobox(container, input, dropdown, getOptions, onChange) {
        const btn = container.querySelector('.combobox-btn');

        // Toggle dropdown on button click
        btn.addEventListener('click', (e) => {
            e.stopPropagation();
            const isOpen = dropdown.classList.contains('open');
            this.closeAllDropdowns();
            if (!isOpen) {
                this.populateDropdown(dropdown, input, getOptions(), onChange);
                this.positionDropdown(dropdown, input);
                dropdown.classList.add('open');
            }
        });

        // Show dropdown on input focus
        input.addEventListener('focus', () => {
            this.closeAllDropdowns();
            this.populateDropdown(dropdown, input, getOptions(), onChange);
            this.positionDropdown(dropdown, input);
            dropdown.classList.add('open');
        });

        // Filter dropdown on input
        input.addEventListener('input', () => {
            const filter = input.value.toLowerCase();
            const options = getOptions().filter(opt =>
                opt.toLowerCase().includes(filter)
            );
            this.populateDropdown(dropdown, input, options, onChange);
            this.positionDropdown(dropdown, input);
            dropdown.classList.add('open');
        });

        // Handle input change
        input.addEventListener('change', onChange);

        // Keyboard navigation
        input.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') {
                dropdown.classList.remove('open');
            } else if (e.key === 'ArrowDown' && dropdown.classList.contains('open')) {
                const firstOption = dropdown.querySelector('.combobox-option');
                if (firstOption) firstOption.focus();
                e.preventDefault();
            }
        });
    }

    populateDropdown(dropdown, input, options, onChange) {
        dropdown.innerHTML = '';
        const currentValue = input.value;

        for (const option of options) {
            const div = document.createElement('div');
            div.className = 'combobox-option' + (option === currentValue ? ' selected' : '');
            div.textContent = option;
            div.tabIndex = 0;

            div.addEventListener('click', () => {
                input.value = option;
                dropdown.classList.remove('open');
                onChange();
            });

            div.addEventListener('keydown', (e) => {
                if (e.key === 'Enter') {
                    input.value = option;
                    dropdown.classList.remove('open');
                    onChange();
                } else if (e.key === 'ArrowDown') {
                    const next = div.nextElementSibling;
                    if (next) next.focus();
                    e.preventDefault();
                } else if (e.key === 'ArrowUp') {
                    const prev = div.previousElementSibling;
                    if (prev) prev.focus();
                    else input.focus();
                    e.preventDefault();
                } else if (e.key === 'Escape') {
                    dropdown.classList.remove('open');
                    input.focus();
                }
            });

            dropdown.appendChild(div);
        }
    }

    positionDropdown(dropdown, input) {
        const rect = input.getBoundingClientRect();
        const combobox = input.closest('.combobox');
        const comboboxRect = combobox.getBoundingClientRect();
        dropdown.style.top = `${rect.bottom}px`;
        dropdown.style.left = `${comboboxRect.left}px`;
        dropdown.style.width = `${comboboxRect.width}px`;
    }

    closeAllDropdowns() {
        document.querySelectorAll('.combobox-dropdown.open').forEach(d => d.classList.remove('open'));
    }

    // Timer Methods
    startStopEntry() {
        if (this.currentStartDatetime === null) {
            // Start or resume
            this.currentStartDatetime = new Date();
            if (this.firstStartDatetime === null) {
                this.firstStartDatetime = this.currentStartDatetime;
                this.startDateEl.textContent = MetaDataProjectTime.formatDisplayDatetime(this.firstStartDatetime);
            }
            this.startStopBtn.textContent = 'Pause';
            if (this.reminderInterval > 0) {
                this.nextReminder = (Math.floor(this.accumulatedDuration / this.reminderInterval) + 1) * this.reminderInterval;
            }
            this.startUpdateLoop();
        } else {
            // Pause
            this.accumulatedDuration += (new Date() - this.currentStartDatetime) / 1000;
            this.currentStartDatetime = null;
            this.startStopBtn.textContent = 'Resume';
            this.currentEntryTimeEl.textContent = MetaDataProjectTime.durationToStr(this.accumulatedDuration);
            this.currentEntryTimeEl.classList.remove('flashing');
            this.stopUpdateLoop();
        }
    }

    endEntry() {
        if (this.firstStartDatetime === null) {
            return;
        }

        // Calculate final duration
        if (this.currentStartDatetime !== null) {
            this.accumulatedDuration += (new Date() - this.currentStartDatetime) / 1000;
        }

        // Add entry (auto-saves to localStorage)
        this.projectTimeData.addEntry(
            this.projectInput.value || CONFIG.DEFAULT_PROJECT,
            this.categoryInput.value || CONFIG.DEFAULT_CATEGORY,
            this.accumulatedDuration,
            MetaDataProjectTime.formatDatetime(this.firstStartDatetime)
        );

        // Reset state
        this.firstStartDatetime = null;
        this.currentStartDatetime = null;
        this.accumulatedDuration = 0;
        this.nextReminder = 0;
        this.startStopBtn.textContent = 'Start';
        this.currentEntryTimeEl.textContent = '0:00';
        this.currentEntryTimeEl.classList.remove('flashing');
        this.startDateEl.textContent = '-';
        this.stopUpdateLoop();

        // Update UI
        this.updateUI();
    }

    startUpdateLoop() {
        if (this.updateTimer) return;
        this.updateTimer = setInterval(() => this.updateCurrentDuration(), CONFIG.UPDATE_INTERVAL);
    }

    stopUpdateLoop() {
        if (this.updateTimer) {
            clearInterval(this.updateTimer);
            this.updateTimer = null;
        }
    }

    updateCurrentDuration() {
        if (this.currentStartDatetime === null) return;

        const totalSeconds = this.accumulatedDuration + (new Date() - this.currentStartDatetime) / 1000;
        this.currentEntryTimeEl.textContent = MetaDataProjectTime.durationToStr(totalSeconds);
        this.updateTotalDurations();
        this.updatePoolTime();

        // Check for reminder
        if (this.nextReminder > 0 && this.reminderInterval > 0 && totalSeconds >= this.nextReminder) {
            if (Date.now() >= this.alertUntilTime) {
                this.alertUntilTime = Date.now() + CONFIG.REMINDER_ALERT_DURATION;
                this.startAlert();
            }
            this.nextReminder += this.reminderInterval;
        }
    }

    updateReminderChoice() {
        const minutes = parseInt(this.reminderInput.value, 10) || 0;
        this.reminderInterval = minutes * 60;

        if (this.reminderInterval === 0) {
            this.nextReminder = 0;
            this.alertUntilTime = 0;
            this.currentEntryTimeEl.classList.remove('flashing');
        } else if (this.currentStartDatetime !== null) {
            const totalSeconds = this.accumulatedDuration + (new Date() - this.currentStartDatetime) / 1000;
            this.nextReminder = (Math.floor(totalSeconds / this.reminderInterval) + 1) * this.reminderInterval;
        }
    }

    // Alert Methods
    startAlert() {
        this.playBeep();
        this.alertTick();
    }

    alertTick() {
        if (Date.now() >= this.alertUntilTime) {
            this.currentEntryTimeEl.classList.remove('flashing');
            return;
        }

        this.currentEntryTimeEl.classList.toggle('flashing');
        this.playBeep();

        setTimeout(() => this.alertTick(), CONFIG.REMINDER_BEEP_INTERVAL);
    }

    playBeep() {
        try {
            if (!this.audioContext) {
                this.audioContext = new (window.AudioContext || window.webkitAudioContext)();
            }
            const oscillator = this.audioContext.createOscillator();
            const gainNode = this.audioContext.createGain();

            oscillator.connect(gainNode);
            gainNode.connect(this.audioContext.destination);

            oscillator.frequency.value = CONFIG.REMINDER_BEEP_FREQUENCY;
            oscillator.type = 'sine';
            gainNode.gain.value = 0.1;

            oscillator.start();
            oscillator.stop(this.audioContext.currentTime + 0.1);
        } catch (err) {
            // Audio not available
        }
    }

    // UI Update Methods
    updateUI() {
        this.updateProjectsAndCategories();
        this.updateTotalDurations();
        this.updatePoolTime();
        this.renderEntries();
        this.renderPools();
    }

    updateProjectsAndCategories() {
        // Combobox dropdowns are populated dynamically when opened
        // No need to update datalists anymore
    }

    updateProjectsForCategory() {
        const category = this.categoryInput.value;
        const projects = this.projectTimeData.getProjectsForCategory(category);
        // Update project input if current value doesn't match category
        if (projects.length > 0 && !projects.includes(this.projectInput.value)) {
            this.projectInput.value = projects[0];
        }
    }

    updateTotalDurations() {
        const project = this.projectInput.value || CONFIG.DEFAULT_PROJECT;
        const category = this.categoryInput.value || CONFIG.DEFAULT_CATEGORY;

        let currentSeconds = this.accumulatedDuration;
        if (this.currentStartDatetime !== null) {
            currentSeconds += (new Date() - this.currentStartDatetime) / 1000;
        }

        const { projectTotal, categoryTotal } = this.projectTimeData.getTotalDuration(project, category);

        this.totalProjectTimeEl.textContent = MetaDataProjectTime.durationToStr(projectTotal + currentSeconds);
        this.totalCategoryTimeEl.textContent = MetaDataProjectTime.durationToStr(categoryTotal + currentSeconds);
    }

    updatePoolTime() {
        const category = this.categoryInput.value || CONFIG.DEFAULT_CATEGORY;
        const remaining = this.calculateRemainingPoolSeconds(category);

        if (remaining !== null) {
            const isNegative = remaining < 0;
            this.poolTimeEl.textContent = (isNegative ? '-' : '') + MetaDataProjectTime.durationToStr(Math.abs(remaining));
            this.poolTimeEl.classList.remove('positive', 'negative');
            this.poolTimeEl.classList.add(isNegative ? 'negative' : 'positive');
        } else {
            this.poolTimeEl.textContent = '-';
            this.poolTimeEl.classList.remove('positive', 'negative');
        }
    }

    calculateRemainingPoolSeconds(category, usedSeconds = 0) {
        const dailyMinutes = this.dailyPoolsData.getDailyMinutes(category);
        if (dailyMinutes <= 0) return null;

        // Add current entry time if tracking this category
        if (this.currentStartDatetime !== null && (this.categoryInput.value || CONFIG.DEFAULT_CATEGORY) === category) {
            usedSeconds += this.accumulatedDuration + (new Date() - this.currentStartDatetime) / 1000;
        }

        // Find earliest start date
        let firstDate = new Date();
        for (const entry of this.projectTimeData.entries) {
            if (entry.category === category) {
                const entryDate = MetaDataProjectTime.parseDatetime(entry.startTime);
                if (entryDate && entryDate < firstDate) {
                    firstDate = entryDate;
                }
                usedSeconds += entry.duration;
            }
        }

        // Calculate days and pool time
        const today = new Date();
        const daysDiff = Math.floor((today.setHours(0, 0, 0, 0) - new Date(firstDate).setHours(0, 0, 0, 0)) / (1000 * 60 * 60 * 24)) + 1;
        const poolSeconds = dailyMinutes * 60 * daysDiff;

        return poolSeconds - usedSeconds;
    }

    renderEntries() {
        this.entriesBody.innerHTML = '';

        // Sort entries by start time (newest first)
        const sortedEntries = [...this.projectTimeData.entries]
            .map((entry, index) => ({ ...entry, originalIndex: index }))
            .sort((a, b) => {
                const dateA = MetaDataProjectTime.parseDatetime(a.startTime);
                const dateB = MetaDataProjectTime.parseDatetime(b.startTime);
                return (dateB || 0) - (dateA || 0);
            });

        for (const entry of sortedEntries) {
            const tr = document.createElement('tr');
            const { projectTotal, categoryTotal } = this.projectTimeData.getTotalDuration(entry.project, entry.category);
            const poolTime = this.calculateRemainingPoolSecondsForEntry(entry.category);
            const poolTimeStr = poolTime !== null
                ? (poolTime < 0 ? '-' : '') + MetaDataProjectTime.durationToStr(Math.abs(poolTime))
                : '-';
            const startDate = MetaDataProjectTime.parseDatetime(entry.startTime);
            const displayDate = startDate ? MetaDataProjectTime.formatDisplayDatetime(startDate) : '-';

            tr.innerHTML = `
                <td><button class="btn-danger remove-btn" data-index="${entry.originalIndex}">Remove</button></td>
                <td></td>
                <td>${this.escapeHtml(entry.project)}</td>
                <td>${this.escapeHtml(entry.category)}</td>
                <td>${MetaDataProjectTime.durationToStr(entry.duration)}</td>
                <td>${MetaDataProjectTime.durationToStr(projectTotal)}</td>
                <td>${MetaDataProjectTime.durationToStr(categoryTotal)}</td>
                <td>${poolTimeStr}</td>
                <td>${displayDate}</td>
            `;

            this.entriesBody.appendChild(tr);
        }

        // Add event listeners for remove buttons
        this.entriesBody.querySelectorAll('.remove-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const index = parseInt(e.target.dataset.index, 10);
                this.removeEntry(index);
            });
        });
    }

    calculateRemainingPoolSecondsForEntry(category) {
        const dailyMinutes = this.dailyPoolsData.getDailyMinutes(category);
        if (dailyMinutes <= 0) return null;

        let usedSeconds = 0;
        let firstDate = new Date();

        for (const entry of this.projectTimeData.entries) {
            if (entry.category === category) {
                const entryDate = MetaDataProjectTime.parseDatetime(entry.startTime);
                if (entryDate && entryDate < firstDate) {
                    firstDate = entryDate;
                }
                usedSeconds += entry.duration;
            }
        }

        const today = new Date();
        const daysDiff = Math.floor((today.setHours(0, 0, 0, 0) - new Date(firstDate).setHours(0, 0, 0, 0)) / (1000 * 60 * 60 * 24)) + 1;
        const poolSeconds = dailyMinutes * 60 * daysDiff;

        return poolSeconds - usedSeconds;
    }

    removeEntry(index) {
        this.projectTimeData.removeEntry(index);
        // Auto-saved by removeEntry
        this.updateUI();
    }

    renderPools() {
        this.poolsBody.innerHTML = '';

        // Get all categories from both pools and entries
        const categories = new Set([
            ...this.dailyPoolsData.getCategories(),
            ...this.projectTimeData.getCategories()
        ]);

        for (const category of [...categories].sort()) {
            const tr = document.createElement('tr');
            const dailyMinutes = this.dailyPoolsData.getDailyMinutes(category);
            const poolTime = this.calculateRemainingPoolSecondsForEntry(category);
            const poolTimeStr = poolTime !== null
                ? (poolTime < 0 ? '-' : '') + MetaDataProjectTime.durationToStr(Math.abs(poolTime))
                : '-';

            // Calculate total time
            let totalSeconds = 0;
            for (const entry of this.projectTimeData.entries) {
                if (entry.category === category) {
                    totalSeconds += entry.duration;
                }
            }

            tr.innerHTML = `
                <td>${this.escapeHtml(category)}</td>
                <td>
                    <div class="combobox daily-time-combobox" data-category="${this.escapeHtml(category)}">
                        <input type="text" class="daily-time-input" value="${dailyMinutes}">
                        <button class="combobox-btn" tabindex="-1">▼</button>
                        <div class="combobox-dropdown daily-time-dropdown"></div>
                    </div>
                </td>
                <td class="${poolTime !== null ? (poolTime < 0 ? 'negative' : 'positive') : ''}">${poolTimeStr}</td>
                <td>${MetaDataProjectTime.durationToStr(totalSeconds)}</td>
            `;

            this.poolsBody.appendChild(tr);
        }

        // Setup comboboxes for daily time
        this.poolsBody.querySelectorAll('.daily-time-combobox').forEach(combobox => {
            const category = combobox.dataset.category;
            const input = combobox.querySelector('.daily-time-input');
            const dropdown = combobox.querySelector('.daily-time-dropdown');
            const btn = combobox.querySelector('.combobox-btn');

            const getOptions = () => [0, ...CONFIG.DAILY_TIME_POOL_CHOICES].map(String);

            const onChange = () => {
                const minutes = parseInt(input.value, 10) || 0;
                this.dailyPoolsData.setDailyMinutes(category, minutes);
                this.updatePoolTime();
                this.renderPools();
                this.renderEntries();
            };

            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const isOpen = dropdown.classList.contains('open');
                this.closeAllDropdowns();
                if (!isOpen) {
                    this.populateDropdown(dropdown, input, getOptions(), onChange);
                    this.positionDropdown(dropdown, input);
                    dropdown.classList.add('open');
                }
            });

            input.addEventListener('focus', () => {
                this.closeAllDropdowns();
                this.populateDropdown(dropdown, input, getOptions(), onChange);
                this.positionDropdown(dropdown, input);
                dropdown.classList.add('open');
            });

            input.addEventListener('change', onChange);

            input.addEventListener('keydown', (e) => {
                if (e.key === 'Enter') {
                    onChange();
                    dropdown.classList.remove('open');
                } else if (e.key === 'Escape') {
                    dropdown.classList.remove('open');
                }
            });
        });
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    // Load/Save Methods
    async loadData() {
        // Open file picker for user to select txt files
        const files = await FileHandler.loadFromFilePicker();

        if (!files) {
            return; // User cancelled
        }

        let loaded = 0;

        // Check for project time file
        if (files[CONFIG.PROJECT_TIME_FILE]) {
            const content = files[CONFIG.PROJECT_TIME_FILE];
            if (content.trim()) {
                this.projectTimeData.parseContent(content);
                this.projectTimeData.save();
                loaded++;
            }
        }

        // Check for daily pools file
        if (files[CONFIG.DAILY_POOLS_FILE]) {
            const content = files[CONFIG.DAILY_POOLS_FILE];
            if (content.trim()) {
                this.dailyPoolsData.parseContent(content);
                this.dailyPoolsData.save();
                loaded++;
            }
        }

        // Update UI
        this.updateUI();

        // Show status
        if (loaded > 0) {
            const entries = this.projectTimeData.entries.length;
            const pools = Object.keys(this.dailyPoolsData.pools).length;
            showToast(`Loaded ${entries} entries, ${pools} category pools`, 'success');
        } else {
            showToast('No valid data files found. Select MetaDataProjectTime.txt and/or MetaDataDailyTimePools.txt', 'error');
        }
    }

    saveData() {
        // Download both files
        FileHandler.downloadFile(this.projectTimeData.toContent(), CONFIG.PROJECT_TIME_FILE);
        setTimeout(() => {
            FileHandler.downloadFile(this.dailyPoolsData.toContent(), CONFIG.DAILY_POOLS_FILE);
        }, 100);
        showToast('Files downloaded. Save them to your Dropbox folder.', 'success');
    }
}

// ============================================================================
// Utility Functions
// ============================================================================

function showToast(message, type = 'info') {
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.textContent = message;
    document.body.appendChild(toast);

    setTimeout(() => {
        toast.remove();
    }, 3000);
}

// ============================================================================
// Column Width Setup
// ============================================================================

function applyColumnWidths() {
    // Apply header column widths
    const headerColgroup = document.getElementById('headerColgroup');
    headerColgroup.innerHTML = CONFIG.HEADER_COLUMN_WIDTHS.map(w => `<col style="width: ${w}%">`).join('');

    // Apply entries column widths
    const entriesColgroup = document.getElementById('entriesColgroup');
    entriesColgroup.innerHTML = CONFIG.ENTRIES_COLUMN_WIDTHS.map(w => `<col style="width: ${w}%">`).join('');

    // Apply pools column widths
    const poolsColgroup = document.getElementById('poolsColgroup');
    poolsColgroup.innerHTML = CONFIG.CATEGORY_POOLS_COLUMN_WIDTHS.map(w => `<col style="width: ${w}%">`).join('');
}

// ============================================================================
// Initialize App
// ============================================================================

let controller;

document.addEventListener('DOMContentLoaded', () => {
    applyColumnWidths();
    controller = new Controller();
});
