from enum import Enum
from datetime import datetime

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
	def isDuration(text):
		# duration format: H:MM:SS or M:SS or S
		parts = text.split(':')
		if len(parts) > 3 or len(parts) == 0:
			return False
		for i, part in enumerate(parts):
			if part == '' or not part.isdigit():
				return False
			if i < len(parts)-1 and int(part) < 0:
				return False
		return True

	@staticmethod
	def timeToSeconds(time):
		assert MetaDataProjectTime.isDuration(time), f'Invalid duration "{time}"'
		parts = time.split(':')
		parts = [int(p) for p in parts]
		while len(parts) < 3:
			parts.insert(0, 0)
		hours, minutes, seconds = parts
		return (hours * 60 + minutes) * 60 + seconds

	@staticmethod
	def isDatetime(text):
		try:
			datetime.strptime(text, '%Y-%m-%d %H:%M:%S.%f')
			return True
		except Exception:
			return False

	@staticmethod
	def isValidFieldValue(fieldType, fieldValue):
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
	def getDefaultFieldValue(fieldType):
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
		fieldSets[MetaDataProjectTime.Field.PROJECT] = {'FitnessTracker', MetaDataProjectTime.getDefaultFieldValue(MetaDataProjectTime.Field.PROJECT)}
		fieldSets[MetaDataProjectTime.Field.CATEGORY] = {MetaDataProjectTime.getDefaultFieldValue(MetaDataProjectTime.Field.CATEGORY)}
		return fieldSets

	def __init__(self):
		self.metaData = []
		self.fieldSets = MetaDataProjectTime.getDefaultFieldSets()
		self.readMetaData()

	def getFieldTypeName(self, fieldType):
		return fieldType.name.capitalize().replace('_', ' ')

	def getFieldByIdx(self, fieldType, idx):
		return self.metaData[idx][fieldType.value]

	def getEntryByIdx(self, idx):
		return self.metaData[idx]

	def getEntryCount(self):
		return len(self.metaData)

	def setFieldByIdx(self, fieldType, idx, fieldValue):
		self.checkField(fieldType, fieldValue)
		self.metaData[idx][fieldType.value] = fieldValue

	def checkField(self, fieldType, fieldValue):
		if not MetaDataProjectTime.isValidFieldValue(fieldType, fieldValue):
			raise ValueError('Invalid formating for ' + self.getFieldTypeName(fieldType) + ' (' + fieldValue + ')')
		elif fieldType in self.fieldSets:
			self.fieldSets.get(fieldType).add(fieldValue)

	def checkFields(self, fields):
		for fieldType in MetaDataProjectTime.Field:
			self.checkField(fieldType, fields[fieldType.value])

	def addEntry(self, fields):
		fields[MetaDataProjectTime.Field.START_TIME.value] = str(datetime.now())
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
