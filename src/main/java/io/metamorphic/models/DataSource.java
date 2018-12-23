package io.metamorphic.models;

/**
 * Created by markmo on 28/03/2015.
 */
public abstract class DataSource extends AuditedModel {

    private Long id;

    private String name;

    private String sourcingMethod;
    private String hostname;
    private String ipaddr;
    private int port;
    private String firewallStatus;
    private String description;
    private AnalysisStatus analysisStatus;

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

    public String getSourcingMethod() {
        return sourcingMethod;
    }

    public void setSourcingMethod(String sourcingMethod) {
        this.sourcingMethod = sourcingMethod;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getIpaddr() {
        return ipaddr;
    }

    public void setIpaddr(String ipaddr) {
        this.ipaddr = ipaddr;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getFirewallStatus() {
        return firewallStatus;
    }

    public void setFirewallStatus(String firewallStatus) {
        this.firewallStatus = firewallStatus;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AnalysisStatus getAnalysisStatus() {
        return analysisStatus;
    }

    public void setAnalysisStatus(AnalysisStatus analysisStatus) {
        this.analysisStatus = analysisStatus;
    }
}
