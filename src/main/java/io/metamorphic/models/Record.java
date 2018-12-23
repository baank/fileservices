package io.metamorphic.models;

import java.util.List;

/**
 * Created by markmo on 22/04/15.
 */
public class Record extends AuditedModel {

    private Long id;

    private String name;

    private String prefix;

    private String description;

    private FileDataset dataset;

    private List<FileColumn> columns;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public FileDataset getDataset() {
        return dataset;
    }

    public void setDataset(FileDataset dataset) {
        this.dataset = dataset;
    }

    public List<FileColumn> getColumns() {
        return columns;
    }

    public void setColumns(List<FileColumn> columns) {
        this.columns = columns;
    }
}
