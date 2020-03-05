package edu.asu.diging.citesphere.exporter.core.data;

import org.springframework.data.repository.PagingAndSortingRepository;

import edu.asu.diging.citesphere.exporter.core.model.impl.App;

public interface AppRepository extends PagingAndSortingRepository<App, String> {

}
