package edu.asu.diging.citesphere.exporter.core.service;

import java.io.IOException;
import java.net.URL;

import edu.asu.diging.citesphere.exporter.core.exception.FileStorageException;

public interface IFileStorageManager {

    void saveFile(String username, String jobId, String filename, byte[] bytes) throws FileStorageException;

    String getFolderPath(String username, String jobId);

    boolean deleteFile(String username, String jobId, String filename, boolean deleteEmptyFolders);

    byte[] getFileContentFromUrl(URL url) throws IOException;

}