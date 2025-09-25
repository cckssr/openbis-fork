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
	INTEGER = 'INTEGER',
	REAL = 'REAL',
	TIMESTAMP = 'TIMESTAMP',
	BOOLEAN = 'BOOLEAN',
	CONTROLLED_VOCABULARY = 'CONTROLLED_VOCABULARY',
	HYPERLINK = 'HYPERLINK',
	SAMPLE = 'SAMPLE',
	WORD_PROCESSOR = 'WORD_PROCESSOR',
	SPREADSHEET = 'SPREADSHEET',
}

export enum Widget {
	RICH_TEXT = 'RichText',
	SPREADSHEET = 'Spreadsheet'
}

export enum FormSection {
	IDENTIFICATION_INFO = 'Identification Info',
	GENERAL = 'General',
	OVERVIEW = 'Overview'
}