package edu.asu.diging.citesphere.exporter.core.service.process;

import java.io.IOException;

import edu.asu.diging.citesphere.exporter.core.exception.ExportFailedException;
import edu.asu.diging.citesphere.model.bib.ICitation;

public interface ExportWriter {

    void cleanUp() throws IOException;

    void writeRow(ICitation citation) throws ExportFailedException, IOException;

}