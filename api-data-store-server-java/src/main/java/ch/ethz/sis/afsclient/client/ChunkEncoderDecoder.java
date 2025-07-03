package ch.ethz.sis.afsclient.client;

import ch.ethz.sis.afsapi.dto.Chunk;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class ChunkEncoderDecoder {

    /**
     Binary chunk encoding:     | 4-byte big-endian int32 : number of following chunks | binary-encoded chunk n. 1 | binary-encoded chunk n. 2 | ...
     Each chunk is encoded this way:   | 4-byte big-endian int32 : owner UTF-8-bytes length ( -1 if owner was null) |
     | owner UTF-8-bytes (if any) |
     | 4-byte big-endian int32 : source UTF-8-bytes length ( -1 if source was null) |
     | source UTF-8-bytes (if any) |
     | 8-byte big-endian int64 : offset (-1 if offset was null) |
     | 4-byte big-endian int32 : limit (-1 if limit was null) |
     | 4-byte big-endian int32 : data length ( -1 if data was null) |
     | data bytes (if any) |
     */


    public static byte[] EMPTY_ARRAY = new byte[0];

    public static byte[] encodeChunk(Chunk chunk) {
        byte[] ownerBytes = Optional.ofNullable(chunk.getOwner()).map(str -> str.getBytes(StandardCharsets.UTF_8)).orElse(null);
        byte[] sourceBytes = Optional.ofNullable(chunk.getSource()).map(str -> str.getBytes(StandardCharsets.UTF_8)).orElse(null);

        int packetSize = 4 /*owner length*/ + ownerBytes.length +
                    4 /*source length*/ + sourceBytes.length +
                    8 /*offset*/ +
                    4 /*limit*/ +
                    4 /*data length*/ + Optional.ofNullable(chunk.getData()).map( data -> data.length ).orElse(0);

        byte[] packet = new byte[packetSize];
        ByteBuffer packetBuffer = ByteBuffer.wrap(packet);
        packetBuffer.putInt(ownerBytes != null ? ownerBytes.length : -1);
        if (ownerBytes != null) {
            packetBuffer.put(ownerBytes);
        }
        packetBuffer.putInt(sourceBytes != null ? sourceBytes.length : -1);
        if (sourceBytes != null) {
            packetBuffer.put(sourceBytes);
        }
        packetBuffer.putLong(Optional.ofNullable(chunk.getOffset()).orElse(-1L));
        packetBuffer.putInt(Optional.ofNullable(chunk.getLimit()).orElse(-1));
        packetBuffer.putInt(chunk.getData() != null ? chunk.getData().length : -1);
        if (chunk.getData() != null) {
            packetBuffer.put(chunk.getData());
        }

        return packet;
    }

    public static byte[] encodeChunksAsBytes(Chunk[] chunks) {
        byte[][] positionallyEncodedChunks = new byte[chunks.length][];

        int i = 0;
        int totalSize = 4; /*int32 for length chunks*/
        for(Chunk chunk: chunks) {
            positionallyEncodedChunks[i] = encodeChunk(chunk);
            totalSize += positionallyEncodedChunks[i].length;
            i++;
        }

        ByteBuffer byteBuffer = ByteBuffer.allocate(totalSize);
        byteBuffer.putInt(chunks.length);
        for(byte[] positionallyEncodedChunk : positionallyEncodedChunks) {
            byteBuffer.put(positionallyEncodedChunk);
        }
        return byteBuffer.array();
    }


    public static Chunk[] decodeChunks(byte[] chunksAsBytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(chunksAsBytes);
        Chunk[] chunks = new Chunk[byteBuffer.getInt()];
        for (int i = 0; i < chunks.length; i++) {
            chunks[i] = decodeChunk(byteBuffer);
        }
        return chunks;
    }

    public static Chunk decodeChunk(ByteBuffer positionallyEncodedChunk) {
        int ownerBytesLength = positionallyEncodedChunk.getInt();
        String owner;
        if (ownerBytesLength < 0) {
            owner = null;
        } else {
            byte[] ownerBytes = new byte[ownerBytesLength];
            positionallyEncodedChunk.get(ownerBytes);
            owner = new String(ownerBytes, StandardCharsets.UTF_8);
        }

        int sourceBytesLength = positionallyEncodedChunk.getInt();
        String source;
        if (sourceBytesLength < 0) {
            source = null;
        } else {
            byte[] sourceBytes = new byte[sourceBytesLength];
            positionallyEncodedChunk.get(sourceBytes);
            source = new String(sourceBytes, StandardCharsets.UTF_8);
        }

        Long offset = positionallyEncodedChunk.getLong();
        if (offset < 0) { offset = null; }

        Integer limit = positionallyEncodedChunk.getInt();
        if (limit < 0) { limit = null; }

        int dataBytesLength = positionallyEncodedChunk.getInt();
        byte[] dataBytes;
        if (dataBytesLength < 0) {
            dataBytes = null;
        } else {
            dataBytes = new byte[dataBytesLength];
            positionallyEncodedChunk.get(dataBytes);
        }

        return new Chunk(owner, source, offset, limit, dataBytes);
    }
}
