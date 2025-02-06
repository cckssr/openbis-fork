define([ "stjs", "util/Exceptions" ], function(stjs, exceptions) {
    var getDefaultHeaders = function(count) {
        if(count < 1) {
            throw new exceptions.IllegalArgumentException("Count must not be less than 1");
        }
        var alphabetMax = 26;
        var aCharacterValue = 'A'.charCodeAt(0);
        var headers = [];

        for(let minChar = aCharacterValue; minChar < aCharacterValue + Math.min(alphabetMax, count); minChar++)
        {
            headers.push(String.fromCharCode(minChar));
        }
        if(count > alphabetMax)
        {
            for(let x = 0; x < Math.floor(count / alphabetMax) ; x++)
            {
                let ch =  String.fromCharCode(aCharacterValue + x);

                for(let y = 0; y < Math.min(alphabetMax, count-alphabetMax*(x+1)) ; y++)
                {
                    headers.push(ch + String.fromCharCode(aCharacterValue + y));
                }
            }
        }
        return headers;
    };
	var Spreadsheet = function(columnNumber, rowNumber) {
	    let columnCount = columnNumber || 10;
	    var rowCount = rowNumber || 10;

	    if(columnCount < 0 || rowCount < 0)
        {
            throw new exceptions.IllegalArgumentException("Parameters must not be negative!");
        }
        this.headers = getDefaultHeaders(columnCount);
        this.data = Array.from(Array(rowCount), () => new Array(columnCount).fill(''));
        this.values = Array.from(Array(rowCount), () => new Array(columnCount).fill(''));
        this.width = new Array(columnCount).fill(50);
        this.style = {}
        this.headers.forEach(header => {
            [...Array(rowCount).keys()].forEach( rowIndex => {
                this.style[header + (rowIndex+1)] = "text-align: center;"
            });
        });
        this.meta = {}
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

		prototype.fromJson = function(jsonObject) {
		    this.version = jsonObject['version'] || this.version;
		    this.headers = jsonObject['headers'];
		    this.data = jsonObject['data'];
		    this.values = jsonObject['values'];
		    this.width = jsonObject['width'];
		    this.style = jsonObject['style'];
		    this.meta = jsonObject['meta'] || this.meta;
		    return this;
		};
		prototype.getVersion = function() {
			return this.version;
		};
		prototype.getMeta = function() {
			return this.meta;
		};
		prototype.setMeta = function(meta) {
			this.meta = meta;
		};
		prototype.getColumnCount = function() {
            return this.headers ? this.headers.length : 0;
        };
        prototype.getRowCount = function() {
            return this.data ? this.data.length : 0;
        };

        prototype.addRow = function() {
            this.data.push(new Array(this.headers.length).fill(''));
            this.values.push(new Array(this.headers.length).fill(''));
            for(let i=0; i < this.headers.length; i++)
            {
                this.style[this.headers[i] + this.data.length] = "text-align: center;";
            }
        };

        prototype.deleteRow = function(rowNumber) {
            if(!rowNumber || rowNumber < 1 || rowNumber > this.data.length) {
                throw new exceptions.IllegalArgumentException("Incorrect row number!");
            }
            this.data.splice(rowNumber-1, 1);
            this.values.splice(rowNumber-1, 1);
            for(let i=rowNumber-1; i<this.data.length;i++) {
                for(let j=0;j<this.headers.length;j++)
                {
                    this.style[this.headers[j] + i] = this.style[this.headers[j] + (i+1)];
                }
            }

            for(let j=0;j<this.headers.length;j++)
            {
                delete this.style[this.headers[j] + (this.data.length+1)];
            }
        };

        prototype.addColumn = function(label) {
            var columnLabel = label;
            if(!columnLabel) {
                columnLabel = getDefaultHeaders(this.headers.length+1)[this.headers.length];
            }

            if(!this.headers) {
                this.headers = [];
            }
            this.headers.push(columnLabel);
            if(!this.width) {
                this.width = [];
            }
            this.width.push(50);

            for(let i=0;i<this.data.length;i++) {
                this.data[i].push('');
                this.values[i].push('');
                this.style[label + (i+1)] = "text-align: center;";
            }
        };

        prototype.deleteColumn = function(labelOrNumber) {
            if(!labelOrNumber || labelOrNumber < 1 || labelOrNumber > this.headers.length) {
                throw new exceptions.IllegalArgumentException("Incorrect column index!");
            }
            var columnNumber = null;
            var index = null;
            var label = null;
            if(parseInt(Number(labelOrNumber)) == labelOrNumber) {
                columnNumber = parseInt(Number(labelOrNumber));
                index = columnNumber - 1;
                label = this.headers[index];
            }
            if(!columnNumber) {
                index = this.headers.indexOf(labelOrNumber);
                if(index < 0) {
                    throw new exceptions.IllegalArgumentException("Could not find column '" + labelOrNumber + "'");
                }
                label = this.headers[index];
            }

            this.headers.splice(index, 1);
            this.width.splice(index, 1);
            for(let i=0;i<this.data.length;i++) {
                this.data[i].splice(index, 1);
                this.values[i].splice(index, 1);
                delete this.style[label + (i+1)];
            }
        };

        // Helper class CellBuilder
        var CellBuilder = class {
            constructor(parent, column, row) {
                this.parent = parent;
                if(!column || column < 1 || column > parent.headers.length) {
                    throw new exceptions.IllegalArgumentException("Incorrect column index!");
                }
                var columnNumber = null;

                if(parseInt(Number(column)) == column) {
                    columnNumber = parseInt(Number(column));
                    this.columnIndex = columnNumber - 1;
                    this.columnHeader = parent.headers[this.columnIndex];

                }
                if(!columnNumber) {
                    this.columnIndex = parent.headers.indexOf(labelOrNumber);
                    if(this.columnIndex < 0) {
                        throw new exceptions.IllegalArgumentException("Could not find column '" + labelOrNumber + "'");
                    }
                    this.columnHeader = parent.headers[this.columnIndex];
                }

                if(!row || row < 1 || row > parent.data.length) {
                    throw new exceptions.IllegalArgumentException("Incorrect row index!");
                }
                this.rowIndex = parseInt(Number(row))-1;
            }

            getFormula()
            {
                return this.parent.data[this.rowIndex][this.columnIndex];
            }

            setFormula(formula)
            {
                this.parent.data[this.rowIndex][this.columnIndex] = formula;
            }

            getValue()
            {
                return this.parent.values[this.rowIndex][this.columnIndex];
            }

            setValue(value)
            {
                this.parent.values[this.rowIndex][this.columnIndex] = value;
                this.parent.data[this.rowIndex][this.columnIndex] = value;
            }

            getStyle()
            {
                return this.parent.style[this.columnHeader + (this.rowIndex+1)];
            }

            setStyle(newStyle)
            {
                this.parent.style[this.columnHeader + (this.rowIndex+1)] = newStyle;
            }

            getColumnHeader()
            {
                return this.columnHeader;
            }

            getColumnNumber()
            {
                return this.columnIndex+1;
            }

            getRowNumber()
            {
                return rthis.owIndex+1;
            }
        };

        // Helper class ColumnBuilder
        var ColumnBuilder = class {
            constructor(parent, column) {
                this.parent = parent;
                if(!column || column < 1 || column > parent.headers.length) {
                    throw new exceptions.IllegalArgumentException("Incorrect column index!");
                }
                var columnNumber = null;

                if(parseInt(Number(column)) == column) {
                    columnNumber = parseInt(Number(column));
                    this.index = columnNumber - 1;
                    this.label = parent.headers[this.index];

                }
                if(!columnNumber) {
                    this.index = parent.headers.indexOf(column);
                    if(this.index < 0) {
                        throw new exceptions.IllegalArgumentException("Could not find column '" + column + "'");
                    }
                    this.label = parent.headers[this.index];
                }
            }

            getHeader()
            {
                return this.parent.headers[this.index];
            }

            setHeader(header)
            {
                this.parent.headers[this.index] = header;
            }

            getWidth()
            {
                return this.parent.width[this.index];
            }

            setWidth(columnWidth)
            {
                return this.parent.width[this.index] = columnWidth;
            }

            getIndex()
            {
                return this.index;
            }

        };

        prototype.cell = function(column, row) {
            return new CellBuilder(this, column, row);
        };
        prototype.column = function(column) {
            return new ColumnBuilder(this, column);
        };

        prototype.toString = function() {
            return `Spreadsheet[version=${this.version}, columns=${this.getColumnCount()}, rows=${this.getRowCount()}]`
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
