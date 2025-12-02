package ch.ethz.sis.rocrateserver.openapi.v1.service.response.result;

import jakarta.annotation.Nullable;

import java.util.List;

public class AsyncResult
{
    String status;

    List<String> errors;

    Object result;

    public AsyncResult(String status, List<String> errors, @Nullable Object result)
    {
        this.status = status;
        this.errors = errors;
        this.result = result;
    }
}
