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
import java.util.NoSuchElementException;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.util.CloseableIterator;
import org.springframework.stereotype.Service;

import edu.asu.diging.citesphere.exporter.core.data.DownloadTaskRepository;
import edu.asu.diging.citesphere.exporter.core.exception.CitesphereCommunicationException;
import edu.asu.diging.citesphere.exporter.core.exception.ExportFailedException;
import edu.asu.diging.citesphere.exporter.core.exception.FileStorageException;
import edu.asu.diging.citesphere.exporter.core.exception.GroupDoesNotExistException;
import edu.asu.diging.citesphere.exporter.core.exception.MessageCreationException;
import edu.asu.diging.citesphere.exporter.core.exception.OutOfDateException;
import edu.asu.diging.citesphere.exporter.core.exception.ZoteroHttpStatusException;
import edu.asu.diging.citesphere.exporter.core.kafka.IKafkaRequestProducer;
import edu.asu.diging.citesphere.exporter.core.model.IDownloadTask;
import edu.asu.diging.citesphere.exporter.core.model.impl.DownloadTask;
import edu.asu.diging.citesphere.exporter.core.service.ICitationManager;
import edu.asu.diging.citesphere.exporter.core.service.ICitesphereConnector;
import edu.asu.diging.citesphere.exporter.core.service.IExportProcessor;
import edu.asu.diging.citesphere.exporter.core.service.IFileStorageManager;
import edu.asu.diging.citesphere.exporter.core.service.process.ExportType;
import edu.asu.diging.citesphere.exporter.core.service.process.ExportWriter;
import edu.asu.diging.citesphere.exporter.core.service.process.Processor;
import edu.asu.diging.citesphere.messages.KafkaTopics;
import edu.asu.diging.citesphere.messages.model.KafkaExportReturnMessage;
import edu.asu.diging.citesphere.messages.model.KafkaJobMessage;
import edu.asu.diging.citesphere.messages.model.ResponseCode;
import edu.asu.diging.citesphere.messages.model.Status;
import edu.asu.diging.citesphere.model.bib.ICitation;
import edu.asu.diging.citesphere.model.bib.ICitationCollection;
import edu.asu.diging.citesphere.model.bib.ICitationGroup;

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
            updateTaskAndSendMessage(message, Status.FAILED, ResponseCode.X20, info, downloadTask);
            return;
        }
        
        
        Optional<DownloadTask> taskOptional = downloadTaskRepo.findFirstByCitesphereTaskId(info.getTaskId());
        if (taskOptional.isPresent()) {
            // if this is a retry due to syncing, there will already be a task object
            downloadTask = taskOptional.get();
        } 


        downloadTask.setCitesphereTaskId(info.getTaskId());
        downloadTask.setUsername(info.getUsername());
        downloadTask.setContentType(info.getExportType().getContentType());

        Processor processor = processors.get(info.getExportType());
        if (processor == null) {
            // TODO: send back error message
            logger.error("No processor available for " + info.getExportType());
            updateTaskAndSendMessage(message, Status.FAILED, ResponseCode.X30, info, downloadTask);
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
                logger.error("Could not create writer. Export failed for " + info.getTaskId(), e);
                updateTaskAndSendMessage(message, Status.FAILED, ResponseCode.X00, info, downloadTask);
                return;
            }
        } catch (IOException e) {
            logger.error("Creating writer failed for " + info.getTaskId(), e);
            updateTaskAndSendMessage(message, Status.FAILED, ResponseCode.X00, info, downloadTask);
            return;
        }

        ExportWriter exportWriter;
        try {
            exportWriter = processor.getWriter(writer);
        } catch (IOException e) {
            logger.error("Couldn't create writer.", e);
            updateTaskAndSendMessage(message, Status.FAILED, ResponseCode.X00, info, downloadTask);
            return;
        }

        // get citations and write
        CloseableIterator<ICitation> citIterator;
        try {
            citIterator = citationManager.getAllGroupItems(info);
        } catch (ZoteroHttpStatusException e) {
            logger.error("Couldn't retrieve citation from Zotero.", e);
            updateTaskAndSendMessage(message, Status.FAILED, ResponseCode.X40, info, downloadTask);
            return;
        } catch (GroupDoesNotExistException e) {
            logger.error("Group does not exist: " + info.getGroupId(), e);
            updateTaskAndSendMessage(message, Status.FAILED, ResponseCode.X50, info, downloadTask);
            return;
        } catch (OutOfDateException e) {
            logger.warn("Group is out of date, wait for syncing.");
            updateTaskAndSendMessage(message, Status.SYNCING_RETRY, ResponseCode.P10, info, downloadTask);
            
            // delete unused file
            deleteFile(info.getUsername(), downloadTask.getId(), filename);
            return;
        }

        boolean issueWritingRow = false;
        while (citIterator.hasNext()) {
            try {
                ICitation citation = citIterator.next();
                ICitationCollection collection = null;
                if (citation.getCollections() != null && !citation.getCollections().isEmpty()) {
                    // lets take first for now
                    collection = citationManager.getCollection(info);
                }
                ICitationGroup group = citationManager.getGroup(info);
                exportWriter.writeRow(citation, group, collection);
            } catch (IOException e) {
                logger.error("Couldn't write citation.", e);
                issueWritingRow = true;
                continue;
            } catch (NoSuchElementException e) {
                sendMessage(message.getId(), info.getUsername(), Status.FAILED, ResponseCode.X00);
                break;
            }
        }

        if (issueWritingRow) {
            sendMessage(message.getId(), info.getUsername(), Status.FAILED, ResponseCode.W10);
        }

        try {
            exportWriter.cleanUp();
        } catch (IOException e) {
            logger.error("Couldn't close writer.", e);
            updateTaskAndSendMessage(message, Status.FAILED, ResponseCode.W10, info, downloadTask);
            return;
        }

        String path = storageManager.getFolderPath(downloadTask.getUsername(), downloadTask.getCitesphereTaskId());
        File file = new File(path + File.separator + downloadTask.getFilename());

        downloadTask.setFileSize(file.length());
        downloadTask.setStatus(Status.SUCCESS);
        downloadTask.setResponseCode(ResponseCode.S00);
        downloadTaskRepo.save((DownloadTask) downloadTask);
        sendMessage(message.getId(), info.getUsername(), Status.DONE, ResponseCode.S00);
    }

    private void updateTaskAndSendMessage(KafkaJobMessage message, Status status, ResponseCode code, JobInfo info,
            IDownloadTask downloadTask) {
        sendMessage(message.getId(), info.getUsername(), status, code);

        downloadTask.setStatus(status);
        downloadTask.setResponseCode(code);
        downloadTaskRepo.save((DownloadTask) downloadTask);
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

        return Files.newBufferedWriter(Paths.get(filePath));
    }

    private void deleteFile(String username, String taskId, String filename) {
        storageManager.deleteFile(username, taskId, filename, false);
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
