package edu.asu.diging.citesphere.exporter.core.service.impl;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import edu.asu.diging.citesphere.exporter.core.data.AccessTokenRepository;
import edu.asu.diging.citesphere.exporter.core.model.IAccessToken;
import edu.asu.diging.citesphere.exporter.core.model.IApp;
import edu.asu.diging.citesphere.exporter.core.model.impl.AccessToken;
import edu.asu.diging.citesphere.exporter.core.service.ITokenManager;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
@PropertySource("classpath:/config.properties")
public class TokenManager implements ITokenManager {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private AccessTokenRepository tokenRepo;

    @Value("${_exporter_jwt_secret}")
    private String secret;

    private SecretKey key;

    @PostConstruct
    public void init() {
        key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.asu.diging.citesphere.exporter.core.service.impl.ITokenManager#
     * createToken(edu.asu.diging.citesphere.exporter.core.model.IApp,
     * java.lang.String)
     */
    @Override
    public String createToken(IApp app, String username) {
        IAccessToken token = new AccessToken();
        token.setApp(app);
        token.setCreatedBy(username);
        token.setCreatedOn(OffsetDateTime.now());
        token = tokenRepo.save((AccessToken) token);

        Map<String, Object> claims = new HashMap<>();
        claims.put("tokenId", token.getId());
        return Jwts.builder().setSubject(app.getId()).addClaims(claims).signWith(key).compact();
    }

    @Override
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException ex) {
            logger.error("Could not validate token.", ex);
            return false;
        }
    }
}
