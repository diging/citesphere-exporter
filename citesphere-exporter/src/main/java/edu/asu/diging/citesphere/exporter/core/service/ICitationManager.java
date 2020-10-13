package edu.asu.diging.citesphere.exporter.core.service;

import org.springframework.data.util.CloseableIterator;

import edu.asu.diging.citesphere.exporter.core.exception.AccessForbiddenException;
import edu.asu.diging.citesphere.exporter.core.exception.GroupDoesNotExistException;
import edu.asu.diging.citesphere.exporter.core.exception.OutOfDateException;
import edu.asu.diging.citesphere.exporter.core.exception.ZoteroHttpStatusException;
import edu.asu.diging.citesphere.exporter.core.service.impl.JobInfo;
import edu.asu.diging.citesphere.model.bib.ICitation;
import edu.asu.diging.citesphere.model.bib.ICitationCollection;
import edu.asu.diging.citesphere.model.bib.ICitationGroup;

public interface ICitationManager {

    CloseableIterator<ICitation> getAllGroupItems(JobInfo info)
            throws ZoteroHttpStatusException, GroupDoesNotExistException, AccessForbiddenException, OutOfDateException;

    ICitationGroup getGroup(JobInfo info);

    ICitationCollection getCollection(JobInfo info);

    
}