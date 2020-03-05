package edu.asu.diging.citesphere.exporter.core.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.asu.diging.citesphere.exporter.core.service.process.ExportType;

@JsonIgnoreProperties(ignoreUnknown=true)
public class JobInfo {

    private String username;
    private String zotero;
    private String zoteroId;
    private String groupId;
    private String collectionId;
    private ExportType exportType;
    private String taskId;
    
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getZotero() {
        return zotero;
    }
    public void setZotero(String zotero) {
        this.zotero = zotero;
    }
    public String getZoteroId() {
        return zoteroId;
    }
    public void setZoteroId(String zoteroId) {
        this.zoteroId = zoteroId;
    }
    public String getGroupId() {
        return groupId;
    }
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
    public String getCollectionId() {
        return collectionId;
    }
    public void setCollectionId(String collectionId) {
        this.collectionId = collectionId;
    }
    public ExportType getExportType() {
        return exportType;
    }
    public void setExportType(ExportType exportType) {
        this.exportType = exportType;
    }
    @Override
    public String toString() {
        return "JobInfo [groupId=" + groupId + ", collectionId=" + collectionId + ", exportType=" + exportType + "]";
    }
    public String getTaskId() {
        return taskId;
    }
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
}
