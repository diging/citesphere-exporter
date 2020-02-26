package edu.asu.diging.citesphere.exporter.core.model.impl;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import edu.asu.diging.citesphere.exporter.core.model.IDownloadTask;
import edu.asu.diging.citesphere.messages.model.ResponseCode;
import edu.asu.diging.citesphere.messages.model.Status;

@Entity
public class DownloadTask implements IDownloadTask {

    @Id
    @GeneratedValue(generator = "task_id_generator")
    @GenericGenerator(name = "task_id_generator",    
                    parameters = @Parameter(name = "prefix", value = "TASK"), 
                    strategy = "edu.asu.diging.citesphere.data.bib.IdGenerator"
            )
    private String id;
    private String citesphereTaskId;
    private String username;
    private String filename;
    private String contentType;
    private long fileSize;
    @Enumerated(EnumType.STRING)
    private Status status;
    @Enumerated(EnumType.STRING)
    private ResponseCode responseCode;
    
    /* (non-Javadoc)
     * @see edu.asu.diging.citesphere.exporter.core.model.impl.IDownloadTask#getId()
     */
    @Override
    public String getId() {
        return id;
    }
    /* (non-Javadoc)
     * @see edu.asu.diging.citesphere.exporter.core.model.impl.IDownloadTask#setId(java.lang.String)
     */
    @Override
    public void setId(String id) {
        this.id = id;
    }
    /* (non-Javadoc)
     * @see edu.asu.diging.citesphere.exporter.core.model.impl.IDownloadTask#getCitesphereTaskId()
     */
    @Override
    public String getCitesphereTaskId() {
        return citesphereTaskId;
    }
    /* (non-Javadoc)
     * @see edu.asu.diging.citesphere.exporter.core.model.impl.IDownloadTask#setCitesphereTaskId(java.lang.String)
     */
    @Override
    public void setCitesphereTaskId(String citesphereTaskId) {
        this.citesphereTaskId = citesphereTaskId;
    }
    /* (non-Javadoc)
     * @see edu.asu.diging.citesphere.exporter.core.model.impl.IDownloadTask#getUsername()
     */
    @Override
    public String getUsername() {
        return username;
    }
    /* (non-Javadoc)
     * @see edu.asu.diging.citesphere.exporter.core.model.impl.IDownloadTask#setUsername(java.lang.String)
     */
    @Override
    public void setUsername(String username) {
        this.username = username;
    }
    /* (non-Javadoc)
     * @see edu.asu.diging.citesphere.exporter.core.model.impl.IDownloadTask#getFilename()
     */
    @Override
    public String getFilename() {
        return filename;
    }
    /* (non-Javadoc)
     * @see edu.asu.diging.citesphere.exporter.core.model.impl.IDownloadTask#setFilename(java.lang.String)
     */
    @Override
    public void setFilename(String filename) {
        this.filename = filename;
    }
    @Override
    public String getContentType() {
        return contentType;
    }
    @Override
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    @Override
    public long getFileSize() {
        return fileSize;
    }
    @Override
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
    /* (non-Javadoc)
     * @see edu.asu.diging.citesphere.exporter.core.model.impl.IDownloadTask#getStatus()
     */
    @Override
    public Status getStatus() {
        return status;
    }
    /* (non-Javadoc)
     * @see edu.asu.diging.citesphere.exporter.core.model.impl.IDownloadTask#setStatus(edu.asu.diging.citesphere.messages.model.Status)
     */
    @Override
    public void setStatus(Status status) {
        this.status = status;
    }
    @Override
    public ResponseCode getResponseCode() {
        return responseCode;
    }
    @Override
    public void setResponseCode(ResponseCode responseCode) {
        this.responseCode = responseCode;
    }
}
