package io.metamorphic.models;

import java.util.Date;

/**
 * Created by markmo on 28/03/2015.
 */
public abstract class AuditedModel {

    private String processName;

    private Date created;

    private String createdBy;

    private Date modified;

    private String modifiedBy;

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Date getModified() {
        return modified;
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public void addAuditInfo() {
        this.created = new Date();
        this.modified = new Date();
    }

    public void updateAuditInfo() {
        this.modified = new Date();
    }
}
