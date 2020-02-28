package edu.asu.diging.citesphere.exporter.core.service.process.impl;

import java.io.IOException;

import org.springframework.stereotype.Component;

import edu.asu.diging.citesphere.exporter.core.service.process.ExportType;
import edu.asu.diging.citesphere.exporter.core.service.process.ExportWriter;
import edu.asu.diging.citesphere.exporter.core.service.process.Processor;

@Component
/**
 * Component to write citations as csv file.
 * 
 * @author jdamerow
 *
 */
public class CsvProcessor implements Processor {

    /* (non-Javadoc)
     * @see edu.asu.diging.citesphere.core.export.proc.impl.Processor#getSupportedType()
     */
    @Override
    public ExportType getSupportedType() {
        return ExportType.CSV;
    }
    
    @Override
    public String getFileExtension() {
        return "csv";
    }
    
    

    /* (non-Javadoc)
     * @see edu.asu.diging.citesphere.core.export.proc.impl.Processor#write(java.util.List, java.lang.Appendable)
     */
    @Override
    public ExportWriter getWriter(Appendable writer) throws IOException {
        CsvWriter csvWriter = new CsvWriter(writer);
        csvWriter.init();
        return csvWriter;
    }
    
    
}
