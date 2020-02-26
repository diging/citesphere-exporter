package edu.asu.diging.citesphere.exporter.core.service;

import edu.asu.diging.citesphere.exporter.core.model.IApp;

public interface ITokenManager {

    String createToken(IApp app, String username);

    boolean validateToken(String token);

}