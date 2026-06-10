#
#   Copyright ETH 2018 - 2026 Zürich, Scientific IT Services
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
"""Spreadsheet widget for openBIS XML properties rendered as tables.

A :class:`Spreadsheet` is returned when a ``MULTILINE_VARCHAR`` or ``XML``
property has been configured in openBIS with the custom widget
``SPREADSHEET``.  It represents a two-dimensional grid with labelled columns
(``A``, ``B``, … ``Z``, ``AA``, …) and 1-based row numbers.

Data access::

    sheet = sample.props.my_spreadsheet
    sheet.cell("B", 3).formula  # read formula at B3
    sheet.cell("B", 3).formula = "=A3*2"  # write formula
    sheet.df("formulas")  # DataFrame of all formulas
    sheet.df("values")  # DataFrame of all computed values
"""

import copy
import json
from typing import Any, Optional, Union

from pandas import DataFrame


def _nonzero(num: int) -> int:
    """Return 1 if ``num`` is non-zero, otherwise 0."""
    if num != 0:
        return 1
    return 0


def _get_headers(count: int) -> list[str]:
    """Generate spreadsheet column headers up to ``count`` columns.

    Produces single-letter headers ``A``–``Z`` and then two-letter headers
    ``AA``, ``AB``, … up to a maximum of 676 columns (26×26).

    Args:
        count: Number of column headers to generate.  Must be ≥ 1.

    Returns:
        A list of column header strings, e.g. ``["A", "B", ..., "Z", "AA"]``.

    Raises:
        ValueError: If ``count`` is less than 1.
    """
    if count < 1:
        raise ValueError("Can not create spreadsheet without columns!")
    min_char = ord("A")
    alphabet_max = 26
    headers = [chr(x) for x in range(min_char, min_char + min(alphabet_max, count))]
    if count > alphabet_max:
        for x in range(count // alphabet_max):
            char = min_char + x
            headers += [
                chr(char) + chr(min_char + y)
                for y in range(min(alphabet_max, count - alphabet_max * (x + 1)))
            ]
    return headers


class Spreadsheet:
    """A two-dimensional spreadsheet embedded in an openBIS XML property.

    Cells are identified by a column header (``"A"``, ``"B"``, … ``"AA"``,
    …) and a 1-based row number.  Each cell stores a *formula* (the raw
    input) and a *value* (the computed display value).

    Typical usage — reading::

        sheet = sample.props.my_spreadsheet_prop
        sheet.df("values")  # pandas DataFrame of computed values
        sheet.cell("A", 1).value  # value at A1
        sheet.cell("A", 1).formula

    Typical usage — writing::

        sheet.cell("A", 1).formula = "Hello"
        sheet.cell("B", 1).formula = "=A1"
        sheet.add_row()
        sample.save()

    Attributes:
        headers (list[str]): Column header labels.
        data (list[list[str]]): 2-D list of formula strings.
        values (list[list[str]]): 2-D list of computed value strings.
        style (dict): Per-cell CSS style strings keyed by ``"A1"``, ``"B2"``,
            etc.
        meta (dict): Arbitrary metadata dict (custom widget metadata).
        width (list[int]): Per-column pixel widths.
        version (str): Spreadsheet format version (``"1"``).
    """

    headers: list[str]
    data: list[list[str]]
    values: list[list[str]]
    style: dict[str, str]
    meta: dict[str, Any]
    width: list[int]
    version: str

    def __init__(self, columns: int = 10, rows: int = 10) -> None:
        """Create a blank spreadsheet with the given dimensions.

        Args:
            columns: Number of columns (default 10).
            rows: Number of rows (default 10).
        """
        self.version = "1"
        self.headers = _get_headers(columns)
        self.data = [["" for _ in range(columns)] for _ in range(rows)]
        self.style = {
            header + str(y): "text-align: center;"
            for header in self.headers
            for y in range(1, rows + 1)
        }
        self.meta = {}
        self.width = [50 for _ in range(columns)]
        self.values = [["" for _ in range(columns)] for _ in range(rows)]

    def _set_data(self, data: "Spreadsheet") -> None:
        """Populate this spreadsheet from another :class:`Spreadsheet` instance.

        Called internally when re-loading data from the server.

        Args:
            data: A :class:`Spreadsheet` (or compatible object) with accessor
                methods ``get_version()``, ``get_headers()``, etc.
        """
        self.version = data.get_version()
        self.headers = data.get_headers()
        self.data = data.get_formulas()
        self.style = data.get_style()
        self.meta = data.get_meta_data()
        self.width = data.get_width()
        self.values = data.get_values()

    def _get_index_str(self, index: str) -> tuple[int, int]:
        """Parse a string cell reference like ``"B3"`` into ``(row, col)`` indices.

        Args:
            index: Cell reference string, e.g. ``"B3"`` or ``"AA12"``.

        Returns:
            A ``(row_index, col_index)`` tuple (both 0-based).

        Raises:
            ValueError: If the column or row part is invalid or out of range.
        """
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

    def _get_index_tuple(self, index: tuple) -> tuple[int, int]:
        """Parse a ``(column, row)`` tuple into ``(row, col)`` indices.

        Args:
            index: A ``(column_identifier, row_number)`` tuple.  The column
                can be a label string (``"B"``) or a 1-based integer.

        Returns:
            A ``(row_number, column_number)`` tuple (both 1-based).

        Raises:
            ValueError: If either component is empty, ``None``, or out of
                range.
        """
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

    def _get_index(self, index: Any) -> tuple[int, int]:
        """Dispatch to the appropriate index parser.

        Args:
            index: A ``(column, row)`` tuple.

        Returns:
            Parsed ``(row, column)`` integers.

        Raises:
            ValueError: If ``index`` is ``None`` or not a tuple.
        """
        if index is None:
            raise ValueError("Index must not be None!")
        if isinstance(index, tuple):
            return self._get_index_tuple(index)
        raise ValueError(f"'{index}' is not a valid index!")

    def __getitem__(self, index: tuple) -> "Spreadsheet.CellBuilder":
        """Return a :class:`CellBuilder` for the cell at ``index``.

        Args:
            index: A ``(column_identifier, row_number)`` tuple.

        Returns:
            A :class:`CellBuilder` for reading/writing the cell.

        Example:
            >>> sheet[("B", 3)].formula
        """
        (row, column) = self._get_index(index)
        return self.CellBuilder(self, column, row)

    def get_column_count(self) -> int:
        """Return the number of columns.

        Returns:
            Number of columns as an integer.
        """
        return len(self.headers)

    def get_row_count(self) -> int:
        """Return the number of rows.

        Returns:
            Number of rows as an integer.
        """
        return len(self.data)

    def get_version(self) -> str:
        """Return the spreadsheet format version string.

        Returns:
            Version string, currently always ``"1"``.
        """
        return self.version

    def get_meta_data(self) -> dict:
        """Return the metadata dict.

        Returns:
            The ``meta`` dict (may be empty).
        """
        return self.meta

    def get_formulas(self) -> list:
        """Return a deep copy of the 2-D formula grid.

        Returns:
            A 2-D list of formula strings.
        """
        return copy.deepcopy(self.data)

    def get_headers(self) -> list[str]:
        """Return a copy of the column header list.

        Returns:
            A list of header label strings.
        """
        return copy.deepcopy(self.headers)

    def get_values(self) -> list:
        """Return a deep copy of the 2-D computed-value grid.

        Returns:
            A 2-D list of value strings.
        """
        return copy.deepcopy(self.values)

    def get_width(self) -> list[int]:
        """Return a copy of the column widths list.

        Returns:
            A list of pixel width integers.
        """
        return copy.deepcopy(self.width)

    def get_style(self) -> dict:
        """Return a copy of the per-cell style dict.

        Returns:
            A dict mapping cell references (``"A1"``, …) to CSS strings.
        """
        return copy.deepcopy(self.style)

    def df(self, attribute: str) -> DataFrame:
        """Return a :class:`~pandas.DataFrame` view of the spreadsheet.

        Args:
            attribute: One of ``"headers"``, ``"formulas"``, ``"values"``,
                or ``"width"``.

        Returns:
            A :class:`~pandas.DataFrame` for the requested attribute.
            For ``"formulas"`` and ``"values"`` the headers are used as
            column labels and row indices are 1-based.

        Raises:
            ValueError: If ``attribute`` is not one of the allowed options.

        Example:
            >>> sheet.df("values")
            >>> sheet.df("formulas")
        """
        options = ["headers", "formulas", "width", "values"]
        if attribute not in options:
            raise ValueError(
                f"Attribute '{attribute}' not found in the spreadsheet! Available attributes are: {options}"
            )

        if attribute == "headers":
            return DataFrame(self.headers)
        elif attribute == "formulas":
            return DataFrame(
                self.data, columns=self.headers, index=range(1, len(self.data) + 1, 1)
            )
        elif attribute == "values":
            return DataFrame(
                self.values,
                columns=self.headers,
                index=range(1, len(self.values) + 1, 1),
            )
        elif attribute == "width":
            return DataFrame(self.width)

    def add_column(self, column_name: Optional[str] = None) -> None:
        """Append a new empty column to the right of the spreadsheet.

        Args:
            column_name: Header label for the new column.  If ``None``, the
                next label in the sequence is used (e.g. ``"K"`` after
                ``"J"``).

        Example:
            >>> sheet.add_column()  # auto-label
            >>> sheet.add_column("STATUS")  # custom label
        """
        if column_name is None:
            column_name = _get_headers(len(self.headers) + 1)[-1]
        self.headers += [column_name]
        for row in self.data:
            row += [""]
        for row in self.values:
            row += [""]
        self.width += [50]
        for x in range(1, len(self.data[0])):
            self.style[column_name + str(x)] = "text-align: center;"

    def add_row(self) -> None:
        """Append a new empty row at the bottom of the spreadsheet.

        Example:
            >>> sheet.add_row()
        """
        self.data += [["" for _ in range(len(self.headers))]]
        self.values += [["" for _ in range(len(self.headers))]]

        for header in self.headers:
            self.style[header + str(len(self.data))] = "text-align: center;"

    def delete_row(self, row_number: int) -> None:
        """Delete the row at ``row_number`` (1-based).

        Args:
            row_number: 1-based row index to delete.

        Raises:
            ValueError: If ``row_number`` is out of range.

        Example:
            >>> sheet.delete_row(3)  # removes row 3
        """
        row = int(row_number)
        if len(self.data) < row or row < 1:
            raise ValueError(f"Row '{row}' does not exists!")

        for header in self.headers:
            for i in range(row, len(self.data), 1):
                self.style[header + str(i)] = self.style[header + str(i + 1)]
            del self.style[header + str(len(self.data))]

        self.data.pop(row - 1)
        self.values.pop(row - 1)

    def delete_column(self, column_identifier: Union[int, str]) -> None:
        """Delete a column by its label or 1-based integer index.

        Args:
            column_identifier: Column header label (e.g. ``"B"``) or
                1-based column number.

        Raises:
            ValueError: If ``column_identifier`` is out of range or not found.

        Example:
            >>> sheet.delete_column("B")
            >>> sheet.delete_column(2)
        """
        if str(column_identifier).isdigit():
            column_identifier = int(column_identifier)
            if column_identifier < 1 or column_identifier > len(self.headers):
                raise ValueError(f"Column '{column_identifier}' does not exists!")
            column_index = column_identifier - 1
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
            del self.style[column_label + str(i + 1)]

    def cell(
        self, column_identifier: Union[int, str], row_number: int
    ) -> "Spreadsheet.CellBuilder":
        """Return a :class:`CellBuilder` for a specific cell.

        Args:
            column_identifier: Column header label (``"A"``) or 1-based index.
            row_number: 1-based row number.

        Returns:
            A :class:`CellBuilder` for reading/writing the cell.

        Example:
            >>> cell = sheet.cell("B", 3)
            >>> cell.formula = "42"
            >>> print(cell.value)
            42
        """
        return self.CellBuilder(self, column_identifier, row_number)

    def column(self, column_identifier: Union[int, str]) -> "Spreadsheet.ColumnBuilder":
        """Return a :class:`ColumnBuilder` for a specific column.

        Args:
            column_identifier: Column header label or 1-based index.

        Returns:
            A :class:`ColumnBuilder` for reading/writing column properties.

        Example:
            >>> col = sheet.column("B")
            >>> col.width = 120
            >>> col.header = "Concentration"
        """
        return self.ColumnBuilder(self, column_identifier)

    class CellBuilder:
        """Read/write accessor for a single spreadsheet cell.

        Obtain via :meth:`~Spreadsheet.cell` or
        :meth:`~Spreadsheet.__getitem__`.

        Attributes:
            formula (str): The raw input formula of the cell.  Can be set.
            value (str): The computed display value (read-only).
            style (str): The CSS style string for this cell (read-only via
                attribute; set via ``cell.style = "…"`` is not yet wired).
            column_header (str): The column label (e.g. ``"B"``).
            column_number (int): The 1-based column index.
            row_number (int): The 1-based row index.

        Example:
            >>> cell = sheet.cell("A", 1)
            >>> cell.formula = "=B1+C1"
            >>> print(cell.value)
            42
        """

        def __init__(
            self,
            parent: "Spreadsheet",
            column_identifier: Union[int, str],
            row_number: int,
        ) -> None:
            """Initialise a CellBuilder.

            Args:
                parent: The owning :class:`Spreadsheet`.
                column_identifier: Column label or 1-based index.
                row_number: 1-based row index.

            Raises:
                ValueError: If the column or row is invalid.
            """
            self.__dict__["parent"] = parent
            if (
                column_identifier is None
                or column_identifier == ""
                or row_number is None
                or row_number == ""
            ):
                raise ValueError(
                    f"('{column_identifier}','{row_number}') is not a valid index!"
                )

            if str(column_identifier).isdigit():
                column_identifier = int(column_identifier)
                if column_identifier < 1 or column_identifier > len(parent.headers):
                    raise ValueError(f"Column '{column_identifier}' does not exists!")
                self.__dict__["column_index"] = column_identifier - 1
                self.__dict__["column_label"] = parent.headers[
                    self.__dict__["column_index"]
                ]
            else:
                if not column_identifier in parent.headers:
                    raise ValueError(f"Column '{column_identifier}' does not exists!")
                self.__dict__["column_index"] = parent.headers.index(column_identifier)
                self.__dict__["column_label"] = parent.headers[
                    self.__dict__["column_index"]
                ]

            row = int(row_number)
            if len(parent.data) < row or row < 1:
                raise ValueError(f"Row '{row}' does not exists!")
            self.__dict__["row_index"] = row - 1

        def __getattr__(self, name: str) -> Any:
            row = self.__dict__["row_index"]
            column = self.__dict__["column_index"]
            label = self.__dict__["column_label"]
            if name == "formula":
                return self.__dict__["parent"].data[row][column]
            elif name == "value":
                return self.__dict__["parent"].values[row][column]
            elif name == "style":
                return self.__dict__["parent"].style[label + str(row + 1)]
            elif name == "column_header":
                return label
            elif name == "column_number":
                return column + 1
            elif name == "row_number":
                return row + 1
            else:
                raise ValueError(f"No such attribute '{name}' found!")

        def __setattr__(self, name: str, value: Any) -> None:
            row = self.__dict__["row_index"]
            column = self.__dict__["column_index"]
            label = self.__dict__["column_label"]
            if name == "formula":
                self.__dict__["parent"].data[row][column] = value
            elif name == "style":
                self.__dict__["parent"].style[label + str(column + 1)] = value
            else:
                raise ValueError(f"No such attribute '{name}' is allowed for setting!")

        def __str__(self) -> str:
            attr = self.__dict__
            row = attr["row_index"]
            column = attr["column_index"]
            return f"Cell[column={column}, row={row}, formula={attr['parent'].data[row][column]}, value={attr['parent'].values[row][column]}]"

    class ColumnBuilder:
        """Read/write accessor for a spreadsheet column's metadata.

        Obtain via :meth:`~Spreadsheet.column`.

        Attributes:
            header (str): The column label.  Can be set to rename the column.
            width (int): The column pixel width.  Can be set.
            column_number (int): The 1-based column index (read-only).

        Example:
            >>> col = sheet.column("B")
            >>> col.width = 100
            >>> col.header = "Measurement"
        """

        def __init__(
            self, parent: "Spreadsheet", column_identifier: Union[int, str]
        ) -> None:
            """Initialise a ColumnBuilder.

            Args:
                parent: The owning :class:`Spreadsheet`.
                column_identifier: Column label or 1-based index.

            Raises:
                ValueError: If the column identifier is invalid.
            """
            self.__dict__["parent"] = parent
            if column_identifier is None or column_identifier == "":
                raise ValueError(
                    f"('{column_identifier}') is not a valid column index!"
                )

            if str(column_identifier).isdigit():
                column_identifier = int(column_identifier)
                if column_identifier < 1 or column_identifier > len(parent.headers):
                    raise ValueError(f"Column '{column_identifier}' does not exists!")
                self.__dict__["column_index"] = column_identifier - 1
                self.__dict__["column_label"] = parent.headers[
                    self.__dict__["column_index"]
                ]
            else:
                if not column_identifier in parent.headers:
                    raise ValueError(f"Column '{column_identifier}' does not exists!")
                self.__dict__["column_index"] = parent.headers.index(column_identifier)
                self.__dict__["column_label"] = parent.headers[
                    self.__dict__["column_index"]
                ]

        def __getattr__(self, name: str) -> Any:
            if name == "header":
                return self.__dict__["column_label"]
            elif name == "width":
                return self.__dict__["parent"].width[self.__dict__["column_index"]]
            elif name == "column_number":
                return self.__dict__["column_index"] + 1
            else:
                raise ValueError(f"No such attribute '{name}' found!")

        def __setattr__(self, name: str, value: Any) -> None:
            if name == "header":
                self.__dict__["parent"].headers[self.__dict__["column_index"]] = value
            elif name == "width":
                self.__dict__["parent"].width[self.__dict__["column_index"]] = value
            else:
                raise ValueError(f"No such attribute '{name}' found!")

        def __str__(self) -> str:
            attr = self.__dict__
            return (
                f"Column[column={attr['column_index']}, header={attr['column_label']}]"
            )

    def __str__(self) -> str:
        return json.dumps(self.__dict__, default=lambda x: x.__dict__)

    def __repr__(self) -> str:
        return json.dumps(self.__dict__, default=lambda x: x.__dict__)

    def to_json(self) -> str:
        """Serialise the spreadsheet to a JSON string.

        Returns:
            A pretty-printed JSON string representation of the spreadsheet.
        """

        def dictionary_creator(x: Any) -> dict:
            dictionary = x.__dict__
            return dictionary

        return json.dumps(self, default=dictionary_creator, sort_keys=True, indent=4)

    @classmethod
    def from_dict(cls, data: Optional[dict]) -> Optional["Spreadsheet"]:
        """Deserialise a :class:`Spreadsheet` from a plain dictionary.

        Args:
            data: A dict whose keys match the spreadsheet annotation
                fields (``headers``, ``data``, ``values``, …).
                If ``None``, returns ``None``.

        Returns:
            A new :class:`Spreadsheet` populated from ``data``, or ``None``.
        """
        if data is None:
            return None
        result = cls(10)
        for prop in cls.__annotations__.keys():
            attribute = data.get(prop)
            result.__dict__[prop] = attribute
        return result
