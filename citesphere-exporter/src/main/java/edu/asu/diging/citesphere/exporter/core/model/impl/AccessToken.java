package edu.asu.diging.citesphere.exporter.core.model.impl;

import java.time.OffsetDateTime;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import edu.asu.diging.citesphere.exporter.core.model.IAccessToken;
import edu.asu.diging.citesphere.exporter.core.model.IApp;

@Entity
public class AccessToken implements IAccessToken {

    @Id
    @GeneratedValue(generator = "token_id_generator")
    @GenericGenerator(name = "token_id_generator",    
                    parameters = @Parameter(name = "prefix", value = "AT"), 
                    strategy = "edu.asu.diging.citesphere.data.bib.IdGenerator"
            )
    private String id;
    
    @ManyToOne(targetEntity=App.class)
    private IApp app;
    private String createdBy;
    private OffsetDateTime createdOn;
    
    /* (non-Javadoc)
     * @see edu.asu.diging.citesphere.exporter.core.model.impl.IAccessToken#getId()
     */
    @Override
    public String getId() {
        return id;
    }
    /* (non-Javadoc)
     * @see edu.asu.diging.citesphere.exporter.core.model.impl.IAccessToken#setId(java.lang.String)
     */
    @Override
    public void setId(String id) {
        this.id = id;
    }
    /* (non-Javadoc)
     * @see edu.asu.diging.citesphere.exporter.core.model.impl.IAccessToken#getApp()
     */
    @Override
    public IApp getApp() {
        return app;
    }
    /* (non-Javadoc)
     * @see edu.asu.diging.citesphere.exporter.core.model.impl.IAccessToken#setApp(edu.asu.diging.citesphere.exporter.core.model.IApp)
     */
    @Override
    public void setApp(IApp app) {
        this.app = app;
    }
    /* (non-Javadoc)
     * @see edu.asu.diging.citesphere.exporter.core.model.impl.IAccessToken#getCreatedBy()
     */
    @Override
    public String getCreatedBy() {
        return createdBy;
    }
    /* (non-Javadoc)
     * @see edu.asu.diging.citesphere.exporter.core.model.impl.IAccessToken#setCreatedBy(java.lang.String)
     */
    @Override
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    /* (non-Javadoc)
     * @see edu.asu.diging.citesphere.exporter.core.model.impl.IAccessToken#getCreatedOn()
     */
    @Override
    public OffsetDateTime getCreatedOn() {
        return createdOn;
    }
    /* (non-Javadoc)
     * @see edu.asu.diging.citesphere.exporter.core.model.impl.IAccessToken#setCreatedOn(java.lang.String)
     */
    @Override
    public void setCreatedOn(OffsetDateTime createdOn) {
        this.createdOn = createdOn;
    }
    
}
