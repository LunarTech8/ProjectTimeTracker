import traceback
import threading
import tkinter as tk
from tkinter import ttk
from datetime import datetime, timedelta
from typing import Optional
from MetaDataProjectTime import MetaDataProjectTime
from GridField import GridField
try:
	import winsound
except Exception:
	winsound = None

# Configurables:
DAILY_TIME_POOLS = {'Programming': 30, 'Television': 60, 'Gaming': 60, 'Art': 10}  # minutes
DAILY_TIME_POOL_CHOICES = (5, 10, 15, 30, 45, 60, 90, 120)  # minutes
WINDOW_SIZE = (1000, 300)
HEADER_COLUMN_NAMES = ('Control:', 'Reminder:', 'Project:', 'Category:', 'Current entry time:', 'Total project time:', 'Total category time:', 'Category pool time:', 'Start date:', '')
HEADER_COLUMN_WIDTHS = (10, 10, 12, 20, 20, 20, 20, 20, 20, 20, 5)
ENTRIES_COLUMN_WIDTHS = (26, 8, 16, 16, 16, 16, 16, 16, 16)
CATEGORY_POOLS_WINDOW_SIZE = (500, 200)
CATEGORY_POOLS_COLUMN_NAMES = ('Name:', 'Daily time:', 'Pool time:', 'Total time:', '')
CATEGORY_POOLS_COLUMN_WIDTHS = (22, 16, 16, 16, 3)  # TODO: adjust widths
UPDATE_INTERVAL = 1000  # milliseconds
REMINDER_ALERT_DURATION = 3000  # milliseconds
REMINDER_BEEP_INTERVAL = 250  # milliseconds
REMINDER_BEEP_DURATION = 200  # milliseconds
REMINDER_BEEP_FREQUENCY = 3000  # Hz
REMINDER_INTERVAL_CHOICES = (0, 15, 30, 60, 120)  # minutes
REMINDER_FLASH_COLOR = 'blue'
POOL_TIME_NEGATIVE_COLOR = 'red'
POOL_TIME_POSITIVE_COLOR = 'green'
DEFAULT_COLOR = 'black'
DATETIME_DISPLAY_FORMAT = '%H:%M %d.%m.%Y'
class BUTTON_NAMES():
	START = 'Start'
	PAUSE = 'Pause'
	RESUME = 'Resume'
	END = 'End'
	REMOVE = 'Remove'
assert len(HEADER_COLUMN_NAMES) == len(HEADER_COLUMN_WIDTHS)-1
# Global variables:
metaData: MetaDataProjectTime = MetaDataProjectTime()
controller: Optional['Controller'] = None
headerFrame: Optional[tk.Canvas] = None
entriesFrame: Optional[tk.Frame] = None
categoryPoolsWindow: Optional['CategoryPoolsWindow'] = None
# TODO: store categories and daily time pools in extra file instead of DAILY_TIME_POOLS
# TODO: allow new categories to be added to categoryPoolsWindow via the UI
# TODO: use StringVars for the EntriesList instead of recreating all grid fields each time
# TODO: finder better system to make header and entries columns widths the same
# TODO: improve shown columns in control/entries list

class Controller:
	def __init__(self, root: tk.Canvas) -> None:
		self.root: tk.Canvas = root
		self.startStopStrVar = tk.StringVar(root, BUTTON_NAMES.START)
		self.reminderChoiceStrVar = tk.StringVar(root, str(REMINDER_INTERVAL_CHOICES[0]))
		self.projectStrVar = tk.StringVar(root, MetaDataProjectTime.getDefaultFieldValue(MetaDataProjectTime.Field.PROJECT))
		self.categoryStrVar = tk.StringVar(root, MetaDataProjectTime.getDefaultFieldValue(MetaDataProjectTime.Field.CATEGORY))
		self.currentEntryDurationStrVar = tk.StringVar(root, MetaDataProjectTime.getDefaultFieldValue(MetaDataProjectTime.Field.DURATION))
		self.totalProjectDurationStrVar = tk.StringVar(root, MetaDataProjectTime.getDefaultFieldValue(MetaDataProjectTime.Field.DURATION))
		self.totalCategoryDurationStrVar = tk.StringVar(root, MetaDataProjectTime.getDefaultFieldValue(MetaDataProjectTime.Field.DURATION))
		self.startDatetimeStrVar = tk.StringVar(root, MetaDataProjectTime.getDefaultFieldValue(MetaDataProjectTime.Field.START_TIME))
		self.poolTimeStrVar = tk.StringVar(root, MetaDataProjectTime.getDefaultFieldValue(MetaDataProjectTime.Field.START_TIME))
		self.sortedProjects = []
		self.sortedCategories = []
		self.firstStartDatetime = None
		self.currentStartDatetime = None
		self.accumulatedDuration = timedelta()
		self.reminderInterval = 0  # seconds
		self.nextReminder = 0  # seconds
		self.alertUntilDatetime = datetime.now()
		self.projectCombobox: ttk.Combobox
		self.currentEntryDurationLabel: tk.Label
		self.poolTimeLabel: tk.Label

	def getStartStopVar(self) -> tk.StringVar:
		return self.startStopStrVar

	def getProjectVar(self) -> tk.StringVar:
		return self.projectStrVar

	def getCategoryVar(self) -> tk.StringVar:
		return self.categoryStrVar

	def getCurrentEntryDurationStrVar(self) -> tk.StringVar:
		return self.currentEntryDurationStrVar

	def getTotalProjectDurationStrVar(self) -> tk.StringVar:
		return self.totalProjectDurationStrVar

	def getTotalCategoryDurationStrVar(self) -> tk.StringVar:
		return self.totalCategoryDurationStrVar

	def getStartDatetimeStrVar(self) -> tk.StringVar:
		return self.startDatetimeStrVar

	def getPoolTimeStrVar(self) -> tk.StringVar:
		return self.poolTimeStrVar

	def getSortedProjects(self) -> list[str]:
		return self.sortedProjects

	def getSortedCategories(self) -> list[str]:
		return self.sortedCategories

	def startStopEntry(self) -> None:
		if self.currentStartDatetime is None:
			# Start or resume timer:
			self.currentStartDatetime = datetime.now()
			if self.firstStartDatetime is None:
				self.firstStartDatetime = self.currentStartDatetime
				self.startDatetimeStrVar.set(self.firstStartDatetime.strftime(DATETIME_DISPLAY_FORMAT))
			self.startStopStrVar.set(BUTTON_NAMES.PAUSE)
			if self.reminderInterval > 0:
				self.nextReminder = ((int(self.accumulatedDuration.total_seconds()) // self.reminderInterval) + 1) * self.reminderInterval
			self.updateCurrentDuration()
		else:
			# Pause timer:
			self.accumulatedDuration += datetime.now() - self.currentStartDatetime
			self.currentStartDatetime = None
			self.startStopStrVar.set(BUTTON_NAMES.RESUME)
			self.currentEntryDurationStrVar.set(MetaDataProjectTime.durationToStr(self.accumulatedDuration))
			self.alertUntilDatetime = datetime.now()
			self.currentEntryDurationLabel.configure(fg=DEFAULT_COLOR)

	def endEntry(self) -> None:
		if self.firstStartDatetime is None:
			return
		# Store current entry:
		if self.currentStartDatetime is not None:
			self.accumulatedDuration += datetime.now() - self.currentStartDatetime
		metaData.addEntry([self.projectStrVar.get(), self.categoryStrVar.get(), str(self.accumulatedDuration.total_seconds()), str(self.firstStartDatetime)])
		metaData.writeMetaData()
		# Reset current entry state:
		self.firstStartDatetime = None
		self.currentStartDatetime = None
		self.accumulatedDuration = timedelta()
		self.nextReminder = 0
		self.startStopStrVar.set(BUTTON_NAMES.START)
		self.currentEntryDurationStrVar.set(MetaDataProjectTime.durationToStr(self.accumulatedDuration))
		self.startDatetimeStrVar.set(MetaDataProjectTime.getDefaultFieldValue(MetaDataProjectTime.Field.START_TIME))
		# Update UI:
		self.updateSortedProjectsAndCategories()
		self.updateTotalDurations()
		self.updatePoolTime()
		createEntriesFrameGridFields()

	def doBeep(self) -> None:
		if winsound is not None:
			winsound.Beep(REMINDER_BEEP_FREQUENCY, min(REMINDER_BEEP_DURATION, REMINDER_BEEP_INTERVAL))
		else:
			self.root.bell()

	def alertTick(self) -> None:
		# Check if alert time is over:
		if datetime.now() >= self.alertUntilDatetime:
			self.currentEntryDurationLabel.configure(fg=DEFAULT_COLOR)
			return
		# Toggle label color and beep:
		newForeground = REMINDER_FLASH_COLOR if self.currentEntryDurationLabel.cget('fg') != REMINDER_FLASH_COLOR else DEFAULT_COLOR
		self.currentEntryDurationLabel.configure(fg=newForeground)
		threading.Thread(target=self.doBeep, daemon=True).start()
		# Schedule next tick:
		self.root.after(REMINDER_BEEP_INTERVAL, self.alertTick)

	def updateReminderChoice(self) -> None:
		self.reminderInterval = parseInt(self.reminderChoiceStrVar.get()) * 60
		if self.reminderInterval == 0:
			self.nextReminder = 0
			self.alertUntilDatetime = datetime.now()
			self.currentEntryDurationLabel.configure(fg=DEFAULT_COLOR)
		elif self.currentStartDatetime is not None:
			self.nextReminder = ((int((datetime.now() - self.currentStartDatetime + self.accumulatedDuration).total_seconds()) // self.reminderInterval) + 1) * self.reminderInterval

	def updateCurrentDuration(self) -> None:
		if self.currentStartDatetime is None:
			return
		# Update labels:
		self.currentEntryDurationStrVar.set(MetaDataProjectTime.durationToStr(datetime.now() - self.currentStartDatetime + self.accumulatedDuration))
		self.updateTotalDurations()
		self.updatePoolTime()
		# Check for reminder alert:
		if self.nextReminder > 0 and self.reminderInterval > 0 and int((datetime.now() - self.currentStartDatetime + self.accumulatedDuration).total_seconds()) >= self.nextReminder:
			if datetime.now() >= self.alertUntilDatetime:
				self.alertUntilDatetime = datetime.now() + timedelta(milliseconds=REMINDER_ALERT_DURATION)
				self.alertTick()
			self.nextReminder += self.reminderInterval
		# Schedule next update:
		self.root.after(UPDATE_INTERVAL, self.updateCurrentDuration)

	def updateTotalDurations(self) -> None:
		# Start total durations at current entry duration:
		currentEntryDuration = self.accumulatedDuration
		if self.currentStartDatetime is not None:
			currentEntryDuration += (datetime.now() - self.currentStartDatetime)
		totalProjectDuration = currentEntryDuration
		totalCategoryDuration = currentEntryDuration
		# Add durations from stored entries:
		currentProject = self.projectStrVar.get()
		currentCategory = self.categoryStrVar.get()
		for idx in range(metaData.getEntryCount()):
			if metaData.getFieldByIdx(MetaDataProjectTime.Field.PROJECT, idx) == currentProject:
				totalProjectDuration += timedelta(seconds=float(metaData.getFieldByIdx(MetaDataProjectTime.Field.DURATION, idx)))
			if metaData.getFieldByIdx(MetaDataProjectTime.Field.CATEGORY, idx) == currentCategory:
				totalCategoryDuration += timedelta(seconds=float(metaData.getFieldByIdx(MetaDataProjectTime.Field.DURATION, idx)))
		# Update labels:
		self.totalProjectDurationStrVar.set(MetaDataProjectTime.durationToStr(totalProjectDuration))
		self.totalCategoryDurationStrVar.set(MetaDataProjectTime.durationToStr(totalCategoryDuration))

	def updatePoolTime(self) -> None:
		if remainingPoolSeconds := calculateRemainingPoolSeconds(self.categoryStrVar.get(), (datetime.now() - self.currentStartDatetime + self.accumulatedDuration).total_seconds() if self.currentStartDatetime is not None else 0.0):
			self.poolTimeStrVar.set(('-' if remainingPoolSeconds < 0. else '') + MetaDataProjectTime.durationToStr(timedelta(seconds=abs(remainingPoolSeconds))))
			self.poolTimeLabel.configure(fg=(POOL_TIME_NEGATIVE_COLOR if remainingPoolSeconds < 0. else POOL_TIME_POSITIVE_COLOR))
		else:
			self.poolTimeStrVar.set(MetaDataProjectTime.getDefaultFieldValue(MetaDataProjectTime.Field.START_TIME))
			self.poolTimeLabel.configure(fg=DEFAULT_COLOR)

	def updateSortedProjectsForCurrentCategory(self) -> None:
		projects: set[str] = set()
		currentCategory = self.categoryStrVar.get()
		for idx in range(metaData.getEntryCount()):
			if metaData.getFieldByIdx(MetaDataProjectTime.Field.CATEGORY, idx) == currentCategory:
				projects.add(metaData.getFieldByIdx(MetaDataProjectTime.Field.PROJECT, idx))
		self.sortedProjects = sorted(projects)
		self.projectStrVar.set(self.sortedProjects[0] if self.sortedProjects else MetaDataProjectTime.getDefaultFieldValue(MetaDataProjectTime.Field.START_TIME))
		if hasattr(self, 'projectCombobox'):
			self.projectCombobox['values'] = self.sortedProjects

	def updateSortedProjectsAndCategories(self) -> None:
		categories: set[str] = set()
		for idx in range(metaData.getEntryCount()):
			categories.add(metaData.getFieldByIdx(MetaDataProjectTime.Field.CATEGORY, idx))
		self.sortedCategories = sorted(categories)
		self.updateSortedProjectsForCurrentCategory()


class EntriesList:
	def __init__(self) -> None:
		self.entryIdxList: list[int] = []
		self.readEntries()

	def getEntryIdx(self, row: int) -> int:
		return self.entryIdxList[row]

	def getEntryCount(self) -> int:
		return len(self.entryIdxList)

	def readEntries(self) -> None:
		self.entryIdxList.clear()
		for idx in range(metaData.getEntryCount()):
			self.entryIdxList.append(idx)
		self.entryIdxList.sort(key=lambda entryIdx: datetime.strptime(metaData.getFieldByIdx(MetaDataProjectTime.Field.START_TIME, entryIdx), MetaDataProjectTime.DATETIME_SAVE_FORMAT), reverse=True)

	def removeEntryByIdx(self, idx: int) -> None:
		assert controller is not None
		metaData.removeEntry(idx)
		metaData.writeMetaData()
		# Update UI:
		controller.updateSortedProjectsAndCategories()
		controller.updateTotalDurations()
		controller.updatePoolTime()
		createEntriesFrameGridFields()

	def calculatePoolTimeString(self, category: str) -> str:
		if remainingPoolSeconds := calculateRemainingPoolSeconds(category):
			return ('-' if remainingPoolSeconds < 0. else '') + MetaDataProjectTime.durationToStr(timedelta(seconds=abs(remainingPoolSeconds)))
		else:
			return MetaDataProjectTime.getDefaultFieldValue(MetaDataProjectTime.Field.START_TIME)


class CategoryPoolsWindow:
	def __init__(self, root: tk.Tk, headerFrame: tk.Frame, entriesFrame: tk.Frame):
		self.root = root
		self.headerFrame = headerFrame
		self.entriesFrame = entriesFrame
		self.poolTimeStrVars: dict[str, tk.StringVar] = {}
		self.totalTimeStrVars: dict[str, tk.StringVar] = {}

	def updatePoolTime(self, category: str) -> None:
		if remaining := calculateRemainingPoolSeconds(category):
			self.poolTimeStrVars[category].set(('-' if remaining < 0 else '') + MetaDataProjectTime.durationToStr(timedelta(seconds=abs(remaining))))
		else:
			self.poolTimeStrVars[category].set(MetaDataProjectTime.getDefaultFieldValue(MetaDataProjectTime.Field.START_TIME))

	def updateTotalTime(self, category: str) -> None:
		totalSeconds: float = 0.0
		for idx in range(metaData.getEntryCount()):
			if metaData.getFieldByIdx(MetaDataProjectTime.Field.CATEGORY, idx) == category:
				totalSeconds += float(metaData.getFieldByIdx(MetaDataProjectTime.Field.DURATION, idx))
		if controller is not None and controller.currentStartDatetime is not None and controller.getCategoryVar().get() == category:
			totalSeconds += (datetime.now() - controller.currentStartDatetime + controller.accumulatedDuration).total_seconds()
		self.totalTimeStrVars[category].set(MetaDataProjectTime.durationToStr(timedelta(seconds=totalSeconds)))

	def updatePoolAndTotalTimes(self) -> None:
		for category in self.poolTimeStrVars.keys():
			self.updatePoolTime(category)
			self.updateTotalTime(category)

	def onDailyTimeChanged(self, category: str, dailyTimeStrVar: tk.StringVar) -> None:
		dailyTime: int = parseInt(dailyTimeStrVar.get())
		if dailyTime == DAILY_TIME_POOLS.get(category, 0):
			return
		DAILY_TIME_POOLS[category] = dailyTime
		if controller is not None:
			controller.updatePoolTime()
		if entriesFrame is not None:
			createEntriesFrameGridFields()
		self.updatePoolAndTotalTimes()


def parseInt(value: str) -> int:  # FIXME: check why this does not allow for new values
	try:
		return int(max(0, float(value)))
	except Exception:
		return 0

def calculateTotalDurations(project: str, category: str) -> tuple[timedelta, timedelta]:
	totalProjectDuration = timedelta()
	totalCategoryDuration = timedelta()
	for idx in range(metaData.getEntryCount()):
		if metaData.getFieldByIdx(MetaDataProjectTime.Field.PROJECT, idx) == project:
			totalProjectDuration += timedelta(seconds=float(metaData.getFieldByIdx(MetaDataProjectTime.Field.DURATION, idx)))
		if metaData.getFieldByIdx(MetaDataProjectTime.Field.CATEGORY, idx) == category:
			totalCategoryDuration += timedelta(seconds=float(metaData.getFieldByIdx(MetaDataProjectTime.Field.DURATION, idx)))
	return totalProjectDuration, totalCategoryDuration

def calculateRemainingPoolSeconds(category: str, usedSeconds: float = 0.0) -> Optional[float]:
	if category not in DAILY_TIME_POOLS:
		return None
	# Find earliest start date and calculate used time from stored entries:
	firstDatetime = datetime.now()
	for idx in range(metaData.getEntryCount()):
		if metaData.getFieldByIdx(MetaDataProjectTime.Field.CATEGORY, idx) == category:
			entryDatetime = datetime.strptime(metaData.getFieldByIdx(MetaDataProjectTime.Field.START_TIME, idx), MetaDataProjectTime.DATETIME_SAVE_FORMAT)
			if entryDatetime < firstDatetime:
				firstDatetime = entryDatetime
			usedSeconds += float(metaData.getFieldByIdx(MetaDataProjectTime.Field.DURATION, idx))
	# Return daily pool time sum minus used time:
	return timedelta(minutes=DAILY_TIME_POOLS[category] * ((datetime.now().date() - firstDatetime.date()).days + 1)).total_seconds() - usedSeconds

def createHeaderFrameGridFields() -> None:
	global controller
	assert headerFrame is not None
	for widget in headerFrame.winfo_children():
		widget.destroy()
	controller = Controller(headerFrame)
	controller.updateSortedProjectsAndCategories()
	controller.updateTotalDurations()
	# Create grid fields for headers:
	row = 0
	GridField.add(headerFrame, row, (0, 2), HEADER_COLUMN_WIDTHS[0] + HEADER_COLUMN_WIDTHS[1], GridField.Type.Header, HEADER_COLUMN_NAMES[0])
	for column in range(2, len(HEADER_COLUMN_WIDTHS)):
		GridField.add(headerFrame, row, column, HEADER_COLUMN_WIDTHS[column], GridField.Type.Header, HEADER_COLUMN_NAMES[column-1])
	# Create grid fields for control fields:
	row += 1
	column = 0
	GridField.add(headerFrame, row, column, HEADER_COLUMN_WIDTHS[column], GridField.Type.Button, controller.getStartStopVar(), controller.startStopEntry, True)
	column += 1
	GridField.add(headerFrame, row, column, HEADER_COLUMN_WIDTHS[column], GridField.Type.Button, BUTTON_NAMES.END, controller.endEntry, False)
	column += 1
	GridField.add(headerFrame, row, column, HEADER_COLUMN_WIDTHS[column], GridField.Type.Combobox, controller.reminderChoiceStrVar, [str(choice) for choice in REMINDER_INTERVAL_CHOICES], lambda *_, controller=controller: controller.updateReminderChoice())
	column += 1
	controller.projectCombobox = GridField.add(headerFrame, row, column, HEADER_COLUMN_WIDTHS[column], GridField.Type.Combobox, controller.getProjectVar(), controller.getSortedProjects(), lambda *_, controller=controller: (controller.updateTotalDurations(), controller.updatePoolTime()))  # type: ignore
	column += 1
	GridField.add(headerFrame, row, column, HEADER_COLUMN_WIDTHS[column], GridField.Type.Combobox, controller.getCategoryVar(), controller.getSortedCategories(), lambda *_, controller=controller: (controller.updateSortedProjectsForCurrentCategory(), controller.updateTotalDurations(), controller.updatePoolTime()))
	column += 1
	controller.currentEntryDurationLabel = GridField.add(headerFrame, row, column, HEADER_COLUMN_WIDTHS[column], GridField.Type.DynamicLabel, controller.getCurrentEntryDurationStrVar())  # type: ignore
	column += 1
	GridField.add(headerFrame, row, column, HEADER_COLUMN_WIDTHS[column], GridField.Type.DynamicLabel, controller.getTotalProjectDurationStrVar())
	column += 1
	GridField.add(headerFrame, row, column, HEADER_COLUMN_WIDTHS[column], GridField.Type.DynamicLabel, controller.getTotalCategoryDurationStrVar())
	column += 1
	controller.poolTimeLabel = GridField.add(headerFrame, row, column, HEADER_COLUMN_WIDTHS[column], GridField.Type.DynamicLabel, controller.getPoolTimeStrVar())  # type: ignore
	controller.updatePoolTime()
	column += 1
	GridField.add(headerFrame, row, column, HEADER_COLUMN_WIDTHS[column], GridField.Type.DynamicLabel, controller.getStartDatetimeStrVar())

def createEntriesFrameGridFields() -> None:
	assert entriesFrame is not None
	for widget in entriesFrame.winfo_children():
		widget.destroy()
	entriesList = EntriesList()
	# Create grid fields for list entries:
	for row in range(entriesList.getEntryCount()):
		entryIdx = entriesList.getEntryIdx(row)
		project = metaData.getFieldByIdx(MetaDataProjectTime.Field.PROJECT, entryIdx)
		category = metaData.getFieldByIdx(MetaDataProjectTime.Field.CATEGORY, entryIdx)
		totalProject, totalCategory = calculateTotalDurations(project, category)
		column = 0
		GridField.add(entriesFrame, row, column, ENTRIES_COLUMN_WIDTHS[column], GridField.Type.Button, BUTTON_NAMES.REMOVE, lambda entryIdx=entryIdx: entriesList.removeEntryByIdx(entryIdx), False)
		column += 1
		GridField.add(entriesFrame, row, column, ENTRIES_COLUMN_WIDTHS[column], GridField.Type.Label, '')
		column += 1
		GridField.add(entriesFrame, row, column, ENTRIES_COLUMN_WIDTHS[column], GridField.Type.Label, project)
		column += 1
		GridField.add(entriesFrame, row, column, ENTRIES_COLUMN_WIDTHS[column], GridField.Type.Label, category)
		column += 1
		GridField.add(entriesFrame, row, column, ENTRIES_COLUMN_WIDTHS[column], GridField.Type.Label, MetaDataProjectTime.durationToStr(timedelta(seconds=float(metaData.getFieldByIdx(MetaDataProjectTime.Field.DURATION, entryIdx)))))
		column += 1
		GridField.add(entriesFrame, row, column, ENTRIES_COLUMN_WIDTHS[column], GridField.Type.Label, MetaDataProjectTime.durationToStr(totalProject))
		column += 1
		GridField.add(entriesFrame, row, column, ENTRIES_COLUMN_WIDTHS[column], GridField.Type.Label, MetaDataProjectTime.durationToStr(totalCategory))
		column += 1
		GridField.add(entriesFrame, row, column, ENTRIES_COLUMN_WIDTHS[column], GridField.Type.Label, entriesList.calculatePoolTimeString(category))
		column += 1
		GridField.add(entriesFrame, row, column, ENTRIES_COLUMN_WIDTHS[column], GridField.Type.Label, datetime.strptime(metaData.getFieldByIdx(MetaDataProjectTime.Field.START_TIME, entryIdx), MetaDataProjectTime.DATETIME_SAVE_FORMAT).strftime(DATETIME_DISPLAY_FORMAT))

def createCategoryPoolsFrameGridFields(root: tk.Tk, headerFrame: tk.Frame, entriesFrame: tk.Frame) -> None:
	global categoryPoolsWindow
	categoryPoolsWindow = CategoryPoolsWindow(root, headerFrame, entriesFrame)
	# Create grid fields for headers:
	for column, name in enumerate(CATEGORY_POOLS_COLUMN_NAMES):
		GridField.add(categoryPoolsWindow.headerFrame, 0, column, CATEGORY_POOLS_COLUMN_WIDTHS[column], GridField.Type.Header, name)
	# Create grid fields for categories:
	categories = set(DAILY_TIME_POOLS.keys())
	for idx in range(metaData.getEntryCount()):
		categories.add(metaData.getFieldByIdx(MetaDataProjectTime.Field.CATEGORY, idx))
	for row, category in enumerate(sorted(categories)):
		column = 0
		GridField.add(categoryPoolsWindow.entriesFrame, row, column, CATEGORY_POOLS_COLUMN_WIDTHS[column], GridField.Type.Label, category)
		column += 1
		dailyTimeStrVar = tk.StringVar(categoryPoolsWindow.root, str(DAILY_TIME_POOLS.get(category, 0)))
		combobox = GridField.add(categoryPoolsWindow.entriesFrame, row, column, CATEGORY_POOLS_COLUMN_WIDTHS[column], GridField.Type.Combobox, dailyTimeStrVar, DAILY_TIME_POOL_CHOICES)
		combobox.bind('<<ComboboxSelected>>', lambda *_ , category=category, dailyTimeStrVar=dailyTimeStrVar: categoryPoolsWindow.onDailyTimeChanged(category, dailyTimeStrVar))
		column += 1
		categoryPoolsWindow.poolTimeStrVars[category] = tk.StringVar(categoryPoolsWindow.root)
		GridField.add(categoryPoolsWindow.entriesFrame, row, column, CATEGORY_POOLS_COLUMN_WIDTHS[column], GridField.Type.DynamicLabel, categoryPoolsWindow.poolTimeStrVars[category])
		column += 1
		categoryPoolsWindow.totalTimeStrVars[category] = tk.StringVar(categoryPoolsWindow.root)
		GridField.add(categoryPoolsWindow.entriesFrame, row, column, CATEGORY_POOLS_COLUMN_WIDTHS[column], GridField.Type.DynamicLabel, categoryPoolsWindow.totalTimeStrVars[category])
		categoryPoolsWindow.updatePoolTime(category)
		categoryPoolsWindow.updateTotalTime(category)

def createCategoryPoolsWindow(root: tk.Tk) -> None:
	global categoryPoolsWindow
	root.title('Category time pools')
	root.geometry(str(CATEGORY_POOLS_WINDOW_SIZE[0]) + 'x' + str(CATEGORY_POOLS_WINDOW_SIZE[1]))
	root.resizable(0, 1)
	outerFrame = tk.Frame(root)
	outerFrame.pack(fill=tk.BOTH, expand=1)
	headerFrame = tk.Frame(outerFrame)
	headerFrame.pack(side=tk.TOP, fill=tk.X, expand=0)
	entriesCanvas = tk.Canvas(outerFrame)
	entriesCanvas.pack(side=tk.LEFT, fill=tk.BOTH, expand=1)
	entriesScrollbar = tk.Scrollbar(outerFrame, orient=tk.VERTICAL, command=entriesCanvas.yview)
	entriesScrollbar.pack(side=tk.RIGHT, fill=tk.Y)
	entriesCanvas.configure(yscrollcommand=entriesScrollbar.set)
	entriesCanvas.bind('<Configure>', lambda _: entriesCanvas.configure(scrollregion=entriesCanvas.bbox('all')))
	entriesFrame = tk.Frame(entriesCanvas)
	entriesCanvas.create_window((0, 0), window=entriesFrame, anchor='nw')
	createCategoryPoolsFrameGridFields(root, headerFrame, entriesFrame)

def createControlWindow(root: tk.Tk) -> None:
	global headerFrame
	global entriesFrame
	root.title('Project time tracker')
	root.geometry(str(WINDOW_SIZE[0]) + 'x' + str(WINDOW_SIZE[1]))
	root.resizable(0, 1)
	# Set up fixed header frame and scrollable entries frame:
	outerFrame = tk.Frame(root)
	outerFrame.pack(fill=tk.BOTH, expand=1)
	headerFrame = tk.Canvas(outerFrame)
	headerFrame.pack(side=tk.TOP, fill=tk.X, expand=0)
	createHeaderFrameGridFields()
	entriesCanvas = tk.Canvas(outerFrame)
	entriesCanvas.pack(side=tk.LEFT, fill=tk.BOTH, expand=1)
	entriesScrollbar = tk.Scrollbar(outerFrame, orient=tk.VERTICAL, command=entriesCanvas.yview)
	entriesScrollbar.pack(side=tk.RIGHT, fill=tk.Y)
	entriesCanvas.configure(yscrollcommand=entriesScrollbar.set)
	entriesCanvas.bind('<Configure>', lambda _: entriesCanvas.configure(scrollregion=entriesCanvas.bbox('all')))
	entriesFrame = tk.Frame(entriesCanvas)
	entriesCanvas.create_window((0, 0), window=entriesFrame, anchor='nw')
	createEntriesFrameGridFields()

def main() -> None:
	try:
		createControlWindow(tk.Tk())
		createCategoryPoolsWindow(tk.Tk())
		tk.mainloop()
	except Exception:
		print(traceback.format_exc())
		input('Press enter to close traceback.')

if __name__ == '__main__':
	main()
