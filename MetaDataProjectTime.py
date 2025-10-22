import math
from enum import Enum
from datetime import datetime, timedelta

class MetaDataProjectTime:
	class Field(Enum):
		PROJECT = 0
		CATEGORY = 1
		DURATION = 2
		START_TIME = 3

	ENCODING = 'utf-8'
	FILE_NAME = 'MetaDataProjectTime.txt'
	FIELD_SEPARATOR = ' --- '

	@staticmethod
	def isDuration(text: str) -> bool:
		try:
			return math.isfinite(float(text))
		except Exception:
			return False

	@staticmethod
	def isDatetime(text: str) -> bool:
		try:
			datetime.strptime(text, '%Y-%m-%d %H:%M:%S.%f')
			return True
		except Exception:
			return False

	@staticmethod
	def durationToStr(duration: timedelta) -> str:
		total_seconds = int(duration.total_seconds())
		hours = total_seconds // 3600
		minutes = (total_seconds % 3600) // 60
		seconds = total_seconds % 60
		if hours:
			return f"{hours}:{minutes:02}:{seconds:02}"
		else:
			return f"{minutes}:{seconds:02}"

	@staticmethod
	def isValidFieldValue(fieldType, fieldValue: str) -> bool:
		if fieldValue is None or MetaDataProjectTime.FIELD_SEPARATOR in fieldValue:
			return False
		match fieldType:
			case MetaDataProjectTime.Field.PROJECT:
				return fieldValue != '' and not fieldValue.isspace()
			case MetaDataProjectTime.Field.CATEGORY:
				return fieldValue != '' and not fieldValue.isspace()
			case MetaDataProjectTime.Field.DURATION:
				return MetaDataProjectTime.isDuration(fieldValue)
			case MetaDataProjectTime.Field.START_TIME:
				return MetaDataProjectTime.isDatetime(fieldValue)
			case _:
				return False

	@staticmethod
	def getDefaultFieldValue(fieldType) -> str:
		match fieldType:
			case MetaDataProjectTime.Field.PROJECT:
				return 'ProjectTimeTracker'
			case MetaDataProjectTime.Field.CATEGORY:
				return 'Programming'
			case MetaDataProjectTime.Field.DURATION:
				return '0:00'
			case MetaDataProjectTime.Field.START_TIME:
				return '2000-01-01 0:00:00.0'
			case _:
				return ''

	@staticmethod
	def getDefaultFieldSets():
		fieldSets = {}
		fieldSets[MetaDataProjectTime.Field.PROJECT] = {MetaDataProjectTime.getDefaultFieldValue(MetaDataProjectTime.Field.PROJECT)}
		fieldSets[MetaDataProjectTime.Field.CATEGORY] = {MetaDataProjectTime.getDefaultFieldValue(MetaDataProjectTime.Field.CATEGORY)}
		return fieldSets

	def __init__(self):
		self.metaData = []
		self.fieldSets = MetaDataProjectTime.getDefaultFieldSets()
		self.readMetaData()

	def getFieldTypeName(self, fieldType):
		return fieldType.name.capitalize().replace('_', ' ')

	def checkField(self, fieldType, fieldValue):
		if not MetaDataProjectTime.isValidFieldValue(fieldType, fieldValue):
			raise ValueError('Invalid formating for ' + self.getFieldTypeName(fieldType) + ' (' + fieldValue + ')')
		elif fieldType in self.fieldSets:
			self.fieldSets[fieldType].add(fieldValue)

	def checkFields(self, fields):
		for fieldType in MetaDataProjectTime.Field:
			self.checkField(fieldType, fields[fieldType.value])

	def addEntry(self, fields):
		self.checkFields(fields)
		self.metaData.append(fields)

	def readMetaData(self):
		metaDataFile = open(MetaDataProjectTime.FILE_NAME, 'r', encoding=MetaDataProjectTime.ENCODING)
		lines = metaDataFile.readlines()
		self.metaData = []
		for line in lines:
			fields = line.strip().split(MetaDataProjectTime.FIELD_SEPARATOR)
			self.checkFields(fields)
			self.metaData.append(fields)

	def writeMetaData(self):
		metaDataFormated = []
		for fields in self.metaData:
			metaDataFormated.append(MetaDataProjectTime.FIELD_SEPARATOR.join(fields))
		with open(MetaDataProjectTime.FILE_NAME, 'w', encoding=MetaDataProjectTime.ENCODING) as metaDataFile:
			metaDataFile.write('\n'.join(metaDataFormated))
