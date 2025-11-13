import struct

def encode_chunk(chunk):
    owner_bytes = chunk.get('owner').encode('utf-8') if chunk.get('owner') is not None else None
    source_bytes = chunk.get('source').encode('utf-8') if chunk.get('source') is not None else None
    data_bytes = chunk.get('data') if chunk.get('data') is not None else None

    # Calculate packet size
    packet_size = (
        4 + (len(owner_bytes) if owner_bytes else 0) +  # owner length + bytes
        4 + (len(source_bytes) if source_bytes else 0) +  # source length + bytes
        8 +  # offset (long)
        4 +  # limit (int)
        4 + (len(data_bytes) if data_bytes else 0)  # data length + bytes
    )

    packet = bytearray(packet_size)

    # Struct format helpers:
    # >  = big-endian (Java ByteBuffer default)
    # i  = int (4 bytes)
    # q  = long (8 bytes)
    pos = 0

    def put_int(value):
        nonlocal pos
        struct.pack_into('>i', packet, pos, value)
        pos += 4

    def put_long(value):
        nonlocal pos
        struct.pack_into('>q', packet, pos, value)
        pos += 8

    def put_bytes(b):
        nonlocal pos
        packet[pos:pos + len(b)] = b
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
    put_long(chunk.get('offset', -1))

    # limit
    put_int(chunk.get('limit', -1))

    # data
    put_int(len(data_bytes) if data_bytes is not None else -1)
    if data_bytes:
        put_bytes(data_bytes)

    return bytes(packet)

def encode_chunks_as_bytes(chunks):
    """
    Encodes a list of chunk dictionaries into a single bytes object.
    Each chunk is encoded using `encode_chunk`.
    """

    # Encode each chunk individually
    positionally_encoded_chunks = [encode_chunk(chunk) for chunk in chunks]

    # Total size = 4 bytes for number of chunks + sum of encoded chunk sizes
    total_size = 4 + sum(len(chunk_bytes) for chunk_bytes in positionally_encoded_chunks)

    # Allocate a bytearray of the correct size
    packet = bytearray(total_size)
    pos = 0

    def put_int(value):
        nonlocal pos
        struct.pack_into('>i', packet, pos, value)
        pos += 4

    def put_bytes(b):
        nonlocal pos
        packet[pos:pos + len(b)] = b
        pos += len(b)

    # Write number of chunks
    put_int(len(chunks))

    # Write each encoded chunk
    for chunk_bytes in positionally_encoded_chunks:
        put_bytes(chunk_bytes)

    return bytes(packet)

def decode_chunks(chunks_as_bytes):
    """
    Decodes a bytes object representing multiple encoded chunks into a list of chunk dictionaries.
    """
    pos = 0

    def get_int():
        nonlocal pos
        value = struct.unpack_from('>i', chunks_as_bytes, pos)[0]
        pos += 4
        return value

    num_chunks = get_int()
    chunks = []

    for _ in range(num_chunks):
        chunk, new_pos = decode_chunk(chunks_as_bytes, pos)
        chunks.append(chunk)
        pos = new_pos

    return chunks


def decode_chunk(buffer, pos):
    """
    Decodes a single Chunk structure from a bytes buffer starting at position `pos`.
    Returns (chunk_dict, new_position).
    """

    def get_int():
        nonlocal pos
        value = struct.unpack_from('>i', buffer, pos)[0]
        pos += 4
        return value

    def get_long():
        nonlocal pos
        value = struct.unpack_from('>q', buffer, pos)[0]
        pos += 8
        return value

    def get_bytes(length):
        nonlocal pos
        data = buffer[pos:pos + length]
        pos += length
        return data

    # --- owner ---
    owner_len = get_int()
    owner = None
    if owner_len >= 0:
        owner = get_bytes(owner_len).decode('utf-8')

    # --- source ---
    source_len = get_int()
    source = None
    if source_len >= 0:
        source = get_bytes(source_len).decode('utf-8')

    # --- offset ---
    offset = get_long()
    if offset < 0:
        offset = None

    # --- limit ---
    limit = get_int()
    if limit < 0:
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
        "data": data
    }

    return chunk, pos