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
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import edu.asu.diging.citesphere.exporter.core.data.DownloadTaskRepository;
import edu.asu.diging.citesphere.exporter.core.exception.CitesphereCommunicationException;
import edu.asu.diging.citesphere.exporter.core.exception.ExportFailedException;
import edu.asu.diging.citesphere.exporter.core.exception.FileStorageException;
import edu.asu.diging.citesphere.exporter.core.exception.MessageCreationException;
import edu.asu.diging.citesphere.exporter.core.exception.ZoteroHttpStatusException;
import edu.asu.diging.citesphere.exporter.core.kafka.IKafkaRequestProducer;
import edu.asu.diging.citesphere.exporter.core.model.IDownloadTask;
import edu.asu.diging.citesphere.exporter.core.model.impl.DownloadTask;
import edu.asu.diging.citesphere.exporter.core.service.ICitationManager;
import edu.asu.diging.citesphere.exporter.core.service.ICitesphereConnector;
import edu.asu.diging.citesphere.exporter.core.service.IExportProcessor;
import edu.asu.diging.citesphere.exporter.core.service.IFileStorageManager;
import edu.asu.diging.citesphere.exporter.core.service.iterator.CitationIterator;
import edu.asu.diging.citesphere.exporter.core.service.process.ExportType;
import edu.asu.diging.citesphere.exporter.core.service.process.ExportWriter;
import edu.asu.diging.citesphere.exporter.core.service.process.Processor;
import edu.asu.diging.citesphere.messages.KafkaTopics;
import edu.asu.diging.citesphere.messages.model.KafkaExportReturnMessage;
import edu.asu.diging.citesphere.messages.model.KafkaJobMessage;
import edu.asu.diging.citesphere.messages.model.ResponseCode;
import edu.asu.diging.citesphere.messages.model.Status;

@Service
@Transactional
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
    
    @Autowired
    private IKafkaRequestProducer requestProducer;
    
    @Autowired
    private DownloadTaskRepository downloadTaskRepo;
    
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
        IDownloadTask downloadTask = new DownloadTask();
        
        try {
            info = connector.getJobInfo(message.getId());
        } catch (CitesphereCommunicationException e) {
            // TODO: send back error message
            logger.error("Couldn't connect to citesphere", e);
            sendMessage(message.getId(), null, Status.FAILED, ResponseCode.X20);
            
            downloadTask.setStatus(Status.FAILED);
            downloadTask.setResponseCode(ResponseCode.X20);
            downloadTaskRepo.save((DownloadTask)downloadTask);
            return;
        }

        downloadTask.setCitesphereTaskId(info.getTaskId());
        downloadTask.setUsername(info.getUsername());
        downloadTask.setContentType(info.getExportType().getContentType());
        
        Processor processor = processors.get(info.getExportType());
        if (processor == null) {
            // TODO: send back error message
            logger.error("No processor available for " + info.getExportType());
            sendMessage(message.getId(), info.getUsername(), Status.FAILED, ResponseCode.X30);
            
            downloadTask.setStatus(Status.FAILED);
            downloadTask.setResponseCode(ResponseCode.X30);
            downloadTaskRepo.save((DownloadTask)downloadTask);
            return;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd--HH-mm-ss");
        String time = LocalDateTime.now().format(formatter);
        String filename = "export-" + time + "." + processor.getFileExtension();
        
        downloadTask.setFilename(filename);
        BufferedWriter writer = null;
        try {
            try {
                writer = createWriter(info.getUsername(), info.getTaskId(), processor, filename);
            } catch (ExportFailedException e) {
                logger.error("Could not create writer. Export failed for " + info.getTaskId());
                sendMessage(message.getId(), info.getUsername(), Status.FAILED, ResponseCode.X00);
                
                downloadTask.setStatus(Status.FAILED);
                downloadTask.setResponseCode(ResponseCode.X00);
                downloadTaskRepo.save((DownloadTask)downloadTask);
                return;
            }
        } catch (IOException e) {
            logger.error("Creating writer failed for " + info.getTaskId());
            sendMessage(message.getId(), info.getUsername(), Status.FAILED, ResponseCode.X00);
                
            downloadTask.setStatus(Status.FAILED);
            downloadTask.setResponseCode(ResponseCode.X00);
            downloadTaskRepo.save((DownloadTask)downloadTask);
            return;
        }
        
        ExportWriter exportWriter;
        try {
            exportWriter = processor.getWriter(writer);
        } catch (IOException e) {
            logger.error("Couldn't create writer.", e);
            sendMessage(message.getId(), info.getUsername(), Status.FAILED, ResponseCode.X00);
            
            downloadTask.setStatus(Status.FAILED);
            downloadTask.setResponseCode(ResponseCode.X00);
            downloadTaskRepo.save((DownloadTask)downloadTask);
            return;
        }
        
        // get citations and write
        CitationIterator citIterator;
        try {
            citIterator = citationManager.getCitations(info);
        } catch (ZoteroHttpStatusException e) {
            logger.error("Couldn't retrieve citation from Zotero.", e);
            sendMessage(message.getId(), info.getUsername(), Status.FAILED, ResponseCode.X40);
            
            downloadTask.setStatus(Status.FAILED);
            downloadTask.setResponseCode(ResponseCode.X40);
            downloadTaskRepo.save((DownloadTask)downloadTask);
            return;
        }
        
        boolean issueWritingRow = false;
        while(citIterator.hasNext()) {
            try {
                exportWriter.writeRow(citIterator.next(), citIterator.getGrouping());
            } catch (IOException e) {
                logger.error("Couldn't write citation.", e);
                issueWritingRow = true;
                continue;
            }
        }
        
        if (issueWritingRow) {
            sendMessage(message.getId(), info.getUsername(), Status.FAILED, ResponseCode.W10);
        }
        
        try {
            exportWriter.cleanUp();
        } catch (IOException e) {
            logger.error("Couldn't close writer.", e);
            sendMessage(message.getId(), info.getUsername(), Status.FAILED, ResponseCode.W10);
            
            downloadTask.setStatus(Status.FAILED);
            downloadTask.setResponseCode(ResponseCode.W10);
            downloadTaskRepo.save((DownloadTask)downloadTask);
            return;
        }
        
        String path = storageManager.getFolderPath(downloadTask.getUsername(), downloadTask.getCitesphereTaskId());
        File file = new File(path + File.separator + downloadTask.getFilename());
        
        downloadTask.setFileSize(file.length());
        downloadTask.setStatus(Status.SUCCESS);
        downloadTask.setResponseCode(ResponseCode.S00);
        downloadTaskRepo.save((DownloadTask)downloadTask);
        sendMessage(message.getId(), info.getUsername(), Status.DONE, ResponseCode.S00);
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
    
    private void sendMessage(String jobId, String username, Status status, ResponseCode code) {
        KafkaExportReturnMessage returnMessage = new KafkaExportReturnMessage(username, jobId);
        returnMessage.setStatus(status);
        returnMessage.setCode(code);
        try {
            requestProducer.sendRequest(returnMessage, KafkaTopics.REFERENCES_EXPORT_DONE_TOPIC);
        } catch (MessageCreationException e) {
            // FIXME handle this case
            logger.error("Exception sending message.", e);
        }
    }
}
