package edu.asu.diging.citesphere.exporter.core.data;

import org.springframework.data.repository.PagingAndSortingRepository;

import edu.asu.diging.citesphere.exporter.core.model.impl.AccessToken;

public interface AccessTokenRepository extends PagingAndSortingRepository<AccessToken, String> {

}
