package io.metamorphic.models;

import java.util.List;

/**
 * Created by markmo on 28/03/2015.
 */
public class FileDataset extends Dataset {

    private FileDataSource dataSource;

    private FileType fileType;

    private CompressionType compressionType;

    private String columnDelimiter;
    private boolean headerRow;
    private boolean footerRow;
    private String rowDelimiter;
    private String textQualifier;
    private boolean multiRecordset;

    private List<Record> records;

    private List<FileColumn> columns;

    private List<FileColumn> naturalKeyColumns;

    @Override
    public String getType() {
        return "File";
    }

    @Override
    public FileDataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(FileDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public FileType getFileType() {
        return fileType;
    }

    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }

    public CompressionType getCompressionType() {
        return compressionType;
    }

    public void setCompressionType(CompressionType compressionType) {
        this.compressionType = compressionType;
    }

    public String getColumnDelimiter() {
        return columnDelimiter;
    }

    public void setColumnDelimiter(String columnDelimiter) {
        this.columnDelimiter = columnDelimiter;
    }

    public boolean isHeaderRow() {
        return headerRow;
    }

    public void setHeaderRow(boolean headerRow) {
        this.headerRow = headerRow;
    }

    public boolean isFooterRow() {
        return footerRow;
    }

    public void setFooterRow(boolean footerRow) {
        this.footerRow = footerRow;
    }

    public String getRowDelimiter() {
        return rowDelimiter;
    }

    public void setRowDelimiter(String rowDelimiter) {
        this.rowDelimiter = rowDelimiter;
    }

    public String getTextQualifier() {
        return textQualifier;
    }

    public void setTextQualifier(String textQualifier) {
        this.textQualifier = textQualifier;
    }

    public boolean isMultiRecordset() {
        return multiRecordset;
    }

    public void setMultiRecordset(boolean multiRecordset) {
        this.multiRecordset = multiRecordset;
    }

    public List<Record> getRecords() {
        return records;
    }

    public void setRecords(List<Record> records) {
        this.records = records;
    }

    public List<FileColumn> getColumns() {
        return columns;
    }

    public void setColumns(List<FileColumn> columns) {
        this.columns = columns;
    }

    public List<FileColumn> getNaturalKeyColumns() {
        return naturalKeyColumns;
    }

    public void setNaturalKeyColumns(List<FileColumn> naturalKeyColumns) {
        this.naturalKeyColumns = naturalKeyColumns;
    }

    public String getNaturalKeyColumnsId() {
        if (naturalKeyColumns == null) {
            return null;
        }
        int n = naturalKeyColumns.size();
        String[] ids = new String[n];
        for (int i = 0; i < n; i++) {
            ids[i] = naturalKeyColumns.get(i).getId().toString();
        }
        if (n == 0) {
            return null;
        } else {
            StringBuilder sb = new StringBuilder();
            for (String id : ids) {
                sb.append(id).append(',');
            }
            return sb.substring(0, sb.length() - 1);
        }
    }
}
