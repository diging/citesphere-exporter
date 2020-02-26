package edu.asu.diging.citesphere.exporter.core.service.process;

import java.io.IOException;

import edu.asu.diging.citesphere.model.bib.ICitation;
import edu.asu.diging.citesphere.model.bib.IGrouping;

public interface ExportWriter {

    void cleanUp() throws IOException;

    void writeRow(ICitation citation, IGrouping grouping) throws IOException;

}