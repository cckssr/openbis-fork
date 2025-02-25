package ch.ethz.sis.afsapi.dto;

import lombok.*;

@Value
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class Chunk {
    private String owner;
    private String source;
    private Long offset;
    private Integer limit;
    private byte[] data;
}
