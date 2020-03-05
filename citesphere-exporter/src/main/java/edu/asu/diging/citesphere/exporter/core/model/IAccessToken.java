package edu.asu.diging.citesphere.exporter.core.model;

import java.time.OffsetDateTime;

public interface IAccessToken {

    String getId();

    void setId(String id);

    IApp getApp();

    void setApp(IApp app);

    String getCreatedBy();

    void setCreatedBy(String createdBy);

    OffsetDateTime getCreatedOn();

    void setCreatedOn(OffsetDateTime createdOn);

}