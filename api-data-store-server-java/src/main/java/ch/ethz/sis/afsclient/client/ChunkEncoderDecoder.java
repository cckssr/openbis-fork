package ch.ethz.sis.afsclient.client;

import ch.ethz.sis.afsapi.dto.Chunk;

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

    public static Chunk decodeChunk(String chunkAsString) {
        String[] chunkParameters = chunkAsString.split(CHUNK_SEPARATOR);
        return new Chunk(chunkParameters[0],
                chunkParameters[1],
                Long.parseLong(chunkParameters[2]),
                Integer.parseInt(chunkParameters[3]),
                Base64.getDecoder().decode(chunkParameters[4]));
    }

    public static Chunk[] decodeChunks(String chunksAsString) {
        String[] chunksParameters = chunksAsString.split(CHUNK_ARRAY_SEPARATOR);
        Chunk[] chunks = new Chunk[chunksParameters.length];
        for (int i = 0; i < chunksParameters.length; i++) {
            chunks[i] = decodeChunk(chunksParameters[i]);
        }
        return chunks;
    }
}
