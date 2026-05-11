package ch.ethz.sis.afsserver.server.impl;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class OperationResult implements ch.ethz.sis.afsserver.server.OperationResult
{
    private final Object result;
    private final Throwable exception;
}
