package edu.asu.diging.citesphere.exporter.core.service.iterator;

import java.util.Iterator;

import edu.asu.diging.citesphere.model.bib.ICitation;

public interface CitationIterator extends Iterator<ICitation> {

    boolean hasNext();

    ICitation next();

}