package edu.asu.diging.citesphere.exporter.core.service.impl;

import java.io.IOException;
import java.util.Arrays;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.asu.diging.citesphere.exporter.core.exception.CitesphereCommunicationException;
import edu.asu.diging.citesphere.exporter.core.service.CitesphereHeaders;
import edu.asu.diging.citesphere.exporter.core.service.ICitesphereConnector;

@Service
@PropertySource("classpath:/config.properties")
public class CitesphereConnector implements ICitesphereConnector {
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    @Value("${_citesphere_base_uri}")
    private String citesphereBaseUri;
    
    @Value("${_citesphere_client_key}")
    private String citesphereClientKey;
    
    @Value("${_citesphere_client_secret}")
    private String citesphereClientSecret;
    
    @Value("${_citesphere_download_path}")
    private String downloadPath;
    
    private RestTemplate restTemplate;
    
    private String accessToken;
    
    @PostConstruct
    public void init() {
        restTemplate = new RestTemplate();
        if (!citesphereBaseUri.endsWith("/")) {
            citesphereBaseUri += "/";
        }
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(citesphereBaseUri));
        ResponseErrorHandler errorHandler = new DefaultResponseErrorHandler() {

            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                HttpStatus status = response.getStatusCode();
                if (HttpStatus.Series.valueOf(status) == HttpStatus.Series.SUCCESSFUL || status == HttpStatus.UNAUTHORIZED) {
                    return false;
                }
                return true;
            }
            
        };
        restTemplate.setErrorHandler(errorHandler);
    }

    /* (non-Javadoc)
     * @see edu.asu.diging.citesphere.importer.core.service.impl.ICitesphereConnector#getZoteroInfo(java.lang.String)
     */
    @Override
    public JobInfo getJobInfo(String apiToken) throws CitesphereCommunicationException {
        @SuppressWarnings("unchecked")
        ResponseEntity<String> response = (ResponseEntity<String>) makeApiCall("api/v1/job/info", apiToken, String.class);
        HttpStatus status = response.getStatusCode();
        
        JobInfo info = null;
        if (status == HttpStatus.OK) {
            String responseBody = response.getBody();
            ObjectMapper mapper = new ObjectMapper();
            try {
                info = mapper.readValue(responseBody, JobInfo.class);
            } catch (IOException e) {
                throw new CitesphereCommunicationException("Could not understand returned message: " + responseBody, e);
            }
        } else {
            throw new CitesphereCommunicationException("Could not communicate with Citesphere properly. Got " + status);
        }
        
        return info;
    }
    
    private ResponseEntity<?> makeApiCall(String url, String apiToken, Class<?> responseType) throws CitesphereCommunicationException {
        HttpEntity<String> entity = buildHeaders(apiToken);
        ResponseEntity<?> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.GET, entity, responseType);
        } catch (RestClientException ex) {
            throw new CitesphereCommunicationException("Could not understand server.", ex);
        }
        HttpStatus status = response.getStatusCode();
        
        if (status == HttpStatus.UNAUTHORIZED) {
            String responseBody = response.getBody().toString();
            if (!refreshToken(responseBody)) {
                throw new CitesphereCommunicationException("Could not understand returned error message: " + responseBody);
            }
            
            // let's try again after getting a new OAuth token
            response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        }
        return response;
    }

    private HttpEntity<String> buildHeaders(String apiToken) {
        if (accessToken == null) {
            accessToken = getAccessToken();
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.put(CitesphereHeaders.CITESPHERE_API_TOKEN, Arrays.asList(apiToken));
        HttpEntity<String> entity = new HttpEntity<>("body", headers);
        return entity;
    }
    
    protected String getAccessToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(citesphereClientKey, citesphereClientSecret);
        HttpEntity<String> entity = new HttpEntity<>("body", headers);

        // not working? FIXME
        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange("api/v1/oauth/token?grant_type=client_credentials", HttpMethod.POST, entity, String.class);
        } catch (ResourceAccessException ex) {
            logger.error("Could not get token.", ex);
            return null;
        }
        
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = null;
        try {
            node = mapper.readTree(response.getBody());
        } catch (IOException e) {
            logger.error("Could not read JSON message.", e);
            return null;
        }
        
        return node.get("access_token").asText();
    }
    
    private boolean refreshToken(String errorMessage) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = null;
        try {
            node = mapper.readTree(errorMessage);
        } catch (IOException e) {
            logger.error("Could not read JSON message.", e);
            return false;
        }
        
        if (node.get("error") != null && node.hasNonNull("error") && node.get("error").asText().equals("invalid_token")) {
            accessToken = getAccessToken();
            return true;
        }
        
        return false;
    }
}
