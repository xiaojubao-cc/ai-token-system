package com.ai.system.service.impl;

import cn.hutool.core.lang.Assert;
import com.ai.system.config.security.JwtBlacklistService;
import com.ai.system.model.dto.token.TokenDO;
import com.ai.system.exception.ServiceException;
import com.ai.system.exception.enums.UserErrorCodeConstants;
import com.ai.system.mapper.UserMapper;
import com.ai.system.model.entity.User;
import com.ai.system.service.AuthService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {
    private static final AuthorizationGrantType PASSWORD = new AuthorizationGrantType("password");

    @Resource
    private AuthenticationManager authenticationManager;

    @Resource
    private RegisteredClientRepository clientRepository;

    @Resource
    private OAuth2AuthorizationService authorizationService;

    @Resource
    private OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;

    @Resource
    private JwtBlacklistService blacklistService;

    @Resource
    private UserMapper userMapper;

    @Resource
    private AuthorizationServerSettings authorizationServerSettings;

    public TokenDO login(String username, String password, Boolean rememberMe) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        Assert.notNull(user, () -> new ServiceException(UserErrorCodeConstants.USER_NOT_EXISTS));
        Assert.equals(user.getStatus().toString(), "1",
                () -> new ServiceException(UserErrorCodeConstants.USER_SUSPENDED));

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));

        String clientId = Boolean.TRUE.equals(rememberMe) ? "ai-client-remember" : "ai-client";
        RegisteredClient client = clientRepository.findByClientId(clientId);
        Set<String> scopes = client.getScopes();

        OAuth2Authorization.Builder builder = OAuth2Authorization.withRegisteredClient(client)
                .id(UUID.randomUUID().toString())
                .principalName(username)
                .authorizationGrantType(PASSWORD)
                .authorizedScopes(scopes)
                .attribute(Principal.class.getName(), auth);

        OAuth2TokenContext tokenContext = DefaultOAuth2TokenContext.builder()
                .registeredClient(client)
                .principal(auth)
                .authorizationServerContext(serverContext())
                .authorizationGrantType(PASSWORD)
                .authorizedScopes(scopes)
                .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                .build();
        OAuth2Token generatedAccessToken = tokenGenerator.generate(tokenContext);
        OAuth2AccessToken accessToken;
        if (generatedAccessToken instanceof OAuth2AccessToken at) {
            accessToken = at;
        } else {
            accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
                    generatedAccessToken.getTokenValue(),
                    generatedAccessToken.getIssuedAt(),
                    generatedAccessToken.getExpiresAt());
        }

        tokenContext = DefaultOAuth2TokenContext.builder()
                .registeredClient(client)
                .principal(auth)
                .authorizationServerContext(serverContext())
                .authorizationGrantType(PASSWORD)
                .authorizedScopes(scopes)
                .tokenType(OAuth2TokenType.REFRESH_TOKEN)
                .build();
        OAuth2Token generatedRefreshToken = tokenGenerator.generate(tokenContext);
        OAuth2RefreshToken refreshToken;
        if (generatedRefreshToken instanceof OAuth2RefreshToken rt) {
            refreshToken = rt;
        } else {
            refreshToken = new OAuth2RefreshToken(generatedRefreshToken.getTokenValue(),
                    generatedRefreshToken.getIssuedAt(),
                    generatedRefreshToken.getExpiresAt());
        }

        OAuth2Authorization authorization = builder
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
        authorizationService.save(authorization);

        long expiresIn = accessToken.getExpiresAt() != null
                ? accessToken.getExpiresAt().getEpochSecond() - System.currentTimeMillis() / 1000
                : 1800;

        return TokenDO.builder()
                .accessToken(accessToken.getTokenValue())
                .refreshToken(refreshToken.getTokenValue())
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .build();
    }

    public void logout(String refreshTokenValue) {
        OAuth2Authorization authorization = authorizationService.findByToken(
                refreshTokenValue, OAuth2TokenType.REFRESH_TOKEN);
        if (authorization == null) {
            return;
        }
        OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization.getAccessToken();
        if (accessToken != null && accessToken.getToken().getExpiresAt() != null) {
            try {
                com.nimbusds.jwt.JWT jwt = com.nimbusds.jwt.JWTParser.parse(
                        accessToken.getToken().getTokenValue());
                String jti = jwt.getJWTClaimsSet().getJWTID();
                if (jti != null) {
                    blacklistService.add(jti, accessToken.getToken().getExpiresAt());
                }
            } catch (Exception ignored) {
            }
        }
        authorizationService.remove(authorization);
    }

    private AuthorizationServerContext serverContext() {
        return new AuthorizationServerContext() {
            @Override
            public String getIssuer() {
                return authorizationServerSettings.getIssuer();
            }
            @Override
            public AuthorizationServerSettings getAuthorizationServerSettings() {
                return authorizationServerSettings;
            }
        };
    }
}
