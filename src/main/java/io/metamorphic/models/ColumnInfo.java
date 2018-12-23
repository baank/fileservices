package io.metamorphic.models;

/**
 * @author Mark Moloney <markmo @ metamorphic.io>
 * Copyright 2015
 */
public class ColumnInfo {

    private String name;
    private int columnIndex;
    private String type;
    private String sqlType;
    private int length;

    public ColumnInfo(String name, int columnIndex, String type, String sqlType, int length) {
        this.name = name;
        this.columnIndex = columnIndex;
        this.type = type;
        this.sqlType = sqlType;
        this.length = length;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getColumnIndex() {
        return columnIndex;
    }

    public void setColumnIndex(int columnIndex) {
        this.columnIndex = columnIndex;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSqlType() {
        return sqlType;
    }

    public void setSqlType(String sqlType) {
        this.sqlType = sqlType;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }
}
