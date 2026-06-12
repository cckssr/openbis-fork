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
"""Coercion of property values (timestamps, arrays, spreadsheets) to wire format."""

import base64
import json
from datetime import datetime

import pandas as pd
from typing import Any

from .spreadsheet import Spreadsheet


def is_of_openbis_supported_date_format(value: str) -> bool:
    """Check whether the value matches one of the openBIS datetime formats."""
    is_supported = False
    for date_format in PropertyReformatter.SUPPORTED_DATETIME_FORMATS:
        try:
            datetime.strptime(value, date_format)
            is_supported = True
            break
        except ValueError:
            pass
    return is_supported


class PropertyReformatter:
    """Coerces property values into their openBIS wire format."""

    LONG_DATETIME_FORMAT = "%Y-%m-%d %H:%M:%S"

    SUPPORTED_DATETIME_FORMATS = [
        "%Y-%m-%d",
        "%y-%m-%d",  # ShortDateFormat
        "%Y-%m-%d %H:%M",
        "%y-%m-%d %H:%M",  # NormalDateFormat
        "%Y-%m-%d %H:%M:%S",
        "%y-%m-%d %H:%M:%S",  # LongDateFormat
        "%Y-%m-%d %H:%M:%S %z",
        "%y-%m-%d %H:%M:%S %z",  # LongDateFormat with timezone
    ]

    def __init__(self, openbis_obj: Any) -> None:
        """Store the Openbis connection used to look up property types."""
        self.openbis = openbis_obj

    def format(self, properties: "dict[str, Any]") -> "dict[str, Any]":
        """Coerce all property values to their wire format, in place."""
        if properties is None:
            raise ValueError("properties can not be None!")

        for key, value in properties.items():
            if value is None or value == "":
                properties[key] = None
                continue
            property_type = self.openbis.get_property_type(key)
            if property_type.dataType == "TIMESTAMP":
                if property_type.multiValue:
                    properties[key] = [self._format_timestamp(x) for x in value]
                else:
                    properties[key] = self._format_timestamp(value)
            if property_type.dataType == "SAMPLE":
                if property_type.multiValue:
                    linked: list[Any] = []
                    for sample in value:
                        if not isinstance(sample, str):
                            linked += sample.permId
                        else:
                            linked += [sample]
                    properties[key] = linked
                else:
                    if not isinstance(value, str):
                        properties[key] = value.permId
            elif property_type.dataType == "ARRAY_TIMESTAMP":
                if property_type.multiValue:
                    properties[key] = [
                        "["
                        + ",".join(map(str, [self._format_timestamp(x) for x in arr]))
                        + "]"
                        for arr in value
                    ]
                else:
                    properties[key] = [self._format_timestamp(x) for x in value]
            elif property_type.dataType.startswith("ARRAY"):
                if property_type.multiValue:
                    properties[key] = ["[" + ",".join(map(str, x)) + "]" for x in value]
            elif (
                property_type.dataType == "XML"
                and "custom_widget" in property_type.metaData
                and property_type.metaData["custom_widget"].upper() == "SPREADSHEET"
            ):
                if isinstance(value, Spreadsheet):
                    json_str = value.to_json().encode("utf-8")
                    b64 = base64.b64encode(json_str).decode("utf-8")
                    result = f"<DATA>{b64}</DATA>"
                    properties[key] = result
        return properties

    def _format_timestamp(self, value: Any) -> Any:
        if value is None:
            return value
        if is_of_openbis_supported_date_format(value):
            return value
        timestamp = pd.to_datetime(value)
        result = timestamp.strftime(PropertyReformatter.LONG_DATETIME_FORMAT)
        print(
            f'WARNING: "{value}" is not of any OpenBis supported datetime formats. Reformatting to "{result}"'
        )
        return result

    def to_array(self, data_type: str, prop_value: Any) -> "list[Any]":
        """Parse a server-side array value into a typed Python list."""
        if prop_value is None or prop_value == "":
            return []
        result: list[Any] = []
        if data_type in ("ARRAY_INTEGER", "INTEGER"):
            result = [int(x.strip()) for x in prop_value]
        elif data_type in ("ARRAY_REAL", "REAL"):
            result = [float(x.strip()) for x in prop_value]
        elif data_type == "BOOLEAN":
            result = [x.strip().lower() == "true" for x in prop_value]
        elif data_type in ("ARRAY_TIMESTAMP", "TIMESTAMP", "DATE"):
            result = [x.strip() for x in prop_value]
        else:
            result = prop_value
        return result

    def to_spreadsheet(self, rawValue: Any) -> Any:
        """Decode a ``<DATA>base64</DATA>`` value into a Spreadsheet."""
        try:
            b64 = rawValue[len("<DATA>") : -len("</DATA>")]
            jsonb = base64.b64decode(b64)
            try:
                json_str = jsonb.decode("utf-8")
            except UnicodeDecodeError as decode_error:
                json_str = jsonb.decode("latin1")
            result = json.loads(json_str)
            return Spreadsheet.from_dict(result)
        except ValueError as e:
            print(f"Could not decode spreadsheet property: {e}")
            return rawValue
