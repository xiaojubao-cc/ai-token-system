package com.ai.system.config.security;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class RedisOAuth2AuthorizationService implements OAuth2AuthorizationService {

    private static final String AUTH_PREFIX = "oauth2:auth:";
    private static final String TOKEN_PREFIX = "oauth2:token:";

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void save(OAuth2Authorization authorization) {
        String id = authorization.getId();
        Duration ttl = calcTtl(authorization);
        redisTemplate.opsForValue().set(AUTH_PREFIX + id, authorization, ttl);

        if (authorization.getAccessToken() != null) {
            String tokenValue = authorization.getAccessToken().getToken().getTokenValue();
            redisTemplate.opsForValue().set(TOKEN_PREFIX + "access:" + tokenValue, id, ttl);
        }
        if (authorization.getRefreshToken() != null) {
            String tokenValue = authorization.getRefreshToken().getToken().getTokenValue();
            redisTemplate.opsForValue().set(TOKEN_PREFIX + "refresh:" + tokenValue, id, ttl);
        }
    }

    @Override
    public void remove(OAuth2Authorization authorization) {
        String id = authorization.getId();
        if (authorization.getAccessToken() != null) {
            String tokenValue = authorization.getAccessToken().getToken().getTokenValue();
            redisTemplate.delete(TOKEN_PREFIX + "access:" + tokenValue);
        }
        if (authorization.getRefreshToken() != null) {
            String tokenValue = authorization.getRefreshToken().getToken().getTokenValue();
            redisTemplate.delete(TOKEN_PREFIX + "refresh:" + tokenValue);
        }
        redisTemplate.delete(AUTH_PREFIX + id);
    }

    @Override
    public OAuth2Authorization findById(String id) {
        return (OAuth2Authorization) redisTemplate.opsForValue().get(AUTH_PREFIX + id);
    }

    @Override
    public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
        String type = tokenType == null || tokenType.getValue() == null
                ? "access" : tokenType.getValue();
        String authId = (String) redisTemplate.opsForValue().get(TOKEN_PREFIX + type + ":" + token);
        if (authId == null) {
            return null;
        }
        return findById(authId);
    }

    private Duration calcTtl(OAuth2Authorization authorization) {
        Instant expireAt = null;
        if (authorization.getAccessToken() != null && authorization.getAccessToken().getToken().getExpiresAt() != null) {
            expireAt = authorization.getAccessToken().getToken().getExpiresAt();
        }
        if (authorization.getRefreshToken() != null && authorization.getRefreshToken().getToken().getExpiresAt() != null) {
            Instant re = authorization.getRefreshToken().getToken().getExpiresAt();
            if (expireAt == null || re.isAfter(expireAt)) {
                expireAt = re;
            }
        }
        if (expireAt == null) {
            return Duration.ofHours(1);
        }
        Duration ttl = Duration.between(Instant.now(), expireAt);
        return ttl.isNegative() ? Duration.ofHours(1) : ttl;
    }
}
