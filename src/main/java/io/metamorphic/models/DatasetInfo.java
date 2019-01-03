package io.metamorphic.models;

import static io.metamorphic.commons.utils.ArrayUtils.memberOf;
import io.metamorphic.fileservices.FileServiceImpl;
import io.metamorphic.fileservices.FileParameters;
import io.metamorphic.fileservices.Naming;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author Mark Moloney <markmo @ metamorphic.io>
 * Copyright 2015
 */
public class DatasetInfo {

    private String ssuDesignation;
    private String dataSourceName;
    private String name;
    private String fileType;
    private FileParameters fileParameters;
    private List<ColumnInfo> columns;
    private String error;

    public DatasetInfo() {}

    public DatasetInfo(String error) {
        this.error = error;
    }

    public String getSsuDesignation() {
        return ssuDesignation;
    }

    public void setSsuDesignation(String ssuDesignation) {
        this.ssuDesignation = ssuDesignation;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public void setDataSourceName(String dataSourceName) {
        this.dataSourceName = dataSourceName;
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

    public String toCurationProperties(Properties props) {
        StringBuilder sb = new StringBuilder();
        String schemaName = Naming.underscoreFormat(dataSourceName);
        String tableName = Naming.underscoreFormat(name);
        sb.append("telstraSSUStatus=").append(ssuDesignation).append("\n");
        sb.append("data_source=").append(schemaName).append("\n");
        sb.append("dataset=").append(tableName).append("\n");
        sb.append("schedule_frequency=0 0 * * *\n");
        sb.append("source_files_pattern=TODO\n");
        sb.append("base_apps_dir=TODO\n");
        sb.append("base_data_dir=TODO\n");
        sb.append("hcat_url=").append(props.getProperty("hcat_url", "TODO")).append("\n");
        sb.append("nameNode=").append(props.getProperty("nameNode", "TODO")).append("\n");
        sb.append("resourceManager=").append(props.getProperty("resourceManager", "TODO"))
                .append("\n");
        sb.append("oozie.coord.application.path=")
                .append(props.getProperty("oozie.coord.application.path", "TODO"))
                .append("\n");
        sb.append("curation_application_spark_opts=--driver-memory 4g --executor-memory 4g  --num-executors 2 --executor-cores 4 --jars ${nameNode}/${base_apps_dir}/share/oozie/lib/notificationService.jar --conf spark.yarn.maxAppAttempts=1\n");
        sb.append("priority_rating=medium\n");
        sb.append("curation_application_extra_columns=\n");
        sb.append("notification_email_address=")
                .append(props.getProperty("notification_email_address", "TODO")).append("\n");
        sb.append("sla_wf_should_start_in_minutes=60\n");
        sb.append("sla_wf_should_end_in_minutes=60\n");
        sb.append("sla_wf_max_duration_minutes=60\n");
        return sb.toString();
    }

    public String toIngestionProperties(Properties props) {
        StringBuilder sb = new StringBuilder();
        String schemaName = Naming.underscoreFormat(dataSourceName);
        String tableName = Naming.underscoreFormat(name);
        sb.append("data_source=").append(schemaName).append("\n");
        sb.append("telstraSSUStatus=").append(ssuDesignation).append("\n");
        sb.append("priority_rating=medium\n");
        sb.append("source_files_pattern=TODO\n");
        sb.append("data_provider=").append(schemaName).append("\n");
        sb.append("dataset=").append(tableName).append("\n");
        sb.append("schedule_frequency=0 0 * * *\n");
        sb.append("data_checker_retry_interval_min=15\n");
        sb.append("oozie.coord.application.path=")
                .append(props.getProperty("oozie.coord.application.path", "TODO"))
                .append("\n");
        sb.append("notification_email_address=")
                .append(props.getProperty("notification_email_address", "TODO")).append("\n");
        sb.append("sla_wf_should_start_in_minutes=60\n");
        sb.append("sla_wf_max_duration_minutes=60\n");
        sb.append("resourceManager=").append(props.getProperty("resourceManager", "TODO")).append("\n");
        sb.append("data_checker_retry_count=3\n");
        sb.append("sla_wf_should_end_in_minutes=60\n");
        sb.append("s3_endpoint=").append(props.getProperty("s3_endpoint", "TODO")).append("\n");
        sb.append("nameNode=").append(props.getProperty("nameNode", "TODO")).append("\n");
        sb.append("base_data_dir=TODO\n");
        sb.append("base_apps_dir=TODO\n");
        return sb.toString();
    }

    public String toFileProperties() {
        StringBuilder sb = new StringBuilder();
        String schemaName = Naming.underscoreFormat(dataSourceName);
        String tableName = Naming.underscoreFormat(name);
        sb.append("srcDataTier=r\n");
        sb.append("telstraSSUStatus=").append(ssuDesignation).append("\n");
        sb.append("dataSource=").append(schemaName).append("\n");
        sb.append("targetDatabase=").append(schemaName).append("\n");
        sb.append("targetTable=").append(tableName).append("\n");
        sb.append("dataSet=").append(tableName).append("\n");
        sb.append("readerOptions=header=");
        if (fileParameters.hasHeader()) {
            sb.append("true");
        } else {
            sb.append("false");
        }
        sb.append(",quote=").append(fileParameters.getTextQualifier());
        sb.append(",escape=").append(fileParameters.getEscapeCharacter());
        sb.append(",ignoreLeadingWhiteSpace=");
        if (fileParameters.isSkipInitialSpace()) {
            sb.append("true");
        } else {
            sb.append("false");
        }
        sb.append(",multiLine=");
        if (FileType.MULTIRECORD.toString().equals(fileType)) {
            sb.append("true");
        } else {
            sb.append("false");
        }
        String dateFormat = fileParameters.getFirstDateFormat();
        if (dateFormat != null) {
            sb.append(",dateFormat=").append(dateFormat);
        }
        String dateTimeFormat = fileParameters.getFirstDateTimeFormat();
        if (dateTimeFormat != null) {
            sb.append(",timestampFormat=").append(dateTimeFormat);
        }
        sb.append("\n");
        sb.append("writeMode=append\n");
        sb.append("fileBasedTagColumns=src_date\n");
        sb.append("recordDelimiter=").append(fileParameters.getLineTerminator()).append("\n");
        sb.append("fieldDelimiter=").append(fileParameters.getColumnDelimiter()).append("\n");
        sb.append("srcFormats=src_date=yyyyMMdd\n");
        sb.append("fileBasedTagPattern=TODO\n");
        sb.append("targetPartition=src_date\n");
        sb.append("readerFormat=");
        if (FileType.DELIMITED.toString().equals(fileType)) {
            sb.append("csv\n");
        } else if (FileType.JSON.toString().equals(fileType)) {
            sb.append("json\n");
        } else if (FileType.FIXED.toString().equals(fileType)) {
            sb.append("fixedwidth\n");
        } else {
            sb.append("TODO\n");
        }
        sb.append("errorThresholdPercent=99\n");
        sb.append("basePath=/data\n");
        if (fileParameters.getSrcFormats().size() > 0) {
            sb.append("srcFormats=");
            int i = 0;
            for (Map.Entry<String, String> entry : fileParameters.getSrcFormats().entrySet()) {
                String columnName = Naming.underscoreFormat(entry.getKey());
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(columnName).append("=").append(entry.getValue());
                i += 1;
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public String toDDL() {
        StringBuilder sb = new StringBuilder();
        String schemaName = Naming.underscoreFormat(dataSourceName);
        String tableName = Naming.underscoreFormat(name);
        sb.append("CREATE DATABASE IF NOT EXISTS ").append(schemaName).append(";\n\n");
        sb.append("DROP EXTERNAL TABLE ").append(schemaName).append('.').append(tableName).append(";\n\n");
        sb.append("CREATE EXTERNAL TABLE ").append(schemaName).append('.').append(tableName).append(" (\n");
        String indent = "    ";
        for (int i = 0; i < columns.size(); i++) {
            ColumnInfo column = columns.get(i);
            String columnName = Naming.underscoreFormat(column.getName());
            String dataType = column.getSqlType();
//            int length = column.getLength();
//            // bin the lengths
//            if (length < 10) {
//                length = 10;
//            } else if (length < 20) {
//                length = 20;
//            } else if (length < 50) {
//                length = 50;
//            } else if (length < 100) {
//                length = 100;
//            } else if (length < 255) {
//                length = 255;
//            } else {
//                dataType = "TEXT";
//            }
            sb.append(indent);
            if (i > 0) {
                sb.append(",");
            }
            sb.append(columnName);
//            if (dataType.equals("NVARCHAR")) {
//                sb.append(" NVARCHAR(").append(length).append(")\n");
            if (memberOf(FileServiceImpl.STRING_TYPES, dataType)) {
                sb.append(" ").append("STRING\n");
            } else {
                sb.append(" ").append(dataType).append("\n");
            }
        }
        sb.append(")\n");
        sb.append("PARTITIONED BY (\n");
        sb.append(indent).append("src_date DATE\n");
        sb.append(")\n");
        sb.append("STORED AS ORC\n");
        sb.append("LOCATION '/data/c/").append(ssuDesignation).append("/")
                .append(schemaName).append("/").append(tableName).append("';\n");
        return sb.toString();
    }
}
