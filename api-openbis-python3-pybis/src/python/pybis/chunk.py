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
"""Binary (de)serialization of AFS transfer chunks (Java ByteBuffer layout)."""

import struct
from typing import Any, Optional


def encode_chunk(chunk: "dict[str, Any]") -> bytes:
    """Encode one chunk dict into its binary wire format."""
    owner = chunk.get("owner")
    owner_bytes = owner.encode("utf-8") if owner is not None else None
    source = chunk.get("source")
    source_bytes = source.encode("utf-8") if source is not None else None
    data_bytes = chunk.get("data") if chunk.get("data") is not None else None

    # Calculate packet size
    packet_size = (
        4
        + (len(owner_bytes) if owner_bytes else 0)  # owner length + bytes
        + 4
        + (len(source_bytes) if source_bytes else 0)  # source length + bytes
        + 8  # offset (long)
        + 4  # limit (int)
        + 4
        + (len(data_bytes) if data_bytes else 0)  # data length + bytes
    )

    packet = bytearray(packet_size)

    # Struct format helpers:
    # >  = big-endian (Java ByteBuffer default)
    # i  = int (4 bytes)
    # q  = long (8 bytes)
    pos = 0

    def put_int(value: int) -> None:
        nonlocal pos
        struct.pack_into(">i", packet, pos, value)
        pos += 4

    def put_long(value: int) -> None:
        nonlocal pos
        struct.pack_into(">q", packet, pos, value)
        pos += 8

    def put_bytes(b: bytes) -> None:
        nonlocal pos
        packet[pos : pos + len(b)] = b
        pos += len(b)

    # owner
    put_int(len(owner_bytes) if owner_bytes is not None else -1)
    if owner_bytes:
        put_bytes(owner_bytes)

    # source
    put_int(len(source_bytes) if source_bytes is not None else -1)
    if source_bytes:
        put_bytes(source_bytes)

    # offset
    put_long(chunk.get("offset", -1))

    # limit
    put_int(chunk.get("limit", -1))

    # data
    put_int(len(data_bytes) if data_bytes is not None else -1)
    if data_bytes:
        put_bytes(data_bytes)

    return bytes(packet)


def encode_chunks_as_bytes(chunks: "list[dict[str, Any]]") -> bytes:
    """Encode a list of chunk dictionaries into a single bytes object.

    Each chunk is encoded using ``encode_chunk``.
    """
    # Encode each chunk individually
    positionally_encoded_chunks = [encode_chunk(chunk) for chunk in chunks]

    # Total size = 4 bytes for number of chunks + sum of encoded chunk sizes
    total_size = 4 + sum(
        len(chunk_bytes) for chunk_bytes in positionally_encoded_chunks
    )

    # Allocate a bytearray of the correct size
    packet = bytearray(total_size)
    pos = 0

    def put_int(value: int) -> None:
        nonlocal pos
        struct.pack_into(">i", packet, pos, value)
        pos += 4

    def put_bytes(b: bytes) -> None:
        nonlocal pos
        packet[pos : pos + len(b)] = b
        pos += len(b)

    # Write number of chunks
    put_int(len(chunks))

    # Write each encoded chunk
    for chunk_bytes in positionally_encoded_chunks:
        put_bytes(chunk_bytes)

    return bytes(packet)


def decode_chunks(chunks_as_bytes: bytes) -> "list[dict[str, Any]]":
    """Decode a bytes object of encoded chunks into a list of chunk dicts."""
    pos = 0

    def get_int() -> int:
        nonlocal pos
        value: int = struct.unpack_from(">i", chunks_as_bytes, pos)[0]
        pos += 4
        return value

    num_chunks = get_int()
    chunks = []

    for _ in range(num_chunks):
        chunk, new_pos = decode_chunk(chunks_as_bytes, pos)
        chunks.append(chunk)
        pos = new_pos

    return chunks


def decode_chunk(buffer: bytes, pos: int) -> "tuple[dict[str, Any], int]":
    """Decode one chunk from ``buffer`` starting at ``pos``.

    Returns:
        A ``(chunk_dict, new_position)`` tuple.
    """

    def get_int() -> int:
        nonlocal pos
        value: int = struct.unpack_from(">i", buffer, pos)[0]
        pos += 4
        return value

    def get_long() -> int:
        nonlocal pos
        value: int = struct.unpack_from(">q", buffer, pos)[0]
        pos += 8
        return value

    def get_bytes(length: int) -> bytes:
        nonlocal pos
        data = buffer[pos : pos + length]
        pos += length
        return data

    # --- owner ---
    owner_len = get_int()
    owner = None
    if owner_len >= 0:
        owner = get_bytes(owner_len).decode("utf-8")

    # --- source ---
    source_len = get_int()
    source = None
    if source_len >= 0:
        source = get_bytes(source_len).decode("utf-8")

    # --- offset ---
    offset: Optional[int] = get_long()
    if offset is not None and offset < 0:
        offset = None

    # --- limit ---
    limit: Optional[int] = get_int()
    if limit is not None and limit < 0:
        limit = None

    # --- data ---
    data_len = get_int()
    data = None
    if data_len >= 0:
        data = get_bytes(data_len)

    chunk = {
        "owner": owner,
        "source": source,
        "offset": offset,
        "limit": limit,
        "data": data,
    }

    return chunk, pos
