package ch.ethz.sis.openbis.generic.server.xls.importer.helper.semanticannotation;

import ch.ethz.sis.openbis.generic.server.xls.importer.utils.IAttribute;

public enum SemanticAnnotationAttribute implements IAttribute
{
    OntologyId("Ontology Id", false, false),
    OntologyVersion("Ontology Version", false, false),
    OntologyAnnotationId("Ontology Annotation Id", false, false);

    private final String headerName;

    private final boolean mandatory;

    private final boolean upperCase;

    SemanticAnnotationAttribute(String headerName, boolean mandatory, boolean upperCase)
    {
        this.headerName = headerName;
        this.mandatory = mandatory;
        this.upperCase = upperCase;
    }

    public String getHeaderName()
    {
        return headerName;
    }

    @Override
    public boolean isMandatory()
    {
        return mandatory;
    }

    @Override
    public boolean isUpperCase()
    {
        return upperCase;
    }
}