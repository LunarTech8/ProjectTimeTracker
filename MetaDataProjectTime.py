import math
import win32com.client
from enum import Enum
from datetime import datetime, timedelta

class MetaDataProjectTime:
	class Field(Enum):
		PROJECT = 0
		CATEGORY = 1
		DURATION = 2
		START_TIME = 3

	ENCODING = 'utf-8'
	FILE_NAME = 'MetaDataProjectTime.txt.lnk'
	FIELD_SEPARATOR = ' --- '
	DATETIME_SAVE_FORMAT = '%Y-%m-%d %H:%M:%S.%f'

	@staticmethod
	def isDuration(text: str) -> bool:
		try:
			return math.isfinite(float(text))
		except Exception:
			return False

	@staticmethod
	def isDatetime(text: str) -> bool:
		try:
			datetime.strptime(text, MetaDataProjectTime.DATETIME_SAVE_FORMAT)
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
	def isValidFieldValue(fieldType: 'MetaDataProjectTime.Field', fieldValue: str) -> bool:
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
	def getDefaultFieldValue(fieldType: 'MetaDataProjectTime.Field') -> str:
		match fieldType:
			case MetaDataProjectTime.Field.PROJECT:
				return 'ProjectTimeTracker'
			case MetaDataProjectTime.Field.CATEGORY:
				return 'Programming'
			case MetaDataProjectTime.Field.DURATION:
				return '0:00'
			case MetaDataProjectTime.Field.START_TIME:
				return '-'
			case _:
				return ''

	@staticmethod
	def getDefaultFieldSets() -> dict['MetaDataProjectTime.Field', set[str]]:
		fieldSets: dict['MetaDataProjectTime.Field', set[str]] = {}
		fieldSets[MetaDataProjectTime.Field.PROJECT] = {MetaDataProjectTime.getDefaultFieldValue(MetaDataProjectTime.Field.PROJECT)}
		fieldSets[MetaDataProjectTime.Field.CATEGORY] = {MetaDataProjectTime.getDefaultFieldValue(MetaDataProjectTime.Field.CATEGORY)}
		return fieldSets

	@staticmethod
	def getMetaDataPath() -> str:
		try:
			if MetaDataProjectTime.FILE_NAME.endswith('.lnk'):
				return win32com.client.Dispatch("WScript.Shell").CreateShortCut(MetaDataProjectTime.FILE_NAME).Targetpath
			else:
				return MetaDataProjectTime.FILE_NAME
		except Exception as e:
			raise RuntimeError(f"Failed to resolve '{MetaDataProjectTime.FILE_NAME}': {e}")

	def __init__(self) -> None:
		self.metaData: list[list[str]] = []
		self.fieldSets = MetaDataProjectTime.getDefaultFieldSets()
		self.readMetaData()

	def getFieldTypeName(self, fieldType: 'MetaDataProjectTime.Field') -> str:
		return fieldType.name.capitalize().replace('_', ' ')

	def getFieldByIdx(self, fieldType: 'MetaDataProjectTime.Field', idx: int) -> str:
		return self.metaData[idx][fieldType.value]

	def getEntryByIdx(self, idx: int) -> list[str]:
		return self.metaData[idx]

	def getEntryCount(self) -> int:
		return len(self.metaData)

	def checkField(self, fieldType: 'MetaDataProjectTime.Field', fieldValue: str) -> None:
		if not MetaDataProjectTime.isValidFieldValue(fieldType, fieldValue):
			raise ValueError('Invalid formating for ' + self.getFieldTypeName(fieldType) + ' (' + fieldValue + ')')
		elif fieldType in self.fieldSets:
			self.fieldSets[fieldType].add(fieldValue)

	def checkFields(self, fields: list[str]) -> None:
		for fieldType in MetaDataProjectTime.Field:
			self.checkField(fieldType, fields[fieldType.value])

	def addEntry(self, fields: list[str]) -> None:
		self.checkFields(fields)
		self.metaData.append(fields)

	def removeEntry(self, idx: int) -> None:
		self.metaData.pop(idx)

	def readMetaData(self) -> None:
		metaDataFile = open(MetaDataProjectTime.getMetaDataPath(), 'r', encoding=MetaDataProjectTime.ENCODING)
		lines = metaDataFile.readlines()
		self.metaData = []
		for line in lines:
			fields = line.strip().split(MetaDataProjectTime.FIELD_SEPARATOR)
			self.checkFields(fields)
			self.metaData.append(fields)

	def writeMetaData(self) -> None:
		metaDataFormated: list[str] = []
		for fields in self.metaData:
			metaDataFormated.append(MetaDataProjectTime.FIELD_SEPARATOR.join(fields))
		with open(MetaDataProjectTime.getMetaDataPath(), 'w', encoding=MetaDataProjectTime.ENCODING) as metaDataFile:
			metaDataFile.write('\n'.join(metaDataFormated))
