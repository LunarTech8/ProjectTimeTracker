from enum import Enum

class MetaDataDailyTimePools:
	class Field(Enum):
		CATEGORY = 0
		DAILY_MINUTES = 1

	ENCODING = 'utf-8'
	FILE_NAME = 'MetaDataDailyTimePools.txt'
	FIELD_SEPARATOR = ' --- '

	def __init__(self) -> None:
		self.metaData: dict[str, int] = {}
		self.readMetaData()

	def getDailyMinutes(self, category: str) -> int:
		return self.metaData.get(category, 0)

	def setDailyMinutes(self, category: str, minutes: int) -> None:
		self.metaData[category] = max(0, minutes)
		self.writeMetaData()

	def getCategories(self) -> set[str]:
		return set(self.metaData.keys())

	def readMetaData(self) -> None:
		metaDataFile = open(MetaDataDailyTimePools.FILE_NAME, 'r', encoding=MetaDataDailyTimePools.ENCODING)
		lines = metaDataFile.readlines()
		self.metaData = {}
		for line in lines:
			category, dailyMinutes = line.strip().split(MetaDataDailyTimePools.FIELD_SEPARATOR)
			self.metaData[category] = int(dailyMinutes)

	def writeMetaData(self) -> None:
		metaDataFormated: list[str] = []
		for category in sorted(self.metaData.keys()):
			metaDataFormated.append(MetaDataDailyTimePools.FIELD_SEPARATOR.join([category, str(self.metaData[category])]))
		with open(MetaDataDailyTimePools.FILE_NAME, 'w', encoding=MetaDataDailyTimePools.ENCODING) as metaDataFile:
			metaDataFile.write('\n'.join(metaDataFormated))
