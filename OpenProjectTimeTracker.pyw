import traceback
import tkinter as tk
from datetime import datetime
from MetaDataProjectTime import MetaDataProjectTime
from GridField import GridField

# Configurables:
WINDOW_SIZE = (800, 50)
HEADER_COLUMN_NAMES = ('Control:', 'Project:', 'Category:', 'Current entry time:', 'Total project time:', 'Total category time:')
HEADER_COLUMN_WIDTHS = (10, 10, 10, 10, 10, 10)
# Global variables:
metaData = MetaDataProjectTime()

class Controller:
	def __init__(self, root):
		self.root = root
		self.projectVar = tk.StringVar(root, '')
		self.categoryVar = tk.StringVar(root, '')
		self.startStopVar = tk.StringVar(root, 'Start')
		self.currentDurationVar = tk.StringVar(root, '0:00')
		self.totalProjectVar = tk.StringVar(root, '0:00')
		self.totalCategoryVar = tk.StringVar(root, '0:00')
		self.timerRunning = False
		self.entryStartTime = None
		self.entryAccumulated = 0  # seconds accumulated for current entry (if paused/resumed in future)

	def toggleStartStop(self):
		if not self.timerRunning:
			# start
			self.entryStartTime = datetime.now()
			self.timerRunning = True
			self.startStopVar.set('Stop')
			self.updateTimer()
		else:
			# stop and record
			elapsed = int((datetime.now() - self.entryStartTime).total_seconds())
			durationSeconds = elapsed + self.entryAccumulated
			durationStr = self.secondsToStr(durationSeconds)
			project = self.projectVar.get()
			category = self.categoryVar.get()
			if project == '':
				project = 'Unnamed'
			if category == '':
				category = 'General'
			fields = [project, category, durationStr, '']
			metaData.addEntry(fields)
			metaData.writeMetaData()
			self.timerRunning = False
			self.entryStartTime = None
			self.entryAccumulated = 0
			self.startStopVar.set('Start')
			self.refreshTotals()
			self.currentDurationVar.set('0:00')
			# update combobox lists
			self.updateComboboxValues()

	def secondsToStr(self, seconds: int) -> str:
		h = seconds // 3600
		m = (seconds % 3600) // 60
		s = seconds % 60
		if h > 0:
			return f"{h}:{m:02d}:{s:02d}"
		else:
			return f"{m}:{s:02d}"

	def updateTimer(self):
		if not self.timerRunning:
			return
		elapsed = int((datetime.now() - self.entryStartTime).total_seconds())
		total = elapsed + self.entryAccumulated
		self.currentDurationVar.set(self.secondsToStr(total))
		# also update totals live
		self.refreshTotals(live_project=self.projectVar.get(), live_category=self.categoryVar.get(), live_extra_seconds=total)
		self.root.after(500, self.updateTimer)

	def refreshTotals(self, live_project=None, live_category=None, live_extra_seconds=0):
		# compute totals from metaData
		total_project = 0
		total_category = 0
		for entry in metaData.metaData:
			project = entry[MetaDataProjectTime.Field.PROJECT.value]
			category = entry[MetaDataProjectTime.Field.CATEGORY.value]
			duration = entry[MetaDataProjectTime.Field.DURATION.value]
			try:
				secs = MetaDataProjectTime.timeToSeconds(duration)
			except AssertionError:
				secs = 0
			if live_project is not None and project == live_project:
				total_project += secs
			elif live_project is None and project == self.projectVar.get():
				total_project += secs
			if live_category is not None and category == live_category:
				total_category += secs
			elif live_category is None and category == self.categoryVar.get():
				total_category += secs
		# add live extra seconds if provided
		if live_project is not None and live_project == self.projectVar.get():
			total_project += live_extra_seconds
		if live_category is not None and live_category == self.categoryVar.get():
			total_category += live_extra_seconds
		# set display variables
		self.totalProjectVar.set(self.secondsToStr(total_project))
		self.totalCategoryVar.set(self.secondsToStr(total_category))

	def updateComboboxValues(self):
		projects = set()
		categories = set()
		for entry in metaData.metaData:
			projects.add(entry[MetaDataProjectTime.Field.PROJECT.value])
			categories.add(entry[MetaDataProjectTime.Field.CATEGORY.value])
		self.projectValues = sorted(x for x in projects if x != '')
		self.categoryValues = sorted(x for x in categories if x != '')

	def projectChanged(self, *args):
		# refresh totals when project changed
		self.refreshTotals()

	def categoryChanged(self, *args):
		self.refreshTotals()


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
	for column in range(len(HEADER_COLUMN_NAMES)):
		GridField.add(headerFrame, row, column, HEADER_COLUMN_WIDTHS[column], GridField.Type.Header, HEADER_COLUMN_NAMES[column])
	row += 1
	GridField.add(headerFrame, row, 0, HEADER_COLUMN_WIDTHS[0], GridField.Type.Button, controller.startStopVar, controller.toggleStartStop, True)
	GridField.add(headerFrame, row, 1, HEADER_COLUMN_WIDTHS[1], GridField.Type.Combobox, controller.projectVar, controller.projectValues, lambda *args: controller.projectChanged())
	GridField.add(headerFrame, row, 2, HEADER_COLUMN_WIDTHS[2], GridField.Type.Combobox, controller.categoryVar, controller.categoryValues, lambda *args: controller.categoryChanged())
	GridField.add(headerFrame, row, 3, HEADER_COLUMN_WIDTHS[3], GridField.Type.DynamicLabel, controller.currentDurationVar)
	GridField.add(headerFrame, row, 4, HEADER_COLUMN_WIDTHS[4], GridField.Type.DynamicLabel, controller.totalProjectVar)
	GridField.add(headerFrame, row, 5, HEADER_COLUMN_WIDTHS[5], GridField.Type.DynamicLabel, controller.totalCategoryVar)
	controller.projectVar.trace_add('write', lambda *args: controller.projectChanged())
	controller.categoryVar.trace_add('write', lambda *args: controller.categoryChanged())
	controller.refreshTotals()

def main():
	try:
		createControlWindow(tk.Tk())
		tk.mainloop()
	except Exception:
		print(traceback.format_exc())
		input('Press enter to close traceback.')

if __name__ == '__main__':
	main()
