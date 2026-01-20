package ch.ethz.sis.rocrateserver.openapi.v1.service.response.result;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public interface IResultPayload
{
}
