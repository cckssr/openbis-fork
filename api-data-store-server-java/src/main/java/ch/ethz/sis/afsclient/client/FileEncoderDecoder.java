package ch.ethz.sis.afsclient.client;

import ch.ethz.sis.afsapi.dto.Chunk;
import ch.ethz.sis.afsapi.dto.File;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;

public class FileEncoderDecoder {

    private static String FILE_SEPARATOR = ",";
    private static String FILE_ARRAY_SEPARATOR = ";";
    private static final File[] EMPTY_FILE_ARRAY = new File[0];

    public static String encodeFile(File file) {
        return new StringBuilder()
                .append(file.getOwner()).append(FILE_SEPARATOR)
                .append(file.getPath()).append(FILE_SEPARATOR)
                .append(file.getName()).append(FILE_SEPARATOR)
                .append(convertNullToEmptyStringOrElseToString(file.getDirectory())).append(FILE_SEPARATOR)
                .append(convertNullToEmptyStringOrElseToString(file.getSize())).append(FILE_SEPARATOR)
                .append(file.getLastModifiedTime() != null ? file.getLastModifiedTime().toInstant().toEpochMilli() : "").toString();
    }

    public static String encodeFiles(File[] files) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < files.length; i++) {
            if (i > 0) {
                builder.append(FILE_ARRAY_SEPARATOR);
            }
            builder.append(encodeFile(files[i]));
        }
        return builder.toString();
    }

    public static byte[] encodeFilesAsBytes(File[] files) {
        String filesAsString = encodeFiles(files);
        return filesAsString.getBytes(StandardCharsets.UTF_8);
    }

    public static File decodeFile(String fileAsString) {
        String[] fileParameters = fileAsString.split(FILE_SEPARATOR, -1);

        Boolean directory = !fileParameters[3].isEmpty() ? Boolean.parseBoolean(fileParameters[3]) : null;
        Long size = !fileParameters[4].isEmpty() ? Long.parseLong(fileParameters[4]) : null;
        OffsetDateTime lastModifiedTime = !fileParameters[5].isEmpty() ?
                Instant.ofEpochMilli(Long.parseLong(fileParameters[5])).atOffset(ZoneOffset.UTC) : null;

        return new File(fileParameters[0], fileParameters[1], fileParameters[2], directory, size, lastModifiedTime);
    }

    public static File[] decodeFiles(String filesAsString) {
        if (!filesAsString.isEmpty()) {
            String[] filesParameters = filesAsString.split(FILE_ARRAY_SEPARATOR, -1);
            File[] files = new File[filesParameters.length];
            for (int i = 0; i < filesParameters.length; i++) {
                files[i] = decodeFile(filesParameters[i]);
            }
            return files;
        } else {
            return EMPTY_FILE_ARRAY;
        }
    }

    public static File[] decodeFiles(byte[] filesAsBytes) {
        String filesAsString = new String(filesAsBytes, StandardCharsets.UTF_8);
        return decodeFiles(filesAsString);
    }

    private static String convertNullToEmptyStringOrElseToString(Object object) {
        return object != null ? object.toString() : "";
    }
}
