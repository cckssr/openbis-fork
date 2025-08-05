package ch.ethz.sis.rocrateserver.openapi.v1.service.helper.validation;

public class PropertyProblem {

    private String node;
    private String property;
    private String message;

    public PropertyProblem(String node, String property, String message) {
        this.node = node;
        this.property = property;
        this.message = message;
    }

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
