package com.ai.system.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_token_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String businessName;

    private String password;

    private String phone;

    private String email;

    private String role;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
