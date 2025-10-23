import traceback
import tkinter as tk
from datetime import datetime, timedelta
from MetaDataProjectTime import MetaDataProjectTime
from GridField import GridField

# Configurables:
WINDOW_SIZE = (800, 500)
HEADER_COLUMN_NAMES = ('Control:', 'Project:', 'Category:', 'Current entry time:', 'Total project time:', 'Total category time:', '')
HEADER_COLUMN_WIDTHS = (10, 10, 20, 20, 20, 20, 20, 3)
ENTRIES_COLUMN_WIDTHS = (30, 18, 18, 18, 18, 18)
UPDATE_INTERVAL = 1000  # milliseconds
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
# TODO: add time reminder after every hour when active (configurable)
# TODO: add schedulable time pools for categories
# TODO: add start date as extra column in entries and header

class Controller:
	def __init__(self, root):
		self.root = root
		self.startStopStrVar = tk.StringVar(root, BUTTON_NAMES.START)
		self.projectStrVar = tk.StringVar(root, MetaDataProjectTime.getDefaultFieldValue(MetaDataProjectTime.Field.PROJECT))
		self.categoryStrVar = tk.StringVar(root, MetaDataProjectTime.getDefaultFieldValue(MetaDataProjectTime.Field.CATEGORY))
		self.currentEntryDurationStrVar = tk.StringVar(root, MetaDataProjectTime.getDefaultFieldValue(MetaDataProjectTime.Field.DURATION))
		self.totalProjectDurationStrVar = tk.StringVar(root, MetaDataProjectTime.getDefaultFieldValue(MetaDataProjectTime.Field.DURATION))
		self.totalCategoryDurationStrVar = tk.StringVar(root, MetaDataProjectTime.getDefaultFieldValue(MetaDataProjectTime.Field.DURATION))
		self.sortedProjects = []
		self.sortedCategories = []
		self.firstStartTime = None
		self.currentStartTime = None
		self.accumulatedDuration = timedelta()

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

	def getSortedProjects(self):
		return self.sortedProjects

	def getSortedCategories(self):
		return self.sortedCategories

	def startStopEntry(self):
		if self.currentStartTime is None:
			# Start or resume timer:
			self.currentStartTime = datetime.now()
			if self.firstStartTime is None:
				self.firstStartTime = self.currentStartTime
			self.startStopStrVar.set(BUTTON_NAMES.PAUSE)
			self.updateCurrentDuration()
		else:
			# Pause timer:
			self.accumulatedDuration += datetime.now() - self.currentStartTime
			self.currentStartTime = None
			self.startStopStrVar.set(BUTTON_NAMES.RESUME)
			self.currentEntryDurationStrVar.set(MetaDataProjectTime.durationToStr(self.accumulatedDuration))

	def endEntry(self):
		if self.firstStartTime is None:
			return
		# Store current entry:
		if self.currentStartTime is not None:
			self.accumulatedDuration += datetime.now() - self.currentStartTime
		metaData.addEntry([self.projectStrVar.get(), self.categoryStrVar.get(), str(self.accumulatedDuration.total_seconds()), str(self.firstStartTime)])
		metaData.writeMetaData()
		# Reset current entry state:
		self.firstStartTime = None
		self.currentStartTime = None
		self.accumulatedDuration = timedelta()
		self.startStopStrVar.set(BUTTON_NAMES.START)
		self.currentEntryDurationStrVar.set(MetaDataProjectTime.durationToStr(self.accumulatedDuration))
		# Update UI:
		self.updateSortedProjectsAndCategories()
		self.updateTotalDurations()
		createEntriesFrameGridFields()

	def updateCurrentDuration(self):
		if self.currentStartTime is None:
			return
		# Update labels:
		self.currentEntryDurationStrVar.set(MetaDataProjectTime.durationToStr(datetime.now() - self.currentStartTime + self.accumulatedDuration))
		self.updateTotalDurations()
		# Schedule next update:
		self.root.after(UPDATE_INTERVAL, self.updateCurrentDuration)

	def updateTotalDurations(self):
		# Start total durations at current entry duration:
		current_entry_duration = self.accumulatedDuration
		if self.currentStartTime is not None:
			current_entry_duration += (datetime.now() - self.currentStartTime)
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
		self.entryIdxList.sort(key=lambda entryIdx: datetime.strptime(metaData.getFieldByIdx(MetaDataProjectTime.Field.START_TIME, entryIdx), '%Y-%m-%d %H:%M:%S.%f'), reverse=True)

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
	GridField.add(headerFrame, row, column, HEADER_COLUMN_WIDTHS[column], GridField.Type.Combobox, controller.getProjectVar(), controller.getSortedProjects(), lambda *_, controller=controller: controller.updateTotalDurations())
	column += 1
	GridField.add(headerFrame, row, column, HEADER_COLUMN_WIDTHS[column], GridField.Type.Combobox, controller.getCategoryVar(), controller.getSortedCategories(), lambda *_, controller=controller: controller.updateTotalDurations())
	column += 1
	GridField.add(headerFrame, row, column, HEADER_COLUMN_WIDTHS[column], GridField.Type.DynamicLabel, controller.getCurrentEntryDurationStrVar())
	column += 1
	GridField.add(headerFrame, row, column, HEADER_COLUMN_WIDTHS[column], GridField.Type.DynamicLabel, controller.getTotalProjectDurationStrVar())
	column += 1
	GridField.add(headerFrame, row, column, HEADER_COLUMN_WIDTHS[column], GridField.Type.DynamicLabel, controller.getTotalCategoryDurationStrVar())

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
