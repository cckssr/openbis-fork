package ch.eth.sis.rocrate.facade;

import java.util.List;

public interface ISchemaFacade
{

    /* Adds a single class */
    void addRdfsClass(IRdfsClass rdfsClass);

    /** Retrieves all Classes */
    List<IRdfsClass> getRdfsClasses();

    /* Get a single type by its ID */
    IRdfsClass getRdfsClass(String id);

    /* Adds a single property */
    void addRfsProperty(IRdfsProperty property);

    /* Get all Properties */
    List<IRdfsProperty> getRdfsProperties();

    /* Gets a single property by its ID. */
    IRdfsProperty getRdfsProperty(String id);

    /* Add a single metadata entry */
    void addEntry(IMetadataEntry entry);

    /* Get a single metadata entry by its ID */
    IMetadataEntry getEntry(String id);

    /* Get all metadata entities */
    List<IMetadataEntry> getEntries(String rdfsClassId);

}
