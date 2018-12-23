package io.metamorphic.models;

import java.sql.Time;

/**
 * Created by markmo on 28/03/2015.
 */
public abstract class Dataset extends AuditedModel {

    private Long id;

    private String name;

    private SecurityClassification securityClassification;

    private String namespace;
    private String description;
    private String comments;
    private String contactPerson;
    private boolean customerData;

    private TimeUnit availableHistoryUnitOfTime;

    private int availableHistoryUnits;
    private boolean batch;

    private TimeUnit refreshFrequencyUnitOfTime;

    private int refreshFrequencyUnits;
    private Time timeOfDayDataAvailable;

    private TimeUnit dataAvailableUnitOfTime;

    private String dataAvailableDaysOfWeek;

    private TimeUnit dataLatencyUnitOfTime;

    private int dataLatencyUnits;

    private AnalysisStatus analysisStatus;


    // custom attributes
    private String architectureDomain;
    private boolean financialBankingData;
    private boolean idAndServiceHistory;
    private boolean creditCardData;
    private boolean financialReportingData;
    private boolean privacyData;
    private boolean regulatoryData;
    private boolean nbnConfidentialData;
    private boolean nbnCompliant;
    private String ssuReady;
    private String ssuRemediationMethod;
    private int historyDataSizeGb;
    private int refreshDataSizeGb;


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

    public abstract String getType();

    public abstract DataSource getDataSource();

    public SecurityClassification getSecurityClassification() {
        return securityClassification;
    }

    public void setSecurityClassification(SecurityClassification securityClassification) {
        this.securityClassification = securityClassification;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public boolean isCustomerData() {
        return customerData;
    }

    public void setCustomerData(boolean customerData) {
        this.customerData = customerData;
    }

    public TimeUnit getAvailableHistoryUnitOfTime() {
        return availableHistoryUnitOfTime;
    }

    public void setAvailableHistoryUnitOfTime(TimeUnit availableHistoryUnitOfTime) {
        this.availableHistoryUnitOfTime = availableHistoryUnitOfTime;
    }

    public int getAvailableHistoryUnits() {
        return availableHistoryUnits;
    }

    public void setAvailableHistoryUnits(int availableHistoryUnits) {
        this.availableHistoryUnits = availableHistoryUnits;
    }

    public boolean isBatch() {
        return batch;
    }

    public void setBatch(boolean batch) {
        this.batch = batch;
    }

    public TimeUnit getRefreshFrequencyUnitOfTime() {
        return refreshFrequencyUnitOfTime;
    }

    public void setRefreshFrequencyUnitOfTime(TimeUnit refreshFrequencyUnitOfTime) {
        this.refreshFrequencyUnitOfTime = refreshFrequencyUnitOfTime;
    }

    public int getRefreshFrequencyUnits() {
        return refreshFrequencyUnits;
    }

    public void setRefreshFrequencyUnits(int refreshFrequencyUnits) {
        this.refreshFrequencyUnits = refreshFrequencyUnits;
    }

    public Time getTimeOfDayDataAvailable() {
        return timeOfDayDataAvailable;
    }

    public void setTimeOfDayDataAvailable(Time timeOfDayDataAvailable) {
        this.timeOfDayDataAvailable = timeOfDayDataAvailable;
    }

    public TimeUnit getDataAvailableUnitOfTime() {
        return dataAvailableUnitOfTime;
    }

    public void setDataAvailableUnitOfTime(TimeUnit dataAvailableUnitOfTime) {
        this.dataAvailableUnitOfTime = dataAvailableUnitOfTime;
    }

    public String getDataAvailableDaysOfWeek() {
        return dataAvailableDaysOfWeek;
    }

    public void setDataAvailableDaysOfWeek(String dataAvailableDaysOfWeek) {
        this.dataAvailableDaysOfWeek = dataAvailableDaysOfWeek;
    }

    public TimeUnit getDataLatencyUnitOfTime() {
        return dataLatencyUnitOfTime;
    }

    public void setDataLatencyUnitOfTime(TimeUnit dataLatencyUnitOfTime) {
        this.dataLatencyUnitOfTime = dataLatencyUnitOfTime;
    }

    public int getDataLatencyUnits() {
        return dataLatencyUnits;
    }

    public void setDataLatencyUnits(int dataLatencyUnits) {
        this.dataLatencyUnits = dataLatencyUnits;
    }

    public AnalysisStatus getAnalysisStatus() {
        return analysisStatus;
    }

    public void setAnalysisStatus(AnalysisStatus analysisStatus) {
        this.analysisStatus = analysisStatus;
    }

    // custom attributes
    public String getArchitectureDomain() {
        return architectureDomain;
    }

    public void setArchitectureDomain(String architectureDomain) {
        this.architectureDomain = architectureDomain;
    }

    public boolean isFinancialBankingData() {
        return financialBankingData;
    }

    public void setFinancialBankingData(boolean financialBankingData) {
        this.financialBankingData = financialBankingData;
    }

    public boolean isIdAndServiceHistory() {
        return idAndServiceHistory;
    }

    public void setIdAndServiceHistory(boolean idAndServiceHistory) {
        this.idAndServiceHistory = idAndServiceHistory;
    }

    public boolean isCreditCardData() {
        return creditCardData;
    }

    public void setCreditCardData(boolean creditCardData) {
        this.creditCardData = creditCardData;
    }

    public boolean isFinancialReportingData() {
        return financialReportingData;
    }

    public void setFinancialReportingData(boolean financialReportingData) {
        this.financialReportingData = financialReportingData;
    }

    public boolean isPrivacyData() {
        return privacyData;
    }

    public void setPrivacyData(boolean privacyData) {
        this.privacyData = privacyData;
    }

    public boolean isRegulatoryData() {
        return regulatoryData;
    }

    public void setRegulatoryData(boolean regulatoryData) {
        this.regulatoryData = regulatoryData;
    }

    public boolean isNbnConfidentialData() {
        return nbnConfidentialData;
    }

    public void setNbnConfidentialData(boolean nbnConfidentialData) {
        this.nbnConfidentialData = nbnConfidentialData;
    }

    public boolean isNbnCompliant() {
        return nbnCompliant;
    }

    public void setNbnCompliant(boolean nbnCompliant) {
        this.nbnCompliant = nbnCompliant;
    }

    public String getSsuReady() {
        return ssuReady;
    }

    public void setSsuReady(String ssuReady) {
        this.ssuReady = ssuReady;
    }

    public String getSsuRemediationMethod() {
        return ssuRemediationMethod;
    }

    public void setSsuRemediationMethod(String ssuRemediationMethod) {
        this.ssuRemediationMethod = ssuRemediationMethod;
    }

    public int getHistoryDataSizeGb() {
        return historyDataSizeGb;
    }

    public void setHistoryDataSizeGb(int historyDataSizeGb) {
        this.historyDataSizeGb = historyDataSizeGb;
    }

    public int getRefreshDataSizeGb() {
        return refreshDataSizeGb;
    }

    public void setRefreshDataSizeGb(int refreshDataSizeGb) {
        this.refreshDataSizeGb = refreshDataSizeGb;
    }
}
