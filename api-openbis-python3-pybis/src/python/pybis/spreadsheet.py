#   Copyright ETH 2024 -2025 Zürich, Scientific IT Services
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#        http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.
#

import json
import copy

from pandas import DataFrame

def _nonzero(num):
    if num != 0:
        return 1
    return 0

def _get_headers(count):
    """Algorithm for generating headers, maximum number of columns supported: 26*26=676"""
    if count < 1:
        raise ValueError("Can not create spreadsheet without columns!")
    min_char = ord('A')
    alphabet_max = 26
    headers = [chr(x) for x in range(min_char, min_char+min(alphabet_max, count))]
    if count > alphabet_max:
        for x in range(count // alphabet_max):
            char = min_char + x
            headers += [chr(char) + chr(min_char+y) for y in range(min(alphabet_max, count - alphabet_max*(x+1)))]
    return headers

class Spreadsheet:
    headers: list
    data: list
    style: dict
    meta: dict
    width: list
    values: list
    version: str

    def __init__(self, columns=10, rows=10):
        self.version = "1"
        self.headers = _get_headers(columns)
        self.data = [["" for _ in range(columns)] for _ in range(rows)]
        self.style = {
                header + str(y): "text-align: center;" for header in self.headers for y in range(1, rows+1)
            }
        self.meta = {}
        self.width = [50 for _ in range(columns)]
        self.values = [["" for _ in range(columns)] for _ in range(rows)]

    def _get_index_str(self, index):
        index = index.strip()
        column = ""
        row = ""
        headers = True
        for i in index:
            if i.isalpha():
                if not headers:
                    raise ValueError("Wrong index schema!")
                column += i
            elif ord(i) >= 48 and ord(i) <= 57:
                headers = False
                row += i
            else:
                raise ValueError("Wrong index schema!")
        if not column in self.headers:
            raise ValueError(f"Column '{column}' does not exists!")
        if row == "":
            raise ValueError("Missing row index!")
        row = int(row)
        if len(self.data) < row or row < 1:
            raise ValueError(f"Row '{row}' does not exists!")

        return row - 1, self.headers.index(column)

    def _get_index_tuple(self, index):
        column, row = index
        if column is None or column == "" or row is None or row == "":
            raise ValueError(f"'{index}' is not a valid index!")

        if str(column).isdigit():
            column = int(column)
        else:
            if not column in self.headers:
                raise ValueError(f"Column '{column}' does not exists!")
            column = self.headers.index(column) + 1

        row = int(row)
        if len(self.data) < row or row < 1:
            raise ValueError(f"Row '{row}' does not exists!")

        return row, column

    def _get_index(self, index):
        if index is None:
            raise ValueError("Index must not be None!")
        if isinstance(index, tuple):
            return self._get_index_tuple(index)
        raise ValueError(f"'{index}' is not a valid index!")

    def __getitem__(self, index):
        (row, column) = self._get_index(index)
        return self.CellBuilder(self, column, row)

    def get_column_count(self):
        return len(self.headers)

    def get_row_count(self):
        return len(self.data)

    def get_version(self):
        return self.version

    def get_meta_data(self):
        return self.meta

    def get_formulas(self):
        return copy.deepcopy(self.data)

    def get_headers(self):
        return copy.deepcopy(self.headers)

    def get_values(self):
        return copy.deepcopy(self.values)

    def get_width(self):
        return copy.deepcopy(self.width)

    def get_style(self):
        return copy.deepcopy(self.style)

    def df(self, attribute):
        options = ['headers', 'formulas', 'width', 'values']
        if attribute not in options:
            raise ValueError(f"Attribute '{attribute}' not found in the spreadsheet! Available attributes are: {options}")

        if attribute == 'headers':
            return DataFrame(self.headers)
        elif attribute == 'formulas':
            return DataFrame(self.data, columns=self.headers, index=range(1, len(self.data)+1, 1))
        elif attribute == 'values':
            return DataFrame(self.values, columns=self.headers, index=range(1, len(self.values) + 1, 1))
        elif attribute == 'width':
            return DataFrame(self.width)


    def add_column(self, column_name=None):
        if column_name is None:
            column_name = _get_headers(len(self.headers)+1)[-1]
        self.headers += [column_name]
        for row in self.data:
            row += ['']
        for row in self.values:
            row += ['']
        self.width += [50]
        for x in range(1, len(self.data[0])):
            self.style[column_name + str(x)] =  "text-align: center;"

    def add_row(self):
        self.data += [['' for _ in range(len(self.headers))]]
        self.values += [['' for _ in range(len(self.headers))]]

        for header in self.headers:
            self.style[header + str(len(self.data))] =  "text-align: center;"

    def delete_row(self, row_number):
        row = int(row_number)
        if len(self.data) < row or row < 1:
            raise ValueError(f"Row '{row}' does not exists!")

        for header in self.headers:
            for i in range(row, len(self.data), 1):
                self.style[header + str(i)] = self.style[header + str(i+1)]
            del self.style[header + str(len(self.data))]

        self.data.pop(row - 1)
        self.values.pop(row - 1)

    def delete_column(self, column_identifier):
        if str(column_identifier).isdigit():
            column_identifier = int(column_identifier)
            if column_identifier < 1 or column_identifier > len(self.headers):
                raise ValueError(f"Column '{column_identifier}' does not exists!")
            column_index = column_identifier-1
            column_label = self.headers[column_index]
        else:
            if not column_identifier in self.headers:
                raise ValueError(f"Column '{column_identifier}' does not exists!")
            column_index = self.headers.index(column_identifier)
            column_label = self.headers[column_index]

        self.headers.pop(column_index)
        self.width.pop(column_index)
        for i in range(len(self.data)):
            self.data[i].pop(column_index)
            self.values[i].pop(column_index)
            del self.style[column_label+str(i+1)]


    def cell(self, column_identifier, row_number):
        return self.CellBuilder(self, column_identifier, row_number)

    def column(self, column_identifier):
        return self.ColumnBuilder(self, column_identifier)

    class CellBuilder:

        def __init__(self, parent, column_identifier, row_number):
            self.__dict__['parent'] = parent
            if column_identifier is None or column_identifier == "" or row_number is None or row_number == "":
                raise ValueError(f"('{column_identifier}','{row_number}') is not a valid index!")

            if str(column_identifier).isdigit():
                column_identifier = int(column_identifier)
                if column_identifier < 1 or column_identifier > len(parent.headers):
                    raise ValueError(f"Column '{column_identifier}' does not exists!")
                self.__dict__['column_index'] = column_identifier - 1
                self.__dict__['column_label'] = parent.headers[self.__dict__['column_index']]
            else:
                if not column_identifier in parent.headers:
                    raise ValueError(f"Column '{column_identifier}' does not exists!")
                self.__dict__['column_index'] = parent.headers.index(column_identifier)
                self.__dict__['column_label'] = parent.headers[self.__dict__['column_index']]

            row = int(row_number)
            if len(parent.data) < row or row < 1:
                raise ValueError(f"Row '{row}' does not exists!")
            self.__dict__['row_index'] = row - 1

        def __getattr__(self, name):
            row = self.__dict__['row_index']
            column = self.__dict__['column_index']
            label = self.__dict__['column_label']
            if name == "formula":
                return self.__dict__['parent'].data[row][column]
            elif name == "value":
                return self.__dict__['parent'].values[row][column]
            elif name == "style":
                return self.__dict__['parent'].style[label + str(row+1)]
            elif name == "column_header":
                return label
            elif name == "column_number":
                return column+1
            elif name == "row_number":
                return row+1
            else:
                raise ValueError(f"No such attribute '{name}' found!")


        def __setattr__(self, name, value):
            row = self.__dict__['row_index']
            column = self.__dict__['column_index']
            label = self.__dict__['column_label']
            if name == "formula":
                self.__dict__['parent'].data[row][column] = value
            elif name == "value":
                self.__dict__['parent'].values[row][column] = value
            elif name == "style":
                self.__dict__['parent'].style[label + str(column + 1)]  = value
            else:
                raise ValueError(f"No such attribute '{name}' found!")

        def __str__(self):
            attr = self.__dict__
            row = attr['row_index']
            column = attr['column_index']
            return f"Cell[column={column}, row={row}, formula={attr['parent'].data[row][column]}, value={attr['parent'].values[row][column]}]"


    class ColumnBuilder:
        def __init__(self, parent, column_identifier):
            # self.__dict__['__df'] = None
            self.__dict__['parent'] = parent
            if column_identifier is None or column_identifier == "":
                raise ValueError(f"('{column_identifier}') is not a valid column index!")

            if str(column_identifier).isdigit():
                column_identifier = int(column_identifier)
                if column_identifier < 1 or column_identifier > len(parent.headers):
                    raise ValueError(f"Column '{column_identifier}' does not exists!")
                self.__dict__['column_index'] = column_identifier - 1
                self.__dict__['column_label'] = parent.headers[self.__dict__['column_index']]
            else:
                if not column_identifier in parent.headers:
                    raise ValueError(f"Column '{column_identifier}' does not exists!")
                self.__dict__['column_index'] = parent.headers.index(column_identifier)
                self.__dict__['column_label'] = parent.headers[self.__dict__['column_index']]

        def __getattr__(self, name):
            if name == "header":
                return self.__dict__['column_label']
            elif name == "width":
                return self.__dict__['parent'].width[self.__dict__['column_index']]
            elif name == "column_number":
                return self.__dict__['column_index']+1
            else:
                raise ValueError(f"No such attribute '{name}' found!")

        def __setattr__(self, name, value):
            if name == "header":
                self.__dict__['parent'].headers[self.__dict__['column_index']] = value
            elif name == "width":
                self.__dict__['parent'].width[self.__dict__['column_index']] = value
            else:
                raise ValueError(f"No such attribute '{name}' found!")

        def __str__(self):
            attr = self.__dict__
            return f"Column[column={attr['column_index']}, header={attr['column_label']}]"


    def __str__(self):
        return json.dumps(self.__dict__, default=lambda x: x.__dict__)

    def __repr__(self):
        return json.dumps(self.__dict__, default=lambda x: x.__dict__)

    def to_json(self):

        def dictionary_creator(x):
            dictionary = x.__dict__
            return dictionary

        return json.dumps(self, default=dictionary_creator, sort_keys=True, indent=4)

    @classmethod
    def from_dict(cls, data):
        if data is None:
            return None
        result = cls(10)
        for prop in cls.__annotations__.keys():
            attribute = data.get(prop)
            result.__dict__[prop] = attribute
        return result
