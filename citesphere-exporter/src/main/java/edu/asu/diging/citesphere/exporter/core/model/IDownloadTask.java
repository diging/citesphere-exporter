package edu.asu.diging.citesphere.exporter.core.model;

import edu.asu.diging.citesphere.messages.model.ResponseCode;
import edu.asu.diging.citesphere.messages.model.Status;

public interface IDownloadTask {

    String getId();

    void setId(String id);

    String getCitesphereTaskId();

    void setCitesphereTaskId(String citesphereTaskId);

    String getUsername();

    void setUsername(String username);

    String getFilename();

    void setFilename(String filename);

    Status getStatus();

    void setStatus(Status status);

    void setResponseCode(ResponseCode responseCode);

    ResponseCode getResponseCode();

    void setContentType(String contentType);

    String getContentType();

    void setFileSize(long fileSize);

    long getFileSize();

}