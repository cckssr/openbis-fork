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

def _nonzero(num):
    if num != 0:
        return 1
    return 0

def _get_headers(count):
    """Algorithm for generating headers, maximum number of columns supported: 26*26=676"""
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

    def __init__(self, columns=10, rows=10):
        self.headers = _get_headers(columns)
        self.data = [["" for _ in range(columns)] for _ in range(rows)]
        self.style = {
                header + str(y): "text-align: center;" for header in self.headers for y in range(1, rows+1)
            }
        self.meta = {}
        self.width = [50 for _ in range(columns)]
        self.values = [["" for _ in range(columns)] for _ in range(rows)]

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
