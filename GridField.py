import tkinter as tk
from tkinter import ttk
from enum import Enum, auto

class GridField():
	FRAME_TO_GRID_FIELD_WIDTH_FACTOR = 0.167

	class Type(Enum):
		Header = auto()
		Button = auto()
		Label = auto()
		DynamicLabel = auto()
		DigitEntry = auto()
		TextEntry = auto()
		Combobox = auto()
		Radiobutton = auto()
		Checkbutton = auto()

	radiobuttons = []
	radiobuttonWidth = 0

	def isFloat(text):
		return text.lstrip('-').replace('.', '').isdigit()and text.count('.') <= 1

	def isFloatOrEmpty(text):
		return str(text) == '' or GridField.isFloat()

	def isText(text):
		return text != None

	def add(root, row, column, width, type, arg1=None, arg2=None, arg3=None):
		if type == GridField.Type.DigitEntry:
			if arg2 != None:
				arg1.trace_add('write', arg2)
			vcmd = (root.register(GridField.isFloatOrEmpty), '%P')
			gridField = tk.Entry(root, justify='center', width=width, textvariable=arg1, validate='key', validatecommand=vcmd)
		elif type == GridField.Type.TextEntry:
			if arg2 != None:
				arg1.trace_add('write', arg2)
			vcmd = (root.register(GridField.isText), '%P')
			gridField = tk.Entry(root, justify='center', width=width, textvariable=arg1, validate='key', validatecommand=vcmd)
			if arg3 != None:
				gridField.bind("<Button-3>", lambda event, root=root, strVar=arg1: arg3(root, strVar))
		elif type == GridField.Type.Combobox:
			if arg3 != None:
				arg1.trace_add('write', arg3)
			gridField = ttk.Combobox(root, justify='center', width=width-5, textvariable=arg1)
			gridField['values'] = arg2
		elif type == GridField.Type.Radiobutton:
			style = ttk.Style(root)
			style.theme_use('classic')
			style.configure('IndicatorOff.TRadiobutton', indicatormargin=-1, indicatordiameter=-1, relief=tk.SUNKEN, focusthickness=0, highlightthickness=0, anchor='center')
			style.map('IndicatorOff.TRadiobutton', background=[('selected', 'grey'), ('active', '#ececec')])
			gridField = ttk.Radiobutton(root, style='IndicatorOff.TRadiobutton', width=width, text=arg1, variable=arg2, value=arg3)
			GridField.radiobuttons.append(gridField)
		elif type == GridField.Type.Checkbutton:
			gridField = ttk.Checkbutton(root, width=width, text=arg3, variable=arg1, command=arg2)
		elif type == GridField.Type.Button:
			if arg3 == True:
				gridField = tk.Button(root, textvariable=arg1, borderwidth=4, width=width-15, command=arg2)
			else:
				gridField = tk.Button(root, text=arg1, borderwidth=4, width=width-15, command=arg2)
		elif type == GridField.Type.Label:
			gridField = tk.Label(root, text=arg1, borderwidth=2, relief='sunken', width=width)
		elif type == GridField.Type.DynamicLabel:
			gridField = tk.Label(root, textvariable=arg1, borderwidth=2, relief='sunken', width=width)
		elif type == GridField.Type.Header:
			gridField = tk.Label(root, text=arg1, borderwidth=2, relief='groove', width=width)
		else:
			raise AttributeError('Invalid grid field type')
		if isinstance(column, int):
			gridField.grid(row=row, column=column, sticky='news')
		elif isinstance(column, tuple) and len(column) == 2:
			gridField.grid(row=row, column=column[0], columnspan=column[1], sticky='news')
			column = column[0]
		else:
			raise AttributeError('Invalid column type')
		root.rowconfigure(row, weight=1)
		root.columnconfigure(column, weight=1)

	def frameResizeCallback(event):
		newRadiobuttonWidth = int(event.width * GridField.FRAME_TO_GRID_FIELD_WIDTH_FACTOR)
		if newRadiobuttonWidth != GridField.radiobuttonWidth:
			GridField.radiobuttonWidth = newRadiobuttonWidth
			for radiobutton in GridField.radiobuttons:
				radiobutton.configure(width=newRadiobuttonWidth)
