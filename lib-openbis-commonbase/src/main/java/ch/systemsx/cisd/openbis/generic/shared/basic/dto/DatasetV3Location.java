package ch.systemsx.cisd.openbis.generic.shared.basic.dto;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;

public class DatasetV3Location implements IDatasetLocation
{

    private final DataSet dataSet;

    public DatasetV3Location(DataSet dataSet)
    {
        this.dataSet = dataSet;
    }

    @Override public String getDataSetLocation()
    {
        return dataSet.getPhysicalData().getLocation();
    }

    @Override public String getDataSetCode()
    {
        return dataSet.getCode();
    }

    @Override public String getDataSetShareId()
    {
        return dataSet.getPhysicalData().getShareId();
    }

    @Override public String getDataStoreUrl()
    {
        return dataSet.getDataStore().getDownloadUrl();
    }

    @Override public String getDataStoreCode()
    {
        return dataSet.getDataStore().getCode();
    }

    @Override public Integer getOrderInContainer(final String containerDataSetCode)
    {
        return 0;
    }

    @Override public Long getDataSetSize()
    {
        return dataSet.getPhysicalData().getSize();
    }

}
