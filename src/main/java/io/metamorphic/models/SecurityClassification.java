package io.metamorphic.models;

/**
 * Created by markmo on 28/03/2015.
 */
public class SecurityClassification extends AuditedModel {

    private Integer id;

    private String name;

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
