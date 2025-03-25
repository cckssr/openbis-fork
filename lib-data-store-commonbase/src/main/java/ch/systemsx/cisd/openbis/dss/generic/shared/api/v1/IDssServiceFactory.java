package ch.systemsx.cisd.openbis.dss.generic.shared.api.v1;

public interface IDssServiceFactory
{

    public IDssService getService(String baseURL);

}
