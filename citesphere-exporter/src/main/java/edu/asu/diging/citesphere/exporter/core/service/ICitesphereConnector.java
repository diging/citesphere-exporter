package edu.asu.diging.citesphere.exporter.core.service;

import edu.asu.diging.citesphere.exporter.core.exception.CitesphereCommunicationException;
import edu.asu.diging.citesphere.exporter.core.service.impl.JobInfo;

public interface ICitesphereConnector {

    JobInfo getJobInfo(String apiToken) throws CitesphereCommunicationException;

}