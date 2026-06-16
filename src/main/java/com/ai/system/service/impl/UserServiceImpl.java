package com.ai.system.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.ai.system.controller.auth.vo.ForgetPasswordVO;
import com.ai.system.controller.auth.vo.UpdatePasswordVO;
import com.ai.system.controller.auth.vo.UserRegisterVO;
import com.ai.system.controller.user.vo.ApiKeyCreateVO;
import com.ai.system.controller.user.vo.UserCreateVO;
import com.ai.system.controller.user.vo.UserPageQueryVO;
import com.ai.system.controller.user.vo.UserUpdateVO;
import com.ai.system.exception.ErrorCode;
import com.ai.system.exception.ServiceException;
import com.ai.system.exception.enums.UserErrorCodeConstants;
import com.ai.system.mapper.ApiKeyMapper;
import com.ai.system.mapper.UserMapper;
import com.ai.system.model.dto.user.UserApiKeyDO;
import com.ai.system.model.dto.user.UserDetailDO;
import com.ai.system.model.dto.user.UserPageResultDO;
import com.ai.system.model.entity.ApiKey;
import com.ai.system.model.entity.User;
import com.ai.system.config.security.JwtBlacklistService;
import com.ai.system.config.properties.TyyProperties;
import com.ai.system.model.pojo.TyyApiKeyResponse;
import com.ai.system.model.pojo.TyyResponse;
import com.ai.system.service.UserService;
import com.ai.system.util.mail.MailUtil;
import com.ai.system.util.tyy.TyySignUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private ApiKeyMapper apiKeyMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private MailUtil mailService;

    @Resource
    private JwtBlacklistService jwtBlacklistService;

    @Resource
    private TyySignUtil tyySignUtil;

    @Resource
    private TyyProperties tyyProperties;

    @Resource
    private HttpServletRequest request;

    @Override
    public Long register(UserRegisterVO req) {
        Assert.equals(req.getPassword(), req.getConfirmPassword(),
                () -> new ServiceException(UserErrorCodeConstants.PASSWORD_NOT_MATCH));

        User exist = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, req.getUsername()));
        Assert.isNull(exist, () -> new ServiceException(UserErrorCodeConstants.USERNAME_EXISTS));
        boolean admin = req.getUsername().trim().equalsIgnoreCase("admin");
        User user = new User();
        user.setUsername(req.getUsername());
        user.setBusinessName(req.getBusinessName());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setPhone(req.getPhone());
        user.setEmail(req.getEmail());
        user.setRole(admin ? "ADMIN" : "USER");
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user.getId();
    }

    @Override
    public Boolean forgetPassword(ForgetPasswordVO req) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, req.getUsername()));
        Assert.notNull(user, () -> new ServiceException(UserErrorCodeConstants.USERNAME_EMAIL_MISMATCH));
        Assert.equals(user.getEmail(), req.getEmail(),
                () -> new ServiceException(UserErrorCodeConstants.USERNAME_EMAIL_MISMATCH));

        String newPassword = RandomUtil.randomString(12);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        mailService.sendPasswordReset(user.getEmail(), user.getUsername(), newPassword);
        return true;
    }

    @Override
    public Boolean changePassword(UpdatePasswordVO updatePasswordReq) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getId, updatePasswordReq.getId()));
        Assert.notNull(user, () -> new ServiceException(UserErrorCodeConstants.USERNAME_EMAIL_MISMATCH));

        if (!passwordEncoder.matches(updatePasswordReq.getOldPassword(), user.getPassword())) {
            throw new ServiceException(UserErrorCodeConstants.OLD_PASSWORD_ERROR);
        }

        user.setPassword(passwordEncoder.encode(updatePasswordReq.getNewPassword()));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        blacklistCurrentToken();
        return true;
    }

    @Override
    public Long createUser(UserCreateVO req) {
        User exist = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, req.getUsername()));
        Assert.isNull(exist, () -> new ServiceException(UserErrorCodeConstants.USERNAME_EXISTS));

        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setUserId(req.getUserId());
        user.setAccountId(req.getAccountId());
        user.setAccessKey(req.getAccessKey());
        user.setSecurityKey(req.getSecurityKey());
        user.setRole(req.getRole() != null ? req.getRole() : "USER");
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user.getId();
    }

    @Override
    public Boolean updateUser(UserUpdateVO req) {
        User user = userMapper.selectById(req.getId());
        Assert.notNull(user, () -> new ServiceException(UserErrorCodeConstants.USERNAME_EMAIL_MISMATCH));

        if (StrUtil.isNotBlank(req.getUsername())) {
            user.setUsername(req.getUsername());
        }
        if (StrUtil.isNotBlank(req.getBusinessName())) {
            user.setBusinessName(req.getBusinessName());
        }
        if (StrUtil.isNotBlank(req.getPassword())) {
            user.setPassword(passwordEncoder.encode(req.getPassword()));
        }
        if (req.getEmail() != null) {
            user.setEmail(req.getEmail());
        }
        if (req.getPhone() != null) {
            user.setPhone(req.getPhone());
        }
        if (req.getUserId() != null) {
            user.setUserId(req.getUserId());
        }
        if (req.getAccountId() != null) {
            user.setAccountId(req.getAccountId());
        }
        if (req.getAccessKey() != null) {
            user.setAccessKey(req.getAccessKey());
        }
        if (req.getSecurityKey() != null) {
            user.setSecurityKey(req.getSecurityKey());
        }
        if (StrUtil.isNotBlank(req.getRole())) {
            user.setRole(req.getRole());
        }
        if (req.getStatus() != null) {
            user.setStatus(req.getStatus());
        }
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        return true;
    }

    @Override
    public UserPageResultDO pageQuery(UserPageQueryVO query) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        // 如果传了 apikey，先查出拥有该 apikey 的用户 ID
        if (StrUtil.isNotBlank(query.getApikey())) {
            List<ApiKey> matchedKeys = apiKeyMapper.selectList(new LambdaQueryWrapper<ApiKey>()
                    .like(ApiKey::getApikey, query.getApikey()));
            if (matchedKeys.isEmpty()) {
                UserPageResultDO empty = new UserPageResultDO();
                empty.setTotal(0L);
                empty.setPage(query.getPage());
                empty.setPageSize(query.getPageSize());
                empty.setList(Collections.emptyList());
                return empty;
            }
            List<Long> userIds = matchedKeys.stream()
                    .map(ApiKey::getUserId)
                    .distinct()
                    .collect(Collectors.toList());
            wrapper.in(User::getId, userIds);
        }

        if (StrUtil.isNotBlank(query.getUsername())) {
            wrapper.like(User::getUsername, query.getUsername());
        }
        if (StrUtil.isNotBlank(query.getStartTime())) {
            wrapper.ge(User::getCreateTime, query.getStartTime());
        }
        if (StrUtil.isNotBlank(query.getEndTime())) {
            wrapper.le(User::getCreateTime, query.getEndTime());
        }
        wrapper.orderByDesc(User::getCreateTime);

        Page<User> page = userMapper.selectPage(
                new Page<>(query.getPage(), query.getPageSize()), wrapper);

        List<UserDetailDO> list = enrichUserDetails(page.getRecords());

        UserPageResultDO result = new UserPageResultDO();
        result.setTotal(page.getTotal());
        result.setPage(page.getCurrent());
        result.setPageSize(page.getSize());
        result.setList(list);
        return result;
    }

    @Override
    public List<UserApiKeyDO> getUserApiKeys(Long userId) {
        List<ApiKey> keys = apiKeyMapper.selectList(new LambdaQueryWrapper<ApiKey>()
                .eq(ApiKey::getUserId, userId)
                .orderByDesc(ApiKey::getCreateTime));

        return keys.stream().map(ak -> {
            UserApiKeyDO dto = new UserApiKeyDO();
            dto.setId(ak.getId());
            dto.setUserId(ak.getUserId());
            dto.setApikey(ak.getApikey());
            dto.setSecretKey(ak.getSecretKey());
            dto.setStatus(ak.getUseStatus());
            if (ak.getCreateTime() != null) {
                dto.setCreateTime(ak.getCreateTime().toString());
            }
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public void addUserApiKey(Long userId, ApiKeyCreateVO req) {
        User user = userMapper.selectById(userId);
        Assert.notNull(user, () -> new ServiceException(UserErrorCodeConstants.USER_NOT_EXISTS));

        // 调用天翼云接口创建 API Key
        String accessKey = StrUtil.isNotBlank(user.getAccessKey()) ? user.getAccessKey() : tyyProperties.getAccessKey();
        String securityKey = StrUtil.isNotBlank(user.getSecurityKey()) ? user.getSecurityKey() : tyyProperties.getSecurityKey();
        TyyResponse resp = tyySignUtil.requestTyyServer(tyyProperties.getApikeyCreateUrl(), "POST", accessKey, securityKey, "{}");
        log.info("创建API Key天翼云响应: {}", resp.getBody());

        // 解析响应获取 id 和 apikey
        String body = resp.getBody();
        TyyApiKeyResponse apiKeyResponse = cn.hutool.json.JSONUtil.toBean(body, TyyApiKeyResponse.class);
        if(200 == apiKeyResponse.getStatusCode()){
            // 存储到本地数据库
            ApiKey key = new ApiKey();
            if (apiKeyResponse != null && apiKeyResponse.getReturnObj() != null) {
                key.setId(apiKeyResponse.getReturnObj().getId());
                key.setSecretKey(apiKeyResponse.getReturnObj().getApikey());
            }
            key.setUserId(userId);
            key.setApikey(StrUtil.isNotBlank(req.getApikey()) ? req.getApikey() : "");
            key.setUseStatus(req.getStatus() != null ? req.getStatus() : 1);
            key.setCreateTime(LocalDateTime.now());
            key.setUpdateTime(LocalDateTime.now());
            apiKeyMapper.insert(key);
        }
    }

    @Override
    public void deleteApiKey(Long apikeyId) {
        ApiKey apiKey = apiKeyMapper.selectById(apikeyId);
        if (apiKey == null) {
            return;
        }
        // 调用天翼云接口删除 API Key
        User user = userMapper.selectById(apiKey.getUserId());
        if (user != null) {
            String accessKey = StrUtil.isNotBlank(user.getAccessKey()) ? user.getAccessKey() : tyyProperties.getAccessKey();
            String securityKey = StrUtil.isNotBlank(user.getSecurityKey()) ? user.getSecurityKey() : tyyProperties.getSecurityKey();
            String body = "{\"id\":" + apikeyId + "}";
            TyyResponse resp = tyySignUtil.requestTyyServer(tyyProperties.getApikeyDeleteUrl(), "POST", accessKey, securityKey, body);
            log.info("【删除API Key天翼云响应: {}】", resp.getBody());
            if(200 == resp.getStatusCode()){
                // 删除本地记录
                apiKeyMapper.deleteById(apikeyId);
            }
        }
    }

    /**
     * 更新 API Key 状态（调用天翼云接口）
     */
    public void updateApiKeyStatus(Long apikeyId, Integer useStatus) {
        ApiKey apiKey = apiKeyMapper.selectById(apikeyId);
        if (apiKey == null) {
            throw new ServiceException(UserErrorCodeConstants.USERNAME_EMAIL_MISMATCH);
        }
        User user = userMapper.selectById(apiKey.getUserId());
        String accessKey = user != null && StrUtil.isNotBlank(user.getAccessKey()) ? user.getAccessKey() : tyyProperties.getAccessKey();
        String securityKey = user != null && StrUtil.isNotBlank(user.getSecurityKey()) ? user.getSecurityKey() : tyyProperties.getSecurityKey();
        String body = "{\"id\":" + apikeyId + ",\"useStatus\":" + useStatus + "}";
        TyyResponse resp = tyySignUtil.requestTyyServer(tyyProperties.getApikeyUpdateUrl(), "POST", accessKey, securityKey, body);
        log.info("【更新API Key状态天翼云响应: {}】", resp.getBody());
        if(200 == resp.getCode()){
            // 更新本地记录
            apiKey.setUseStatus(useStatus);
            apiKey.setUpdateTime(LocalDateTime.now());
            apiKeyMapper.updateById(apiKey);
        }else {
            throw new ServiceException(new ErrorCode(resp.getCode(), resp.getMessage()));
        }
    }

    /**
     * 更新 API Key 明文（仅本地数据库，不调天翼云）
     */
    public void updateApiKeyPlaintext(Long apikeyId, String apikey) {
        ApiKey apiKey = apiKeyMapper.selectById(apikeyId);
        if (apiKey == null) {
            throw new ServiceException(UserErrorCodeConstants.USERNAME_EMAIL_MISMATCH);
        }
        apiKey.setApikey(apikey);
        apiKey.setUpdateTime(LocalDateTime.now());
        apiKeyMapper.updateById(apiKey);
    }

    /**
     * 查询用户可用模型列表（调用天翼云接口）
     */
    public List<Map<String, Object>> queryAvailableModels(Long userId) {
        User user = userMapper.selectById(userId);
        Assert.notNull(user, () -> new ServiceException(UserErrorCodeConstants.USER_NOT_EXISTS));

        String accessKey = StrUtil.isNotBlank(user.getAccessKey()) ? user.getAccessKey() : tyyProperties.getAccessKey();
        String securityKey = StrUtil.isNotBlank(user.getSecurityKey()) ? user.getSecurityKey() : tyyProperties.getSecurityKey();
        String url = tyyProperties.getModelListUrl();
        TyyResponse resp = tyySignUtil.requestTyyServer(url, "GET", accessKey, securityKey, "{}");
        log.info("查询可用模型天翼云响应: {}", resp.getBody());

        String body = resp.getBody();
        Map<String, Object> resultMap = cn.hutool.json.JSONUtil.toBean(body, Map.class);
        Object resultObj = resultMap.get("returnObj");
        if (resultObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> list = (List<Map<String, Object>>) resultObj;
            return list;
        }
        return Collections.emptyList();
    }

    /**
     * 批量补全用户详情：API Key 数量
     */
    private List<UserDetailDO> enrichUserDetails(List<User> users) {
        if (users.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> userIds = users.stream().map(User::getId).collect(Collectors.toList());

        // 查询这些用户的全部 API Key
        List<ApiKey> allKeys = apiKeyMapper.selectList(new LambdaQueryWrapper<ApiKey>()
                .in(ApiKey::getUserId, userIds));
        Map<Long, List<ApiKey>> userKeyMap = allKeys.stream()
                .collect(Collectors.groupingBy(ApiKey::getUserId));

        return users.stream().map(u -> {
            UserDetailDO dto = new UserDetailDO();
            dto.setId(u.getId());
            dto.setUsername(u.getUsername());
            dto.setBusinessName(u.getBusinessName());
            dto.setEmail(u.getEmail());
            dto.setPhone(u.getPhone());
            dto.setUserId(u.getUserId());
            dto.setAccountId(u.getAccountId());
            dto.setAccessKey(u.getAccessKey());
            dto.setSecurityKey(u.getSecurityKey());
            dto.setRole(u.getRole());
            dto.setStatus(u.getStatus());
            if (u.getCreateTime() != null) {
                dto.setCreateTime(u.getCreateTime().toString());
            }

            List<ApiKey> userKeys = userKeyMap.getOrDefault(u.getId(), Collections.emptyList());
            dto.setApiKeyCount((long) userKeys.size());

            return dto;
        }).collect(Collectors.toList());
    }

    private void blacklistCurrentToken() {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                JWT jwt = JWTParser.parse(token);
                String jti = jwt.getJWTClaimsSet().getJWTID();
                if (jti != null) {
                    jwtBlacklistService.add(jti,
                            jwt.getJWTClaimsSet().getExpirationTime().toInstant());
                }
            }
        } catch (Exception e) {
            log.warn("修改密码后 JWT 黑名单添加失败", e);
        }
    }
}
