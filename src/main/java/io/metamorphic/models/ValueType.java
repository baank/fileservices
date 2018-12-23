package io.metamorphic.models;

/**
 * Created by markmo on 28/03/2015.
 */
public class ValueType extends AuditedModel {

    private Integer id;

    private String name;

    public ValueType() {}

    public ValueType(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
