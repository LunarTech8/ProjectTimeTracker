import traceback
import threading
import tkinter as tk
from datetime import datetime, timedelta
from MetaDataProjectTime import MetaDataProjectTime
from GridField import GridField
try:
	import winsound
except Exception:
	winsound = None

# Configurables:
WINDOW_SIZE = (900, 300)
HEADER_COLUMN_NAMES = ('Control:', 'Reminder:', 'Project:', 'Category:', 'Current entry time:', 'Total project time:', 'Total category time:', 'Start date:', '')
HEADER_COLUMN_WIDTHS = (10, 10, 12, 20, 20, 20, 20, 20, 20, 5)
ENTRIES_COLUMN_WIDTHS = (36, 16, 16, 16, 16, 16, 16)
UPDATE_INTERVAL = 1000  # milliseconds
REMINDER_ALERT_DURATION = 3000  # milliseconds
REMINDER_BEEP_INTERVAL = 250  # milliseconds
REMINDER_BEEP_DURATION = 200  # milliseconds
REMINDER_BEEP_FREQUENCY = 3000  # Hz
REMINDER_CHOICES_MINUTES = (0, 15, 30, 60, 120)
REMINDER_FLASH_COLOR = 'blue'
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
metaData = MetaDataProjectTime()
controller = None
headerFrame = None
entriesFrame = None
# TODO: add schedulable time pools for categories
# TODO: finder better system to make header and entries columns widths the same

class Controller:
	def __init__(self, root):
		self.root = root
		self.startStopStrVar = tk.StringVar(root, BUTTON_NAMES.START)
		self.reminderChoiceStrVar = tk.StringVar(root, str(REMINDER_CHOICES_MINUTES[0]))
		self.projectStrVar = tk.StringVar(root, MetaDataProjectTime.getDefaultFieldValue(MetaDataProjectTime.Field.PROJECT))
		self.categoryStrVar = tk.StringVar(root, MetaDataProjectTime.getDefaultFieldValue(MetaDataProjectTime.Field.CATEGORY))
		self.currentEntryDurationStrVar = tk.StringVar(root, MetaDataProjectTime.getDefaultFieldValue(MetaDataProjectTime.Field.DURATION))
		self.totalProjectDurationStrVar = tk.StringVar(root, MetaDataProjectTime.getDefaultFieldValue(MetaDataProjectTime.Field.DURATION))
		self.totalCategoryDurationStrVar = tk.StringVar(root, MetaDataProjectTime.getDefaultFieldValue(MetaDataProjectTime.Field.DURATION))
		self.startDatetimeStrVar = tk.StringVar(root, MetaDataProjectTime.getDefaultFieldValue(MetaDataProjectTime.Field.START_TIME))
		self.sortedProjects = []
		self.sortedCategories = []
		self.firstStartDatetime = None
		self.currentStartDatetime = None
		self.accumulatedDuration = timedelta()
		self.reminderInterval = 0  # seconds
		self.nextReminder = 0  # seconds
		self.alertUntilDatetime = datetime.now()
		self.currentEntryDurationLabel: tk.Label

	def getStartStopVar(self):
		return self.startStopStrVar

	def getProjectVar(self):
		return self.projectStrVar

	def getCategoryVar(self):
		return self.categoryStrVar

	def getCurrentEntryDurationStrVar(self):
		return self.currentEntryDurationStrVar

	def getTotalProjectDurationStrVar(self):
		return self.totalProjectDurationStrVar

	def getTotalCategoryDurationStrVar(self):
		return self.totalCategoryDurationStrVar

	def getStartDatetimeStrVar(self):
		return self.startDatetimeStrVar

	def getSortedProjects(self):
		return self.sortedProjects

	def getSortedCategories(self):
		return self.sortedCategories

	def startStopEntry(self):
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

	def endEntry(self):
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
		createEntriesFrameGridFields()

	def doBeep(self):
		if winsound is not None:
			winsound.Beep(REMINDER_BEEP_FREQUENCY, min(REMINDER_BEEP_DURATION, REMINDER_BEEP_INTERVAL))
		else:
			self.root.bell()

	def alertTick(self):
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

	def parseReminderChoice(self, value: str) -> int:
		try:
			return int(max(0, float(value)) * 60)
		except Exception:
			return 0

	def updateReminderChoice(self):
		self.reminderInterval = self.parseReminderChoice(self.reminderChoiceStrVar.get())
		if self.reminderInterval == 0:
			self.nextReminder = 0
			self.alertUntilDatetime = datetime.now()
			self.currentEntryDurationLabel.configure(fg=DEFAULT_COLOR)
		elif self.currentStartDatetime is not None:
			self.nextReminder = ((int((datetime.now() - self.currentStartDatetime + self.accumulatedDuration).total_seconds()) // self.reminderInterval) + 1) * self.reminderInterval

	def updateCurrentDuration(self):
		if self.currentStartDatetime is None:
			return
		# Update labels:
		self.currentEntryDurationStrVar.set(MetaDataProjectTime.durationToStr(datetime.now() - self.currentStartDatetime + self.accumulatedDuration))
		self.updateTotalDurations()
		# Check for reminder alert:
		if self.nextReminder > 0 and self.reminderInterval > 0 and int((datetime.now() - self.currentStartDatetime + self.accumulatedDuration).total_seconds()) >= self.nextReminder:
			if datetime.now() >= self.alertUntilDatetime:
				self.alertUntilDatetime = datetime.now() + timedelta(milliseconds=REMINDER_ALERT_DURATION)
				self.alertTick()
			self.nextReminder += self.reminderInterval
		# Schedule next update:
		self.root.after(UPDATE_INTERVAL, self.updateCurrentDuration)

	def updateTotalDurations(self):
		# Start total durations at current entry duration:
		current_entry_duration = self.accumulatedDuration
		if self.currentStartDatetime is not None:
			current_entry_duration += (datetime.now() - self.currentStartDatetime)
		total_project_duration = current_entry_duration
		total_category_duration = current_entry_duration
		# Add durations from stored entries:
		current_project = self.projectStrVar.get()
		current_category = self.categoryStrVar.get()
		for idx in range(metaData.getEntryCount()):
			if metaData.getFieldByIdx(MetaDataProjectTime.Field.PROJECT, idx) == current_project:
				total_project_duration += timedelta(seconds=float(metaData.getFieldByIdx(MetaDataProjectTime.Field.DURATION, idx)))
			if metaData.getFieldByIdx(MetaDataProjectTime.Field.CATEGORY, idx) == current_category:
				total_category_duration += timedelta(seconds=float(metaData.getFieldByIdx(MetaDataProjectTime.Field.DURATION, idx)))
		# Update labels:
		self.totalProjectDurationStrVar.set(MetaDataProjectTime.durationToStr(total_project_duration))
		self.totalCategoryDurationStrVar.set(MetaDataProjectTime.durationToStr(total_category_duration))

	def updateSortedProjectsAndCategories(self):
		projects = set()
		categories = set()
		for idx in range(metaData.getEntryCount()):
			projects.add(metaData.getFieldByIdx(MetaDataProjectTime.Field.PROJECT, idx))
			categories.add(metaData.getFieldByIdx(MetaDataProjectTime.Field.CATEGORY, idx))
		self.sortedProjects = sorted(projects)
		self.sortedCategories = sorted(categories)


class EntriesList:
	def __init__(self):
		self.entryIdxList: list[int] = []
		self.readEntries()

	def getEntryIdx(self, row):
		return self.entryIdxList[row]

	def getEntryCount(self):
		return len(self.entryIdxList)

	def readEntries(self):
		self.entryIdxList.clear()
		for idx in range(metaData.getEntryCount()):
			self.entryIdxList.append(idx)
		self.entryIdxList.sort(key=lambda entryIdx: datetime.strptime(metaData.getFieldByIdx(MetaDataProjectTime.Field.START_TIME, entryIdx), MetaDataProjectTime.DATETIME_SAVE_FORMAT), reverse=True)

	def removeEntryByIdx(self, idx):
		assert controller is not None
		metaData.removeEntry(idx)
		metaData.writeMetaData()
		# Update UI:
		controller.updateSortedProjectsAndCategories()
		controller.updateTotalDurations()
		createEntriesFrameGridFields()


def calculateTotalDurations(project, category):
	total_project_duration = timedelta()
	total_category_duration = timedelta()
	for idx in range(metaData.getEntryCount()):
		if metaData.getFieldByIdx(MetaDataProjectTime.Field.PROJECT, idx) == project:
			total_project_duration += timedelta(seconds=float(metaData.getFieldByIdx(MetaDataProjectTime.Field.DURATION, idx)))
		if metaData.getFieldByIdx(MetaDataProjectTime.Field.CATEGORY, idx) == category:
			total_category_duration += timedelta(seconds=float(metaData.getFieldByIdx(MetaDataProjectTime.Field.DURATION, idx)))
	return total_project_duration, total_category_duration

def createHeaderFrameGridFields():
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
	GridField.add(headerFrame, row, column, HEADER_COLUMN_WIDTHS[column], GridField.Type.Combobox, controller.reminderChoiceStrVar, [str(choice) for choice in REMINDER_CHOICES_MINUTES], lambda *_, controller=controller: controller.updateReminderChoice())
	column += 1
	GridField.add(headerFrame, row, column, HEADER_COLUMN_WIDTHS[column], GridField.Type.Combobox, controller.getProjectVar(), controller.getSortedProjects(), lambda *_, controller=controller: controller.updateTotalDurations())
	column += 1
	GridField.add(headerFrame, row, column, HEADER_COLUMN_WIDTHS[column], GridField.Type.Combobox, controller.getCategoryVar(), controller.getSortedCategories(), lambda *_, controller=controller: controller.updateTotalDurations())
	column += 1
	controller.currentEntryDurationLabel = GridField.add(headerFrame, row, column, HEADER_COLUMN_WIDTHS[column], GridField.Type.DynamicLabel, controller.getCurrentEntryDurationStrVar())  # type: ignore
	column += 1
	GridField.add(headerFrame, row, column, HEADER_COLUMN_WIDTHS[column], GridField.Type.DynamicLabel, controller.getTotalProjectDurationStrVar())
	column += 1
	GridField.add(headerFrame, row, column, HEADER_COLUMN_WIDTHS[column], GridField.Type.DynamicLabel, controller.getTotalCategoryDurationStrVar())
	column += 1
	GridField.add(headerFrame, row, column, HEADER_COLUMN_WIDTHS[column], GridField.Type.DynamicLabel, controller.getStartDatetimeStrVar())

def createEntriesFrameGridFields():
	assert entriesFrame is not None
	for widget in entriesFrame.winfo_children():
		widget.destroy()
	entriesList = EntriesList()
	# Create grid fields for list entries:
	for row in range(entriesList.getEntryCount()):
		entryIdx = entriesList.getEntryIdx(row)
		project = metaData.getFieldByIdx(MetaDataProjectTime.Field.PROJECT, entryIdx)
		category = metaData.getFieldByIdx(MetaDataProjectTime.Field.CATEGORY, entryIdx)
		total_project, total_category = calculateTotalDurations(project, category)
		column = 0
		GridField.add(entriesFrame, row, column, ENTRIES_COLUMN_WIDTHS[column], GridField.Type.Button, BUTTON_NAMES.REMOVE, lambda entryIdx=entryIdx: entriesList.removeEntryByIdx(entryIdx), False)
		column += 1
		GridField.add(entriesFrame, row, column, ENTRIES_COLUMN_WIDTHS[column], GridField.Type.Label, project)
		column += 1
		GridField.add(entriesFrame, row, column, ENTRIES_COLUMN_WIDTHS[column], GridField.Type.Label, category)
		column += 1
		GridField.add(entriesFrame, row, column, ENTRIES_COLUMN_WIDTHS[column], GridField.Type.Label, MetaDataProjectTime.durationToStr(timedelta(seconds=float(metaData.getFieldByIdx(MetaDataProjectTime.Field.DURATION, entryIdx)))))
		column += 1
		GridField.add(entriesFrame, row, column, ENTRIES_COLUMN_WIDTHS[column], GridField.Type.Label, MetaDataProjectTime.durationToStr(total_project))
		column += 1
		GridField.add(entriesFrame, row, column, ENTRIES_COLUMN_WIDTHS[column], GridField.Type.Label, MetaDataProjectTime.durationToStr(total_category))
		column += 1
		GridField.add(entriesFrame, row, column, ENTRIES_COLUMN_WIDTHS[column], GridField.Type.Label, datetime.strptime(metaData.getFieldByIdx(MetaDataProjectTime.Field.START_TIME, entryIdx), MetaDataProjectTime.DATETIME_SAVE_FORMAT).strftime(DATETIME_DISPLAY_FORMAT))

def createControlWindow(root):
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

def main():
	try:
		createControlWindow(tk.Tk())
		tk.mainloop()
	except Exception:
		print(traceback.format_exc())
		input('Press enter to close traceback.')

if __name__ == '__main__':
	main()
