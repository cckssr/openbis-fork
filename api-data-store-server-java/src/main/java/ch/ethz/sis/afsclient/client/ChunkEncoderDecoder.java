package ch.ethz.sis.afsclient.client;

import ch.ethz.sis.afsapi.dto.Chunk;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ChunkEncoderDecoder {

    private static String CHUNK_SEPARATOR = ",";
    private static String CHUNK_ARRAY_SEPARATOR = ";";
    public static byte[] EMPTY_ARRAY = new byte[0];

    public static String encodeChunk(Chunk chunk) {
        return new StringBuilder()
                .append(chunk.getOwner()).append(CHUNK_SEPARATOR)
                .append(chunk.getSource()).append(CHUNK_SEPARATOR)
                .append(chunk.getOffset()).append(CHUNK_SEPARATOR)
                .append(chunk.getLimit()).append(CHUNK_SEPARATOR)
                .append(Base64.getEncoder().encodeToString(chunk.getData())).toString();
    }

    public static String encodeChunks(Chunk[] chunks) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < chunks.length; i++) {
            if (i > 0) {
                builder.append(CHUNK_ARRAY_SEPARATOR);
            }
            builder.append(encodeChunk(chunks[i]));
        }
        return builder.toString();
    }

    public static byte[] encodeChunksAsBytes(Chunk[] chunks) {
        String chunksAsString = encodeChunks(chunks);
        return chunksAsString.getBytes(StandardCharsets.UTF_8);
    }

    public static Chunk decodeChunk(String chunkAsString) {
        String[] chunkParameters = chunkAsString.split(CHUNK_SEPARATOR);

        byte[] data = null;
        if (chunkParameters.length == 5) {
            data = Base64.getDecoder().decode(chunkParameters[4]);
        } else {
            data = EMPTY_ARRAY;
        }

        return new Chunk(chunkParameters[0],
                chunkParameters[1],
                Long.parseLong(chunkParameters[2]),
                Integer.parseInt(chunkParameters[3]),
                data);
    }

    public static Chunk[] decodeChunks(String chunksAsString) {
        String[] chunksParameters = chunksAsString.split(CHUNK_ARRAY_SEPARATOR);
        Chunk[] chunks = new Chunk[chunksParameters.length];
        for (int i = 0; i < chunksParameters.length; i++) {
            chunks[i] = decodeChunk(chunksParameters[i]);
        }
        return chunks;
    }

    public static Chunk[] decodeChunks(byte[] chunksAsBytes) {
        String chunksAsString = new String(chunksAsBytes, StandardCharsets.UTF_8);
        return decodeChunks(chunksAsString);
    }
}
