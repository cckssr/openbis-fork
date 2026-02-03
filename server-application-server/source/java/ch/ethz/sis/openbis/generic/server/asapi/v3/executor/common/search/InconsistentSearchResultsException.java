package ch.ethz.sis.openbis.generic.server.asapi.v3.executor.common.search;

public class InconsistentSearchResultsException extends RuntimeException
{

    public InconsistentSearchResultsException(int pagedResultSize, int pagedResultV3DTOsSize)
    {
        super(String.format("Number of results after translation has changed. "
                        + "Total count value will be incorrect. "
                        + "[pagedResult.size()=%d, pagedResultV3DTOs.size()=%d]",
                pagedResultSize, pagedResultV3DTOsSize));
    }

}
