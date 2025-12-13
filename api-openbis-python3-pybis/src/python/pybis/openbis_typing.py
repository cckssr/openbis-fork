"""
Classes and type aliases for openBIS entities and data structures.
"""

from typing import Dict, List, Optional, Union, Literal
from enum import StrEnum


class EntityKindCode(StrEnum):
    DATASET = "DATA_SET"
    SAMPLE = "SAMPLE"
    EXPERIMENT = "EXPERIMENT"