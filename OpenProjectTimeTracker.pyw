import traceback
import tkinter as tk
from datetime import datetime, timedelta
from MetaDataProjectTime import MetaDataProjectTime
from GridField import GridField

# Configurables:
WINDOW_SIZE = (800, 50)
HEADER_COLUMN_NAMES = ('Control:', 'Project:', 'Category:', 'Current entry time:', 'Total project time:', 'Total category time:')
HEADER_COLUMN_WIDTHS = (10, 10, 20, 20, 20, 20, 20)
UPDATE_INTERVAL = 1000  # milliseconds
class BUTTON_NAMES():
	START = 'Start'
	PAUSE = 'Pause'
	RESUME = 'Resume'
	END = 'End'
# Global variables:
metaData = MetaDataProjectTime()

class Controller:
	def __init__(self, root):
		self.root = root
		self.startStopVar = tk.StringVar(root, BUTTON_NAMES.START)
		self.projectVar = tk.StringVar(root, MetaDataProjectTime.getDefaultFieldValue(MetaDataProjectTime.Field.PROJECT))
		self.categoryVar = tk.StringVar(root, MetaDataProjectTime.getDefaultFieldValue(MetaDataProjectTime.Field.CATEGORY))
		self.currentEntryDurationVar = tk.StringVar(root, MetaDataProjectTime.getDefaultFieldValue(MetaDataProjectTime.Field.DURATION))
		self.totalProjectDurationVar = tk.StringVar(root, MetaDataProjectTime.getDefaultFieldValue(MetaDataProjectTime.Field.DURATION))
		self.totalCategoryDurationVar = tk.StringVar(root, MetaDataProjectTime.getDefaultFieldValue(MetaDataProjectTime.Field.DURATION))
		self.firstStartTime = None
		self.currentStartTime = None
		self.accumulatedDuration = timedelta()

	def startStopEntry(self):
		if self.currentStartTime is None:
			# Start or resume timer:
			self.currentStartTime = datetime.now()
			if self.firstStartTime is None:
				self.firstStartTime = self.currentStartTime
			self.startStopVar.set(BUTTON_NAMES.PAUSE)
			self.updateCurrentDuration()
		else:
			# Pause timer:
			self.accumulatedDuration += datetime.now() - self.currentStartTime
			self.currentStartTime = None
			self.startStopVar.set(BUTTON_NAMES.RESUME)
			self.currentEntryDurationVar.set(MetaDataProjectTime.durationToStr(self.accumulatedDuration))

	def endEntry(self):
		if self.firstStartTime is None:
			return
		# Store current entry:
		if self.currentStartTime is not None:
			self.accumulatedDuration += datetime.now() - self.currentStartTime
		metaData.addEntry([self.projectVar.get(), self.categoryVar.get(), str(self.accumulatedDuration.total_seconds()), str(self.firstStartTime)])
		metaData.writeMetaData()
		# Reset current entry state:
		self.firstStartTime = None
		self.currentStartTime = None
		self.accumulatedDuration = timedelta()
		self.startStopVar.set(BUTTON_NAMES.START)
		self.currentEntryDurationVar.set(MetaDataProjectTime.durationToStr(self.accumulatedDuration))
		self.updateTotalDurations()
		self.updateComboboxValues()

	def updateCurrentDuration(self):
		if self.currentStartTime is None:
			return
		# Update labels:
		self.currentEntryDurationVar.set(MetaDataProjectTime.durationToStr(datetime.now() - self.currentStartTime + self.accumulatedDuration))
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
		current_project = self.projectVar.get()
		current_category = self.categoryVar.get()
		for entry in metaData.metaData:
			if entry[MetaDataProjectTime.Field.PROJECT.value] == current_project:
				total_project_duration += timedelta(seconds=float(entry[MetaDataProjectTime.Field.DURATION.value]))
			if entry[MetaDataProjectTime.Field.CATEGORY.value] == current_category:
				total_category_duration += timedelta(seconds=float(entry[MetaDataProjectTime.Field.DURATION.value]))
		# Update labels:
		self.totalProjectDurationVar.set(MetaDataProjectTime.durationToStr(total_project_duration))
		self.totalCategoryDurationVar.set(MetaDataProjectTime.durationToStr(total_category_duration))

	def updateComboboxValues(self):
		projects = set()
		categories = set()
		for entry in metaData.metaData:
			projects.add(entry[MetaDataProjectTime.Field.PROJECT.value])
			categories.add(entry[MetaDataProjectTime.Field.CATEGORY.value])
		self.projectValues = sorted(projects)
		self.categoryValues = sorted(categories)


def createControlWindow(root):
	controller = Controller(root)
	root.title('Project time tracker')
	root.geometry(str(WINDOW_SIZE[0]) + 'x' + str(WINDOW_SIZE[1]))
	root.resizable(0, 1)
	# Set up fixed header frame:
	outerFrame = tk.Frame(root)
	outerFrame.pack(fill=tk.BOTH, expand=1)
	headerFrame = tk.Canvas(outerFrame)
	headerFrame.pack(side=tk.TOP, fill=tk.X, expand=0)
	controller.updateComboboxValues()
	row = 0
	GridField.add(headerFrame, row, (0, 2), HEADER_COLUMN_WIDTHS[0] + HEADER_COLUMN_WIDTHS[1], GridField.Type.Header, HEADER_COLUMN_NAMES[0])
	for column in range(2, len(HEADER_COLUMN_WIDTHS)):
		GridField.add(headerFrame, row, column, HEADER_COLUMN_WIDTHS[column], GridField.Type.Header, HEADER_COLUMN_NAMES[column-1])
	row += 1
	GridField.add(headerFrame, row, 0, HEADER_COLUMN_WIDTHS[0], GridField.Type.Button, controller.startStopVar, controller.startStopEntry, True)
	GridField.add(headerFrame, row, 1, HEADER_COLUMN_WIDTHS[1], GridField.Type.Button, BUTTON_NAMES.END, controller.endEntry, False)
	GridField.add(headerFrame, row, 2, HEADER_COLUMN_WIDTHS[2], GridField.Type.Combobox, controller.projectVar, controller.projectValues, lambda *_: controller.updateTotalDurations())
	GridField.add(headerFrame, row, 3, HEADER_COLUMN_WIDTHS[3], GridField.Type.Combobox, controller.categoryVar, controller.categoryValues, lambda *_: controller.updateTotalDurations())
	GridField.add(headerFrame, row, 4, HEADER_COLUMN_WIDTHS[4], GridField.Type.DynamicLabel, controller.currentEntryDurationVar)
	GridField.add(headerFrame, row, 5, HEADER_COLUMN_WIDTHS[5], GridField.Type.DynamicLabel, controller.totalProjectDurationVar)
	GridField.add(headerFrame, row, 6, HEADER_COLUMN_WIDTHS[6], GridField.Type.DynamicLabel, controller.totalCategoryDurationVar)
	controller.updateTotalDurations()
	# TODO: Add scrollable frame for stored entries

def main():
	try:
		createControlWindow(tk.Tk())
		tk.mainloop()
	except Exception:
		print(traceback.format_exc())
		input('Press enter to close traceback.')

if __name__ == '__main__':
	main()
