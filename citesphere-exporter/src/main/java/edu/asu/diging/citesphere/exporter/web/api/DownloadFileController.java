package edu.asu.diging.citesphere.exporter.web.api;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import edu.asu.diging.citesphere.exporter.core.data.DownloadTaskRepository;
import edu.asu.diging.citesphere.exporter.core.model.IDownloadTask;
import edu.asu.diging.citesphere.exporter.core.model.impl.DownloadTask;
import edu.asu.diging.citesphere.exporter.core.service.IFileStorageManager;
import edu.asu.diging.citesphere.exporter.core.service.ITokenManager;

@Controller
public class DownloadFileController {
    
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ITokenManager tokenManager;
    
    @Autowired
    private DownloadTaskRepository taskRepo;
    
    @Autowired
    private IFileStorageManager storageManager;
    
    @RequestMapping("/api/v1/task/{id}/download")
    public ResponseEntity<String> get(@PathVariable String id, HttpServletResponse response, @RequestHeader HttpHeaders headers) {
        List<String> authHeaders = headers.get(HttpHeaders.AUTHORIZATION);
        if (authHeaders == null || authHeaders.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        
        boolean isValidToken = false;
        for (String authHeader : authHeaders) {
            isValidToken = tokenManager.validateToken(authHeader);
            if (isValidToken) {    
                break;
            }
        }
        
        if (!isValidToken) {
            return new ResponseEntity<String>(HttpStatus.UNAUTHORIZED);
        }
        
        Optional<DownloadTask> taskOptional = taskRepo.findFirstByCitesphereTaskId(id);
        if (!taskOptional.isPresent()) {
            return new ResponseEntity<String>(HttpStatus.NOT_FOUND);
        }
        
        IDownloadTask task = taskOptional.get();
        String path = storageManager.getFolderPath(task.getUsername(), task.getCitesphereTaskId());
        byte[] fileContent;
        try {
            fileContent = storageManager.getFileContentFromUrl(new URL("file:" + path + File.separator + task.getFilename()));
        } catch (IOException e) {
            logger.error("Could not read file.", e);
            return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        response.setContentType(task.getContentType());
        response.setContentLength(new Long(task.getFileSize()).intValue());
        response.setHeader("Content-disposition", "filename=\"" + task.getFilename() + "\""); 
        try {
            response.getOutputStream().write(fileContent);
            response.getOutputStream().close();
        } catch (IOException e) {
            logger.error("Could not create repsonse object.", e);
            return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<String>(HttpStatus.OK);
    }
}
