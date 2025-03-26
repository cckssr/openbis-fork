#   Copyright ETH 2025 Zürich, Scientific IT Services
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
from pandas.testing import assert_frame_equal
from pandas import DataFrame
import pytest
import base64
from pybis.spreadsheet import Spreadsheet
from pybis.property_reformatter import PropertyReformatter


# base64 encoded Spreadsheet with special characters encoded with latin-1 encoding
SPREADSHEET_LATIN1 = '<DATA>eyJoZWFkZXJzIjpbIkEiLCJCIiwiQyIsIkQiLCJFIiwiRiIsIkciLCJIIiwiSSIsIkoiXSwiZGF0YSI6W1siIiwidXNpbmcgsCBvciB1bWxhdXRzICjkLPYs/CkiLCIiLCIiLCIiLCIiLCIiLCIiLCIiLCIiXSxbIiIsIklmIEdlcm1hbiB1bWxhdXRzLCDfIG9yILAgb2NjdXIsIHRoZSBkZWNvZGluZyBmYWlscy4iLCIiLCIiLCIiLCIiLCIiLCIiLCIiLCIiXSxbIiIsImEiLCIiLCIiLCIiLCIiLCIiLCIiLCIiLCIiXSxbIiIsIiIsImEiLCIiLCIiLCIiLCIiLCIiLCIiLCIiXSxbIiIsIiIsImIiLCJhIiwiIiwiIiwiIiwiIiwiIiwiIl0sWyIiLCIiLCIiLCIiLCIiLCIiLCIiLCIiLCIiLCIiXSxbIiIsIiIsIiIsIiIsIiIsIiIsIiIsIiIsIiIsIiJdLFsiIiwiIiwiIiwiIiwiIiwiIiwiIiwiIiwiIiwiIl0sWyIiLCIiLCIiLCIiLCIiLCIiLCIiLCIiLCIiLCIiXSxbIiIsIiIsIiIsIiIsIiIsIiIsIiIsIiIsIiIsIiJdXSwic3R5bGUiOnsiQTEiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IG92ZXJmbG93OiBoaWRkZW47IiwiQjEiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiQzEiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiRDEiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiRTEiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiRjEiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiRzEiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiSDEiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiSTEiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiSjEiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiQTIiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IG92ZXJmbG93OiBoaWRkZW47IiwiQjIiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiQzIiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiRDIiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiRTIiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiRjIiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiRzIiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiSDIiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiSTIiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiSjIiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiQTMiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IG92ZXJmbG93OiBoaWRkZW47IiwiQjMiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiQzMiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiRDMiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiRTMiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiRjMiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiRzMiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiSDMiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiSTMiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiSjMiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiQTQiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiQjQiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IG92ZXJmbG93OiBoaWRkZW47IiwiQzQiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiRDQiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiRTQiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiRjQiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiRzQiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiSDQiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiSTQiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiSjQiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiQTUiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiQjUiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IG92ZXJmbG93OiBoaWRkZW47IiwiQzUiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IG92ZXJmbG93OiBoaWRkZW47IiwiRDUiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiRTUiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiRjUiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiRzUiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiSDUiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiSTUiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiSjUiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiQTYiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiQjYiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiQzYiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiRDYiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiRTYiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiRjYiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiRzYiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiSDYiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiSTYiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiSjYiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiQTciOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiQjciOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiQzciOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiRDciOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiRTciOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiRjciOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiRzciOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiSDciOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiSTciOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiSjciOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiQTgiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiQjgiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiQzgiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiRDgiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiRTgiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiRjgiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiRzgiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiSDgiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiSTgiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiSjgiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiQTkiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiQjkiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiQzkiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiRDkiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiRTkiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiRjkiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiRzkiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiSDkiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiSTkiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiSjkiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiQTEwIjoidGV4dC1hbGlnbjogY2VudGVyOyIsIkIxMCI6InRleHQtYWxpZ246IGNlbnRlcjsiLCJDMTAiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiRDEwIjoidGV4dC1hbGlnbjogY2VudGVyOyIsIkUxMCI6InRleHQtYWxpZ246IGNlbnRlcjsiLCJGMTAiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiRzEwIjoidGV4dC1hbGlnbjogY2VudGVyOyIsIkgxMCI6InRleHQtYWxpZ246IGNlbnRlcjsiLCJJMTAiOiJ0ZXh0LWFsaWduOiBjZW50ZXI7IiwiSjEwIjoidGV4dC1hbGlnbjogY2VudGVyOyJ9LCJtZXRhIjpudWxsLCJ3aWR0aCI6WzUwLCIyNjMiLDUwLDUwLDUwLDUwLDUwLDUwLDUwLDUwXSwidmFsdWVzIjpbWyIiLCJ1c2luZyCwIG9yIHVtbGF1dHMgKOQs9iz8KSIsIiIsIiIsIiIsIiIsIiIsIiIsIiIsIiJdLFsiIiwiSWYgR2VybWFuIHVtbGF1dHMsIN8gb3IgsCBvY2N1ciwgdGhlIGRlY29kaW5nIGZhaWxzLiIsIiIsIiIsIiIsIiIsIiIsIiIsIiIsIiJdLFsiIiwiYSIsIiIsIiIsIiIsIiIsIiIsIiIsIiIsIiJdLFsiIiwiIiwiYSIsIiIsIiIsIiIsIiIsIiIsIiIsIiJdLFsiIiwiIiwiYiIsImEiLCIiLCIiLCIiLCIiLCIiLCIiXSxbIiIsIiIsIiIsIiIsIiIsIiIsIiIsIiIsIiIsIiJdLFsiIiwiIiwiIiwiIiwiIiwiIiwiIiwiIiwiIiwiIl0sWyIiLCIiLCIiLCIiLCIiLCIiLCIiLCIiLCIiLCIiXSxbIiIsIiIsIiIsIiIsIiIsIiIsIiIsIiIsIiIsIiJdLFsiIiwiIiwiIiwiIiwiIiwiIiwiIiwiIiwiIiwiIl1dfQo=</DATA>'

def test_create_simple_spreadsheet():

    spreadsheet = Spreadsheet(5, 5)
    for i in range(5):
        spreadsheet.data[i][i] = i+1
        spreadsheet.values[i][i] = i - 1

    assert spreadsheet.width == [50,50,50,50,50]
    assert len(spreadsheet.style) == 25
    assert spreadsheet.headers == ['A', 'B', 'C', 'D', 'E']
    for i in range(5):
        assert spreadsheet[spreadsheet.headers[i], i+1].formula == i+1
        assert spreadsheet[spreadsheet.headers[i], i+1].value == i-1

def test_add_row():
    spreadsheet = Spreadsheet(3, 3)
    for i in range(3):
        for j in range(3):
            spreadsheet.data[i][j] = (i+1)*(j+1)
            spreadsheet.values[i][j] = (i+1)*(j+1)

    assert len(spreadsheet.data) == 3
    assert len(spreadsheet.values) == 3
    assert len(spreadsheet.style) == 9

    spreadsheet.add_row()

    assert len(spreadsheet.data) == 4
    assert len(spreadsheet.values) == 4
    assert len(spreadsheet.style) == 12

    for i in range(3):
        for j in range(3):
            assert spreadsheet[i+1, j+1].formula == (i+1)*(j+1)
            assert spreadsheet[i+1, j+1].value == (i+1)*(j+1)

def test_delete_row_first():
    spreadsheet = Spreadsheet(5, 5)

    for i in range(5):
        for j in range(5):
            spreadsheet.data[i][j] = (i + 1) * (j + 1)
            spreadsheet.values[i][j] = (i + 1) * (j + 1)

    columns = ['A', 'B', 'C', 'D', 'E']
    for i in columns:
        spreadsheet.style[i + '2'] = i

    assert len(spreadsheet.style) == 25
    assert len(spreadsheet.data) == 5
    assert len(spreadsheet.values) == 5

    spreadsheet.delete_row(1)

    assert len(spreadsheet.data) == 4
    assert len(spreadsheet.values) == 4
    assert len(spreadsheet.style) == 20

    for i in range(5):
        for j in range(4):
            assert spreadsheet[i + 1, j+1].formula == (i + 1) * (j+2)
            assert spreadsheet[i + 1, j+1].value == (i + 1) * (j+2)

    for i in range(5):
        assert spreadsheet[columns[i], 1].style == columns[i]


def test_delete_row_last():
    spreadsheet = Spreadsheet(5, 5)

    for i in range(5):
        for j in range(5):
            spreadsheet.data[i][j] = (i + 1) * (j + 1)
            spreadsheet.values[i][j] = (i + 1) * (j + 1)

    columns = ['A', 'B', 'C', 'D', 'E']
    for i in columns:
        spreadsheet.style[i + '4'] = i

    assert len(spreadsheet.style) == 25
    assert len(spreadsheet.data) == 5
    assert len(spreadsheet.values) == 5

    spreadsheet.delete_row(5)

    assert len(spreadsheet.data) == 4
    assert len(spreadsheet.values) == 4
    assert len(spreadsheet.style) == 20

    for i in range(5):
        for j in range(4):
            assert spreadsheet[i + 1, j + 1].formula == (i + 1) * (j + 1)
            assert spreadsheet[i + 1, j + 1].value == (i + 1) * (j + 1)

    for i in range(5):
        assert spreadsheet[columns[i], 4].style == columns[i]


def test_delete_row_middle():
    spreadsheet = Spreadsheet(5, 5)

    for i in range(5):
        for j in range(5):
            spreadsheet.data[i][j] = (i + 1) * (j + 1)
            spreadsheet.values[i][j] = (i + 1) * (j + 1)

    columns = ['A', 'B', 'C', 'D', 'E']
    for i in columns:
        spreadsheet.style[i + '4'] = i
        spreadsheet.style[i + '2'] = i

    assert len(spreadsheet.style) == 25
    assert len(spreadsheet.data) == 5
    assert len(spreadsheet.values) == 5

    spreadsheet.delete_row(3)

    assert len(spreadsheet.data) == 4
    assert len(spreadsheet.values) == 4
    assert len(spreadsheet.style) == 20

    for i in range(5):
        for j in range(2):
            assert spreadsheet[i + 1, j + 1].formula == (i + 1) * (j + 1)
            assert spreadsheet[i + 1, j + 1].value == (i + 1) * (j + 1)
        for j in range(3,5,1):
            assert spreadsheet[i + 1, j].formula == (i + 1) * (j + 1)
            assert spreadsheet[i + 1, j].value == (i + 1) * (j + 1)

    for i in range(5):
        assert spreadsheet[columns[i], 2].style == columns[i]
        assert spreadsheet[columns[i], 3].style == columns[i]

def test_add_column_no_name():
    spreadsheet = Spreadsheet(5, 5)

    for i in range(5):
        for j in range(5):
            spreadsheet.data[i][j] = (i + 1) * (j + 1)
            spreadsheet.values[i][j] = (i + 1) * (j + 1)

    columns = ['A', 'B', 'C', 'D', 'E']
    for i in columns:
        spreadsheet.style[i + '4'] = i
        spreadsheet.style[i + '2'] = i

    spreadsheet.width[4] = 123

    assert len(spreadsheet.style) == 25
    assert len(spreadsheet.data) == 5
    assert len(spreadsheet.values) == 5

    spreadsheet.add_column()

    assert len(spreadsheet.style) == 30
    assert len(spreadsheet.headers) == 6
    assert len(spreadsheet.width) == 6
    assert len(spreadsheet.data) == 5
    assert len(spreadsheet.values) == 5

    for i in range(5):
        assert len(spreadsheet.data[i]) == 6
        assert len(spreadsheet.values[i]) == 6

    assert spreadsheet.headers == ['A', 'B', 'C', 'D', 'E', 'F']
    assert spreadsheet.width == [50, 50, 50, 50, 123, 50]

    for i in range(5):
        for j in range(5):
            assert spreadsheet[i + 1, j + 1].formula == (i + 1) * (j + 1)
            assert spreadsheet[i + 1, j + 1].value == (i + 1) * (j + 1)
        assert spreadsheet[6, i+1].formula == ''
        assert spreadsheet[6, i+1].value == ''
        assert spreadsheet[6, i+1].style == 'text-align: center;'

def test_add_column_with_name():
    spreadsheet = Spreadsheet(5, 5)

    for i in range(5):
        for j in range(5):
            spreadsheet.data[i][j] = (i + 1) * (j + 1)
            spreadsheet.values[i][j] = (i + 1) * (j + 1)

    columns = ['A', 'B', 'C', 'D', 'E']
    for i in columns:
        spreadsheet.style[i + '4'] = i
        spreadsheet.style[i + '2'] = i

    spreadsheet.width[4] = 123

    assert len(spreadsheet.style) == 25
    assert len(spreadsheet.data) == 5
    assert len(spreadsheet.values) == 5

    spreadsheet.add_column('OPENBIS')

    assert len(spreadsheet.style) == 30
    assert len(spreadsheet.headers) == 6
    assert len(spreadsheet.width) == 6
    assert len(spreadsheet.data) == 5
    assert len(spreadsheet.values) == 5

    for i in range(5):
        assert len(spreadsheet.data[i]) == 6
        assert len(spreadsheet.values[i]) == 6

    assert spreadsheet.headers == ['A', 'B', 'C', 'D', 'E', 'OPENBIS']
    assert spreadsheet.width == [50, 50, 50, 50, 123, 50]

    for i in columns:
        assert spreadsheet.style[i + '4'] == i
        assert spreadsheet.style[i + '2'] == i

    for i in range(5):
        for j in range(5):
            assert spreadsheet[j + 1, i + 1].formula == (i + 1) * (j + 1)
            assert spreadsheet[j + 1, i + 1].value == (i + 1) * (j + 1)
        assert spreadsheet['OPENBIS', i+1].formula == ''
        assert spreadsheet['OPENBIS', i+1].value == ''
        assert spreadsheet['OPENBIS', i+1].style == 'text-align: center;'

def test_delete_column_first_digit():
    spreadsheet = Spreadsheet(5, 5)

    for i in range(5):
        for j in range(5):
            spreadsheet.data[i][j] = (i + 1) * (j + 1)
            spreadsheet.values[i][j] = (i + 1) * (j + 1)

    columns = ['A', 'B', 'C', 'D', 'E']
    for i in columns:
        spreadsheet.style[i + '1'] = i
        spreadsheet.style[i + '2'] = i

    spreadsheet.width[1] = 123

    assert len(spreadsheet.style) == 25
    assert len(spreadsheet.data) == 5
    assert len(spreadsheet.values) == 5
    assert len(spreadsheet.width) == 5
    assert len(spreadsheet.headers) == 5

    spreadsheet.delete_column(1)

    assert len(spreadsheet.style) == 20
    assert len(spreadsheet.data) == 5
    assert len(spreadsheet.values) == 5
    assert spreadsheet.width == [123, 50, 50, 50]
    assert spreadsheet.headers == ['B', 'C', 'D', 'E']

    for j in range(4):
        for i in range(5):
            assert spreadsheet[j + 1, i + 1].formula == (i + 1) * (j + 2)
            assert spreadsheet[j + 1, i + 1].value == (i + 1) * (j + 2)

    for i in ['B', 'C', 'D', 'E']:
        assert spreadsheet[i, 1].style == i
        assert spreadsheet[i, 2].style == i


def test_delete_column_first_column_name():
    spreadsheet = Spreadsheet(5, 5)

    for i in range(5):
        for j in range(5):
            spreadsheet.data[i][j] = (i + 1) * (j + 1)
            spreadsheet.values[i][j] = (i + 1) * (j + 1)

    columns = ['A', 'B', 'C', 'D', 'E']
    for i in columns:
        spreadsheet.style[i + '1'] = i
        spreadsheet.style[i + '2'] = i

    spreadsheet.width[1] = 123

    assert len(spreadsheet.style) == 25
    assert len(spreadsheet.data) == 5
    assert len(spreadsheet.values) == 5
    assert len(spreadsheet.width) == 5
    assert len(spreadsheet.headers) == 5

    spreadsheet.delete_column('A')

    assert len(spreadsheet.style) == 20
    assert len(spreadsheet.data) == 5
    assert len(spreadsheet.values) == 5
    assert spreadsheet.width == [123, 50, 50, 50]
    assert spreadsheet.headers == ['B', 'C', 'D', 'E']

    for i in range(5):
        for j in range(4):
            assert spreadsheet[j + 1, i + 1].formula == (i + 1) * (j + 2)
            assert spreadsheet[j + 1, i + 1].value == (i + 1) * (j + 2)

    for i in ['B', 'C', 'D', 'E']:
        assert spreadsheet[i, 1].style == i
        assert spreadsheet[i, 2].style == i

def test_delete_column_last_digit():
    spreadsheet = Spreadsheet(5, 5)

    for i in range(5):
        for j in range(5):
            spreadsheet.data[i][j] = (i + 1) * (j + 1)
            spreadsheet.values[i][j] = (i + 1) * (j + 1)

    columns = ['A', 'B', 'C', 'D', 'E']
    for i in columns:
        spreadsheet.style[i + '1'] = i
        spreadsheet.style[i + '2'] = i

    spreadsheet.width[4] = 123
    spreadsheet.width[3] = 321

    assert len(spreadsheet.style) == 25
    assert len(spreadsheet.data) == 5
    assert len(spreadsheet.values) == 5
    assert len(spreadsheet.width) == 5
    assert len(spreadsheet.headers) == 5

    spreadsheet.delete_column(5)

    assert len(spreadsheet.style) == 20
    assert len(spreadsheet.data) == 5
    assert len(spreadsheet.values) == 5
    assert spreadsheet.width == [50, 50, 50, 321]
    assert spreadsheet.headers == ['A', 'B', 'C', 'D']

    for i in range(5):
        for j in range(4):
            assert spreadsheet[j + 1, i + 1].formula == (i + 1) * (j + 1)
            assert spreadsheet[j + 1, i + 1].value == (i + 1) * (j + 1)

    for i in ['A', 'B', 'C', 'D']:
        assert spreadsheet[i, 1].style == i
        assert spreadsheet[i, 2].style == i

def test_delete_column_last_name():
    spreadsheet = Spreadsheet(5, 5)

    for i in range(5):
        for j in range(5):
            spreadsheet.data[i][j] = (i + 1) * (j + 1)
            spreadsheet.values[i][j] = (i + 1) * (j + 1)

    columns = ['A', 'B', 'C', 'D', 'E']
    for i in columns:
        spreadsheet.style[i + '1'] = i
        spreadsheet.style[i + '2'] = i

    spreadsheet.width[4] = 123
    spreadsheet.width[3] = 321

    assert len(spreadsheet.style) == 25
    assert len(spreadsheet.data) == 5
    assert len(spreadsheet.values) == 5
    assert len(spreadsheet.width) == 5
    assert len(spreadsheet.headers) == 5

    spreadsheet.delete_column('E')

    assert len(spreadsheet.style) == 20
    assert len(spreadsheet.data) == 5
    assert len(spreadsheet.values) == 5
    assert spreadsheet.width == [50, 50, 50, 321]
    assert spreadsheet.headers == ['A', 'B', 'C', 'D']

    for i in range(5):
        for j in range(4):
            assert spreadsheet[j + 1, i + 1].formula == (i + 1) * (j + 1)
            assert spreadsheet[j + 1, i + 1].value == (i + 1) * (j + 1)

    for i in ['A', 'B', 'C', 'D']:
        assert spreadsheet[i, 1].style == i
        assert spreadsheet[i, 2].style == i

def test_delete_column_middle_digit():
    spreadsheet = Spreadsheet(5, 5)

    for i in range(5):
        for j in range(5):
            spreadsheet.data[i][j] = (i + 1) * (j + 1)
            spreadsheet.values[i][j] = (i + 1) * (j + 1)

    columns = ['A', 'B', 'C', 'D', 'E']
    for i in columns:
        spreadsheet.style[i + '1'] = i
        spreadsheet.style[i + '2'] = i

    spreadsheet.width[2] = 222
    spreadsheet.width[1] = 123
    spreadsheet.width[3] = 321

    assert len(spreadsheet.style) == 25
    assert len(spreadsheet.data) == 5
    assert len(spreadsheet.values) == 5
    assert len(spreadsheet.width) == 5
    assert len(spreadsheet.headers) == 5

    spreadsheet.delete_column(3)

    assert len(spreadsheet.style) == 20
    assert len(spreadsheet.data) == 5
    assert len(spreadsheet.values) == 5
    assert spreadsheet.width == [50, 123, 321, 50]
    assert spreadsheet.headers == ['A', 'B', 'D', 'E']

    for i in range(5):
        for j in [0, 1]:
            assert spreadsheet[j + 1, i + 1].formula == (i + 1) * (j + 1)
            assert spreadsheet[j + 1, i + 1].value == (i + 1) * (j + 1)
        for j in [3, 4]:
            assert spreadsheet[j, i + 1].formula == (i + 1) * (j + 1)
            assert spreadsheet[j, i + 1].value == (i + 1) * (j + 1)

    for i in ['A', 'B', 'D', 'E']:
        assert spreadsheet[i, 1].style == i
        assert spreadsheet[i, 2].style == i

def test_delete_column_middle_name():
    spreadsheet = Spreadsheet(5, 5)

    for i in range(5):
        for j in range(5):
            spreadsheet.data[i][j] = (i + 1) * (j + 1)
            spreadsheet.values[i][j] = (i + 1) * (j + 1)

    columns = ['A', 'B', 'C', 'D', 'E']
    for i in columns:
        spreadsheet.style[i + '1'] = i
        spreadsheet.style[i + '2'] = i

    spreadsheet.width[2] = 222
    spreadsheet.width[1] = 123
    spreadsheet.width[3] = 321

    assert len(spreadsheet.style) == 25
    assert len(spreadsheet.data) == 5
    assert len(spreadsheet.values) == 5
    assert len(spreadsheet.width) == 5
    assert len(spreadsheet.headers) == 5

    spreadsheet.delete_column('C')

    assert len(spreadsheet.style) == 20
    assert len(spreadsheet.data) == 5
    assert len(spreadsheet.values) == 5
    assert spreadsheet.width == [50, 123, 321, 50]
    assert spreadsheet.headers == ['A', 'B', 'D', 'E']

    for i in range(5):
        for j in [0, 1]:
            assert spreadsheet[j + 1, i + 1].formula == (i + 1) * (j + 1)
            assert spreadsheet[j + 1, i + 1].value == (i + 1) * (j + 1)
        for j in [3, 4]:
            assert spreadsheet[j, i + 1].formula == (i + 1) * (j + 1)
            assert spreadsheet[j, i + 1].value == (i + 1) * (j + 1)

    for i in ['A', 'B', 'D', 'E']:
        assert spreadsheet[i, 1].style == i
        assert spreadsheet[i, 2].style == i

def test_get_formulas():
    spreadsheet = Spreadsheet(5, 5)

    for i in range(5):
        for j in range(5):
            spreadsheet.data[i][j] = (i + 1) * (j + 1)
            spreadsheet.values[i][j] = (i + 1) * (j + 1)

    # get_formula returns deep copy
    formulas = spreadsheet.get_formulas()
    formulas[0][0] = 0

    assert spreadsheet[1, 1].formula == 1
    assert formulas[0][0] == 0

def test_get_df():
    spreadsheet = Spreadsheet(3, 3)

    for i in range(3):
        for j in range(3):
            spreadsheet.data[i][j] = (i + 1) * (j + 1)
            spreadsheet.values[i][j] = (i + 1) * (j + 1)

    # get_formula returns deep copy
    df = spreadsheet.df('formulas')

    expected_df = DataFrame([[1,2,3],[2,4,6],[3,6,9]], columns=['A', 'B', 'C'], index=[1,2,3])
    assert_frame_equal(expected_df, df)

def test_deserialize_latin1():
    reformatter = PropertyReformatter(None)

    spreadsheet = reformatter.to_spreadsheet(SPREADSHEET_LATIN1)

    assert spreadsheet.cell(2, 1).formula == 'using ° or umlauts (ä,ö,ü)'
