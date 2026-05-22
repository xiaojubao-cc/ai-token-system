package com.ai.system.service;

import com.ai.system.model.dto.token.TokenDO;

public interface AuthService {

    public TokenDO login(String username, String password, Boolean rememberMe);

    public void logout(String refreshTokenValue);
}
