package ch.eth.sis.rocrate.facade;


import java.util.List;

public interface IRdfsClass
{
    /* Returns the ID of this class */
    String getId();

    /* Returns classes this class inherits from */
    List<String> getSuperClasses();

    /* Returns the ontological annotations of this class */
    List<String> getOntologicalAnnotations();

    void addProperty(RdfsProperty rdfsProperty);

}
