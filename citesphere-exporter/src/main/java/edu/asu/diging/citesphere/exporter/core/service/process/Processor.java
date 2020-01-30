package edu.asu.diging.citesphere.exporter.core.service.process;

import java.io.IOException;

public interface Processor {

    ExportType getSupportedType();

    ExportWriter getWriter(Appendable writer) throws IOException;

    String getFileExtension();

}