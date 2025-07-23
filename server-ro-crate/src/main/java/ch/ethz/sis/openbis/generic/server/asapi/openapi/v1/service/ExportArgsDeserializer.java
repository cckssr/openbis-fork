package ch.ethz.sis.openbis.generic.server.asapi.openapi.v1.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.HttpHeaders;

import java.io.IOException;
import java.io.InputStream;

public class ExportArgsDeserializer {
    public enum ExportOption {};

    private final HttpHeaders httpHeaders;
    private final InputStream inputStream;

    public ExportArgsDeserializer(HttpHeaders httpHeaders, InputStream inputStream) {
        this.httpHeaders = httpHeaders;
        this.inputStream = inputStream;
    }

    public String getOption(ExportOption option) {
        return null;
    }

    public String[] getIdentifiers() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(inputStream, String[].class);
    }
}
