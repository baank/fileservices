package io.metamorphic.models;

import io.metamorphic.fileservices.FileParameters;
import io.metamorphic.fileservices.Naming;

import java.util.List;

/**
 * @author Mark Moloney <markmo @ metamorphic.io>
 * Copyright 2015
 */
public class DatasetInfo {

    private String name;
    private String fileType;
    private FileParameters fileParameters;
    private List<ColumnInfo> columns;
    private String error;

    public DatasetInfo() {}

    public DatasetInfo(String error) {
        this.error = error;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public FileParameters getFileParameters() {
        return fileParameters;
    }

    public void setFileParameters(FileParameters fileParameters) {
        this.fileParameters = fileParameters;
    }

    public List<ColumnInfo> getColumns() {
        return columns;
    }

    public void setColumns(List<ColumnInfo> columns) {
        this.columns = columns;
    }

    public String toDDL() {
        StringBuilder sb = new StringBuilder();
        String tableName = Naming.underscoreFormat(name);
        sb.append("DROP TABLE ").append(tableName).append(";\n");
        sb.append("CREATE TABLE ").append(tableName).append(" (\n");
        String indent = "    ";
        for (int i = 0; i < columns.size(); i++) {
            ColumnInfo column = columns.get(i);
            String columnName = Naming.underscoreFormat(column.getName());
            String dataType = column.getSqlType();
            int length = column.getLength();
            // bin the lengths
            if (length < 10) {
                length = 10;
            } else if (length < 20) {
                length = 20;
            } else if (length < 50) {
                length = 50;
            } else if (length < 100) {
                length = 100;
            } else if (length < 255) {
                length = 255;
            } else {
                dataType = "TEXT";
            }
            sb.append(indent);
            if (i > 0) {
                sb.append(",");
            }
            if (dataType.equals("NVARCHAR")) {
                sb.append(columnName).append(" NVARCHAR(").append(length).append(")\n");
            } else {
                sb.append(columnName).append(" ").append(dataType).append("\n");
            }
        }
        sb.append(indent).append(",src_date DATE\n");
        sb.append(");\n");
        return sb.toString();
    }
}
