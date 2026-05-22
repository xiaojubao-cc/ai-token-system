package com.ai.system.model.dto.token;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenDO {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
}
