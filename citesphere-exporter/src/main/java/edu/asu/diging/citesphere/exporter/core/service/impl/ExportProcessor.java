package edu.asu.diging.citesphere.exporter.core.service.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import edu.asu.diging.citesphere.exporter.core.exception.CitesphereCommunicationException;
import edu.asu.diging.citesphere.exporter.core.exception.ExportFailedException;
import edu.asu.diging.citesphere.exporter.core.exception.FileStorageException;
import edu.asu.diging.citesphere.exporter.core.exception.ZoteroHttpStatusException;
import edu.asu.diging.citesphere.exporter.core.service.ICitesphereConnector;
import edu.asu.diging.citesphere.exporter.core.service.IExportProcessor;
import edu.asu.diging.citesphere.exporter.core.service.IFileStorageManager;
import edu.asu.diging.citesphere.exporter.core.service.iterator.CitationIterator;
import edu.asu.diging.citesphere.exporter.core.service.process.ExportType;
import edu.asu.diging.citesphere.exporter.core.service.process.ExportWriter;
import edu.asu.diging.citesphere.exporter.core.service.process.Processor;
import edu.asu.diging.citesphere.messages.model.KafkaJobMessage;

@Service
public class ExportProcessor implements IExportProcessor {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ICitesphereConnector connector;

    @Autowired
    private ApplicationContext ctx;
    
    @Autowired
    private IFileStorageManager storageManager;
    
    @Autowired
    private ICitationManager citationManager;
    
    private Map<ExportType, Processor> processors;
    

    @PostConstruct
    public void init() {
        processors = new HashMap<>();
        Map<String, Processor> beans = ctx.getBeansOfType(Processor.class);
        for (Processor bean : beans.values()) {
            processors.put(bean.getSupportedType(), bean);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * edu.asu.diging.citesphere.exporter.core.service.impl.IExportProcessor#process
     * (edu.asu.diging.citesphere.messages.model.KafkaJobMessage)
     */
    @Override
    public void process(KafkaJobMessage message) {
        JobInfo info = null;
        try {
            info = connector.getJobInfo(message.getId());
        } catch (CitesphereCommunicationException e) {
            // TODO: send back error message
            logger.error("Couldn't connect to citesphere", e);
            return;
        }

        Processor processor = processors.get(info.getExportType());
        if (processor == null) {
            // TODO: send back error message
            logger.error("No processor available for " + info.getExportType());
            return;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd--HH-mm-ss");
        String time = LocalDateTime.now().format(formatter);
        String filename = "export-" + time + "." + processor.getFileExtension();
        
        BufferedWriter writer = null;
        try {
            try {
                writer = createWriter(info.getUsername(), info.getTaskId(), processor, filename);
            } catch (ExportFailedException e) {
                // TODO: send back error message
                logger.error("Could not create writer. Export failed for " + info.getTaskId());
                return;
            }
        } catch (IOException e) {
            // TODO: send back error message
            logger.error("Creating writer failed for " + info.getTaskId());
            return;
        }
        
        ExportWriter exportWriter;
        try {
            exportWriter = processor.getWriter(writer);
        } catch (IOException e) {
            // TODO: send back error message
            logger.error("Couldn't connect to citesphere", e);
            return;
        }
        
        // get citations and write
        CitationIterator citIterator;
        try {
            citIterator = citationManager.getCitations(info);
        } catch (ZoteroHttpStatusException e) {
            // TODO: send back error message
            logger.error("Couldn't retrieve citation from Zotero.", e);
            return;
        }
        
        while(citIterator.hasNext()) {
            try {
                exportWriter.writeRow(citIterator.next());
            } catch (ExportFailedException | IOException e) {
                // TODO: send back error message
                logger.error("Couldn't write citations.", e);
                return;
            }
        }
    }
    
    private BufferedWriter createWriter(String username, String taskId, Processor processor, String filename)
            throws ExportFailedException, IOException {
         try {
            storageManager.saveFile(username, taskId, filename, new byte[0]);
        } catch (FileStorageException e) {
            throw new ExportFailedException("Could not create export file.", e);
        }
        
        String filePath = storageManager.getFolderPath(username, taskId);
        filePath += File.separator + filename;
        
        BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath));
        return writer;
    }
}
