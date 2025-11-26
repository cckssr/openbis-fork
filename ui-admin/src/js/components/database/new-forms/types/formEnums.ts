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
	CONTROLLED_VOCABULARY = 'CONTROLLED_VOCABULARY',
	HYPERLINK = 'HYPERLINK',
	SAMPLE = 'SAMPLE',
	SPREADSHEET = 'SPREADSHEET',
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
