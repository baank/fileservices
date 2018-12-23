package io.metamorphic.models;

/**
 * Created by markmo on 28/03/2015.
 */
public abstract class DataColumn extends AuditedModel {

    private Long id;

    private String name;

    /**
     * The inferred data type.
     */
    private DataType dataType;

    private DataType originalDataType;

    private ValueType valueType;

    private int columnIndex;

    private String description;
    private String characterSet;

    private String collation;

    private boolean unique;

    private NullableType nullableType;

    private int length;

    private String defaultValue;
    private boolean autoinc;
    private boolean dimension;

    private int precision;

    private int scale;

    private boolean featureParamCandidate;
    private boolean ignore;

    private AnalysisStatus analysisStatus;

    public abstract Dataset getDataset();

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

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    public String getDataTypeName() {
        return dataType == null ? null : dataType.getName();
    }

    public DataType getOriginalDataType() {
        return originalDataType;
    }

    public void setOriginalDataType(DataType originalDataType) {
        this.originalDataType = originalDataType;
    }

    public ValueType getValueType() {
        return valueType;
    }

    public void setValueType(ValueType valueType) {
        this.valueType = valueType;
    }

    public String getValueTypeName() {
        return valueType == null ? null : valueType.getName();
    }

    public int getColumnIndex() {
        return columnIndex;
    }

    public void setColumnIndex(int columnIndex) {
        this.columnIndex = columnIndex;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCharacterSet() {
        return characterSet;
    }

    public void setCharacterSet(String characterSet) {
        this.characterSet = characterSet;
    }

    public String getCollation() {
        return collation;
    }

    public void setCollation(String collation) {
        this.collation = collation;
    }

    public boolean isUnique() {
        return unique;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    public NullableType getNullableType() {
        return nullableType;
    }

    public void setNullableType(NullableType nullableType) {
        this.nullableType = nullableType;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isAutoinc() {
        return autoinc;
    }

    public void setAutoinc(boolean autoinc) {
        this.autoinc = autoinc;
    }

    public boolean isDimension() {
        return dimension;
    }

    public void setDimension(boolean dimension) {
        this.dimension = dimension;
    }

    public int getPrecision() {
        return precision;
    }

    public void setPrecision(int precision) {
        this.precision = precision;
    }

    public int getScale() {
        return scale;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    public boolean isFeatureParamCandidate() {
        return featureParamCandidate;
    }

    public void setFeatureParamCandidate(boolean featureParamCandidate) {
        this.featureParamCandidate = featureParamCandidate;
    }

    public boolean isIgnore() {
        return ignore;
    }

    public void setIgnore(boolean ignore) {
        this.ignore = ignore;
    }

    public AnalysisStatus getAnalysisStatus() {
        return analysisStatus;
    }

    public void setAnalysisStatus(AnalysisStatus analysisStatus) {
        this.analysisStatus = analysisStatus;
    }
}
