package edu.asu.diging.citesphere.exporter.core.data;

import java.util.Optional;

import org.springframework.data.repository.PagingAndSortingRepository;

import edu.asu.diging.citesphere.exporter.core.model.impl.DownloadTask;

public interface DownloadTaskRepository extends PagingAndSortingRepository<DownloadTask, String> {

    Optional<DownloadTask> findFirstByCitesphereTaskId(String citesphereTaskId);
}
