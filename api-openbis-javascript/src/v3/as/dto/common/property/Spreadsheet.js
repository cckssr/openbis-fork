define([ "stjs" ], function(stjs) {
	var Spreadsheet = function() {
	};
	stjs.extend(Spreadsheet, null, [], function(constructor, prototype) {
		prototype['@type'] = 'as.dto.common.property.Spreadsheet';
		constructor.serialVersionUID = 1;

		prototype.version = '1';
		prototype.headers = null;
		prototype.data = null;
		prototype.values = null;
		prototype.width = null;
		prototype.style = null;
		prototype.meta = null;

		prototype.getVersion = function() {
			return this.version;
		};
		prototype.setVersion = function(version) {
			this.version = version;
		};
		prototype.getHeaders = function() {
			return this.headers;
		};
		prototype.setHeaders = function(headers) {
			this.headers = headers;
		};
		prototype.getData = function() {
			return this.data;
		};
		prototype.setData = function(data) {
			this.data = data;
		};
		prototype.getValues = function() {
			return this.values;
		};
		prototype.setValues = function(values) {
			this.values = values;
		};
		prototype.getWidth = function() {
			return this.width;
		};
		prototype.setWidth = function(width) {
			this.width = width;
		};
		prototype.getStyle = function() {
			return this.style;
		};
		prototype.setStyle = function(style) {
			this.style = style;
		};
		prototype.getMeta = function() {
			return this.meta;
		};
		prototype.setMeta = function(meta) {
			this.meta = meta;
		};

	}, {
	    style : {
            name : "Map",
            arguments : [ "String", "String" ]
        },
        meta : {
            name : "Map",
            arguments : [ "String", "String" ]
        },
        headers: "String[]",
        data: "String[][]",
        values: "String[][]",
        width: "Integer[]"
	});
	return Spreadsheet;
})
