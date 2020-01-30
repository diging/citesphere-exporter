package edu.asu.diging.citesphere.exporter.core.service.impl;

import edu.asu.diging.citesphere.exporter.core.exception.ZoteroHttpStatusException;
import edu.asu.diging.citesphere.exporter.core.service.iterator.CitationIterator;

public interface ICitationManager {

    CitationIterator getCitations(JobInfo info) throws ZoteroHttpStatusException;

}