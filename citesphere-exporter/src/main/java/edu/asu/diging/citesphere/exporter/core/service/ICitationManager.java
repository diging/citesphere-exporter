package edu.asu.diging.citesphere.exporter.core.service;

import edu.asu.diging.citesphere.exporter.core.exception.ZoteroHttpStatusException;
import edu.asu.diging.citesphere.exporter.core.service.impl.JobInfo;
import edu.asu.diging.citesphere.exporter.core.service.iterator.CitationIterator;

public interface ICitationManager {

    CitationIterator getCitations(JobInfo info) throws ZoteroHttpStatusException;

}