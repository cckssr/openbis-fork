package ch.eth.sis.rocrate.facade;

import java.util.List;

public interface IRdfsProperty
{
    /* Returns the ID of this property */
    String getId();

    /* Return possible values for the subject of this property */
    List<String> getDomain();

    /* Return possible values for the object of this property */
    List<String> getRange();

    /* Returns the ontological annotations of this class */
    List<String> getOntologicalAnnotations();

}
