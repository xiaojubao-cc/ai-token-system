package com.ai.system.exception.enums;

import com.ai.system.exception.ErrorCode;

public interface UserErrorCodeConstants {

    ErrorCode USER_NOT_EXISTS = new ErrorCode(1_001_002_000, "用户不存在");
    ErrorCode USERNAME_EXISTS = new ErrorCode(1_001_002_001, "用户名已存在");
    ErrorCode PASSWORD_NOT_MATCH = new ErrorCode(1_001_002_002, "两次密码不一致");
    ErrorCode OLD_PASSWORD_ERROR = new ErrorCode(1_001_002_004, "当前密码不正确");
    ErrorCode USERNAME_EMAIL_MISMATCH = new ErrorCode(1_001_002_003, "用户名与邮箱地址不匹配");
    ErrorCode USER_SUSPENDED = new ErrorCode(1_001_002_005, "用户已被停止服务");

}
