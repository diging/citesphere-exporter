package edu.asu.diging.citesphere.exporter.core.model.impl;

import java.time.OffsetDateTime;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import edu.asu.diging.citesphere.exporter.core.model.IApp;

@Entity
public class App implements IApp {

    @Id
    @GeneratedValue(generator = "app_id_generator")
    @GenericGenerator(name = "app_id_generator",    
                    parameters = @Parameter(name = "prefix", value = "APP"), 
                    strategy = "edu.asu.diging.citesphere.data.bib.IdGenerator"
            )
    private String id;
    private String appName;
    private String description;
    private String createdBy;
    private OffsetDateTime createdOn;
    
    @Override
    public String getId() {
        return id;
    }
    @Override
    public void setId(String id) {
        this.id = id;
    }
    /* (non-Javadoc)
     * @see edu.asu.diging.citesphere.exporter.core.apps.impl.IApp#getAppName()
     */
    @Override
    public String getAppName() {
        return appName;
    }
    /* (non-Javadoc)
     * @see edu.asu.diging.citesphere.exporter.core.apps.impl.IApp#setAppName(java.lang.String)
     */
    @Override
    public void setAppName(String appName) {
        this.appName = appName;
    }
    @Override
    public String getDescription() {
        return description;
    }
    @Override
    public void setDescription(String description) {
        this.description = description;
    }
    /* (non-Javadoc)
     * @see edu.asu.diging.citesphere.exporter.core.apps.impl.IApp#getCreatedBy()
     */
    @Override
    public String getCreatedBy() {
        return createdBy;
    }
    /* (non-Javadoc)
     * @see edu.asu.diging.citesphere.exporter.core.apps.impl.IApp#setCreatedBy(java.lang.String)
     */
    @Override
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    /* (non-Javadoc)
     * @see edu.asu.diging.citesphere.exporter.core.apps.impl.IApp#getCreatedOn()
     */
    @Override
    public OffsetDateTime getCreatedOn() {
        return createdOn;
    }
    /* (non-Javadoc)
     * @see edu.asu.diging.citesphere.exporter.core.apps.impl.IApp#setCreatedOn(java.lang.String)
     */
    @Override
    public void setCreatedOn(OffsetDateTime createdOn) {
        this.createdOn = createdOn;
    }
}
