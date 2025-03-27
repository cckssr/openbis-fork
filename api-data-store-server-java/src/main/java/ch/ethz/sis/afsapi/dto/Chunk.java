package ch.ethz.sis.afsapi.dto;

import lombok.*;

import java.io.Serializable;

@Value
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class Chunk implements DTO, Serializable
{
    private String owner;
    private String source;
    private Long offset;
    private Integer limit;
    private byte[] data;
}
