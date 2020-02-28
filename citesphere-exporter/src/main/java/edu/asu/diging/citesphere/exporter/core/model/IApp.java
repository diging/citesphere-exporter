package edu.asu.diging.citesphere.exporter.core.model;

import java.time.OffsetDateTime;

public interface IApp {

    String getAppName();

    void setAppName(String appName);

    String getCreatedBy();

    void setCreatedBy(String createdBy);

    OffsetDateTime getCreatedOn();

    void setCreatedOn(OffsetDateTime createdOn);

    void setId(String id);

    String getId();

    String getDescription();

    void setDescription(String description);

}