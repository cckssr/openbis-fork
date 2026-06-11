export enum FormMode {
	VIEW = 'view',
	CREATE = 'create',
	EDIT = 'edit',
}

export enum EntityKind {
	SPACE = 'space',
	PROJECT = 'project',
	EXPERIMENT = 'experiment',
	OBJECT = 'object',
	SAMPLE = 'sample',
	COLLECTION = 'collection',
	DATASET = 'dataSet',
	DATA_SET = 'DATA_SET',
	NEW_SPACE = 'newSpace',
	NEW_PROJECT = 'newProject',
	NEW_OBJECT = 'newObject',
	NEW_COLLECTION = 'newCollection',
	NEW_DATASET = 'newDataSet',
}

export enum FormFieldDataType {
  VARCHAR = 'VARCHAR',
  MULTILINE_VARCHAR = 'MULTILINE_VARCHAR',
  WORD_PROCESSOR = 'WORD_PROCESSOR',
  WORD_PROCESSOR_PAGE = 'WORD_PROCESSOR_PAGE',
  WORD_PROCESSOR_CLASSIC = 'WORD_PROCESSOR_CLASSIC',
  MONOSPACE_FONT = 'MONOSPACE_FONT',
  INTEGER = 'INTEGER',
  REAL = 'REAL',
  TIMESTAMP = 'TIMESTAMP',
  BOOLEAN = 'BOOLEAN',
  CONTROLLEDVOCABULARY = 'CONTROLLEDVOCABULARY',
  HYPERLINK = 'HYPERLINK',
  SAMPLE = 'SAMPLE',
  SPREADSHEET = 'SPREADSHEET',
  ARRAY_INTEGER = 'ARRAY_INTEGER',
  ARRAY_REAL = 'ARRAY_REAL',
  ARRAY_STRING = 'ARRAY_STRING',
  ARRAY_TIMESTAMP = 'ARRAY_TIMESTAMP',
  JSON = 'JSON',
  XML = 'XML',
  DATE = 'DATE',
}

export enum Widget {
	RICH_TEXT = 'RichText',
	SPREADSHEET = 'Spreadsheet',
	WORD_PROCESSOR = 'Word Processor',
	WORD_PROCESSOR_PAGE = 'Word Processor Page',
	WORD_PROCESSOR_CLASSIC = 'Word Processor Classic',
	MONOSPACE_FONT = 'Monospace Font'
}

export enum FormSection {
	SELECT_TYPE = 'Select Type',
	IDENTIFICATION_INFO = 'Identification Info',
	GENERAL = 'General',
	OVERVIEW = 'Overview',
	METADATA = 'Metadata',
	UNKNOWN = 'Unknown',
}
