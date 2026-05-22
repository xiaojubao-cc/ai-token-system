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
import com.ai.system.exception.ServiceException;
import com.ai.system.exception.enums.UserErrorCodeConstants;
import com.ai.system.mapper.ApiKeyMapper;
import com.ai.system.mapper.ModelMapper;
import com.ai.system.mapper.UserMapper;
import com.ai.system.model.dto.user.UserApiKeyDO;
import com.ai.system.model.dto.user.UserDetailDO;
import com.ai.system.model.dto.user.UserPageResultDO;
import com.ai.system.model.entity.ApiKey;
import com.ai.system.model.entity.ModelInfo;
import com.ai.system.model.entity.User;
import com.ai.system.config.security.JwtBlacklistService;
import com.ai.system.service.UserService;
import com.ai.system.util.mail.MailUtil;
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
    private ModelMapper modelMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private MailUtil mailService;

    @Resource
    private JwtBlacklistService jwtBlacklistService;

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

        Map<Long, ModelInfo> modelMap = buildModelMap(keys);

        return keys.stream().map(ak -> {
            UserApiKeyDO dto = new UserApiKeyDO();
            dto.setId(ak.getId());
            dto.setUserId(ak.getUserId());
            dto.setApikey(ak.getApikey());
            dto.setModelId(ak.getModelId());
            dto.setStatus(ak.getUseStatus());
            if (ak.getCreateTime() != null) {
                dto.setCreateTime(ak.getCreateTime().toString());
            }
            ModelInfo model = modelMap.get(ak.getModelId());
            if (model != null) {
                dto.setModelName(model.getModelName());
            }
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public void addUserApiKey(Long userId, ApiKeyCreateVO req) {
        ApiKey key = new ApiKey();
        key.setUserId(userId);
        key.setApikey(req.getApikey());
        key.setModelId(req.getModelId());
        key.setUseStatus(req.getStatus() != null ? req.getStatus() : 1);
        key.setCreateTime(LocalDateTime.now());
        key.setUpdateTime(LocalDateTime.now());
        apiKeyMapper.insert(key);
    }

    @Override
    public void deleteApiKey(Long apikeyId) {
        apiKeyMapper.deleteById(apikeyId);
    }

    /**
     * 批量补全用户详情：API Key 数量、关联模型
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

        // 查询所有关联模型
        Map<Long, ModelInfo> modelMap = buildModelMap(allKeys);

        return users.stream().map(u -> {
            UserDetailDO dto = new UserDetailDO();
            dto.setId(u.getId());
            dto.setUsername(u.getUsername());
            dto.setBusinessName(u.getBusinessName());
            dto.setEmail(u.getEmail());
            dto.setPhone(u.getPhone());
            dto.setRole(u.getRole());
            dto.setStatus(u.getStatus());
            if (u.getCreateTime() != null) {
                dto.setCreateTime(u.getCreateTime().toString());
            }

            List<ApiKey> userKeys = userKeyMap.getOrDefault(u.getId(), Collections.emptyList());
            dto.setApiKeyCount((long) userKeys.size());

            String models = userKeys.stream()
                    .map(ak -> modelMap.get(ak.getModelId()))
                    .filter(Objects::nonNull)
                    .map(ModelInfo::getModelName)
                    .distinct()
                    .collect(Collectors.joining(", "));
            dto.setAssociatedModels(models.isEmpty() ? null : models);

            return dto;
        }).collect(Collectors.toList());
    }

    private Map<Long, ModelInfo> buildModelMap(List<ApiKey> apiKeys) {
        List<Long> modelIds = apiKeys.stream()
                .map(ApiKey::getModelId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (modelIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ModelInfo> models = modelMapper.selectBatchIds(modelIds);
        return models.stream().collect(Collectors.toMap(ModelInfo::getId, m -> m));
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
