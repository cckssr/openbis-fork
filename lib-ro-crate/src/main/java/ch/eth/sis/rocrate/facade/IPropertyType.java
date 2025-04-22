package ch.eth.sis.rocrate.facade;

import java.util.List;

public interface IPropertyType
{
    /* Returns the ID of this property type */
    String getId();

    /* Return possible values for the subject of this property type */
    List<IType> getDomain();

    /* Return possible values for the object of this property type */
    List<String> getRange();

    /* Returns the ontological annotations of this property type */
    List<String> getOntologicalAnnotations();

    /* Returns whether this property has a min cardinality. 0 means optional, 1 means mandatory. */
    int getMinCardinality();

    /* Returns whether this property has a max cardinality. 0 means many values possible, 1 means only one is possible. */
    int getMaxCardinality();

    /* Returns a human-readable description of this type */
    String getComment();

    /* Returns a human-readable label of this type */
    String getLabel();

}
