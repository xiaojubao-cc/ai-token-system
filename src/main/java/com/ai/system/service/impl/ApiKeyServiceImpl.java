package com.ai.system.service.impl;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.json.JSONUtil;
import com.ai.system.config.properties.TyyProperties;
import com.ai.system.controller.apikey.vo.AdminApiKeyPageVO;
import com.ai.system.controller.apikey.vo.ApiKeyItemVO;
import com.ai.system.controller.apikey.vo.ApiKeyPageVO;
import com.ai.system.model.dto.apikey.ApikeyDO;
import com.ai.system.mapper.ApiKeyMapper;
import com.ai.system.mapper.UserMapper;
import com.ai.system.model.dto.apikey.ApikeyPageResultDO;
import com.ai.system.model.entity.ApiKey;
import com.ai.system.model.entity.User;
import com.ai.system.model.pojo.TyyResponse;
import com.ai.system.service.ApiKeyService;
import com.ai.system.util.tyy.TyySignUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ai.system.model.pojo.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ApiKeyServiceImpl implements ApiKeyService {

    @Resource
    private ApiKeyMapper apiKeyMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private TyyProperties tyyProperties;

    @Resource
    private TyySignUtil tyySignUtil;

    /**
     * Admin: 分页查询所有 API Key，支持按用户和时间筛选
     */
    public ApikeyPageResultDO adminPageQuery(AdminApiKeyPageVO query) {
        LambdaQueryWrapper<ApiKey> wrapper = new LambdaQueryWrapper<>();
        if (query.getUserId() != null) {
            wrapper.eq(ApiKey::getUserId, query.getUserId());
        }
        if (query.getStartTime() != null) {
            wrapper.ge(ApiKey::getCreateTime, query.getStartTime());
        }
        if (query.getEndTime() != null) {
            wrapper.le(ApiKey::getCreateTime, query.getEndTime());
        }
        wrapper.orderByDesc(ApiKey::getCreateTime);

        Page<ApiKey> page = apiKeyMapper.selectPage(
                new Page<>(query.getPage(), query.getPageSize()), wrapper);

        return enrichWithUserAndModel(page.getRecords(),page);
    }

    /**
     * User: 分页查询当前用户的 API Key
     */
    public ApikeyPageResultDO userPageQuery(Long userId, Long page, Long pageSize) {
        LambdaQueryWrapper<ApiKey> wrapper = new LambdaQueryWrapper<ApiKey>()
                .eq(ApiKey::getUserId, userId)
                .orderByDesc(ApiKey::getCreateTime);

        Page<ApiKey> result = apiKeyMapper.selectPage(new Page<>(page, pageSize), wrapper);

        return enrichWithUserAndModel(result.getRecords(),result);
    }

    /**
     * 从 TYY 同步 API Key 数据到本地数据库
     */
    public void syncFromTyy() {
        String url = tyyProperties.getApikeyUrl() + "?page=1&pageSize=1000";
        TyyResponse response = tyySignUtil.requestTyyServer(url, "GET", tyyProperties.getAccessKey(), tyyProperties.getSecurityKey(), null);
        String json = JSONUtil.toJsonStr(response);
        ApiKeyPageVO tyyResult = JSONUtil.toBean(json,
                new TypeReference<ApiKeyPageVO>() {}, false);
        if (tyyResult == null || tyyResult.getList() == null) {
            return;
        }
        for (ApiKeyItemVO item : tyyResult.getList()) {
            ApiKey exist = apiKeyMapper.selectById(item.getId());
            if (exist == null) {
                ApiKey entity = new ApiKey();
                entity.setId(item.getId());
                entity.setSecretKey(item.getApikey());
                entity.setUserId(item.getUserId());
                entity.setUseStatus(item.getUseStatus());
                apiKeyMapper.insert(entity);
            } else {
                exist.setSecretKey(item.getApikey());
                exist.setUseStatus(item.getUseStatus());
                if (item.getUserId() != null) {
                    exist.setUserId(item.getUserId());
                }
                apiKeyMapper.updateById(exist);
            }
        }
    }

    private ApikeyPageResultDO  enrichWithUserAndModel(List<ApiKey> apiKeys,Page<ApiKey> result) {
        Map<Long, User> userMap = buildUserMap(apiKeys);

        List<ApikeyDO> list = apiKeys.stream().map(ak -> {
            ApikeyDO dto = new ApikeyDO();
            dto.setId(ak.getId());
            dto.setUserId(ak.getUserId());
            dto.setApikey(ak.getApikey());
            dto.setSecretKey(ak.getSecretKey());
            dto.setUseStatus(ak.getUseStatus());
            if (ak.getCreateTime() != null) {
                dto.setCreateTime(ak.getCreateTime().toString());
            }
            User user = userMap.get(ak.getUserId());
            if (user != null) {
                dto.setUsername(user.getUsername());
                dto.setBusinessName(user.getBusinessName());
            }
            return dto;
        }).collect(Collectors.toList());

        ApikeyPageResultDO pageResult = new ApikeyPageResultDO();
        pageResult.setTotal(result.getTotal());
        pageResult.setPage(result.getCurrent());
        pageResult.setPageSize(result.getSize());
        pageResult.setList(list);
        return pageResult;
    }

    private Map<Long, User> buildUserMap(List<ApiKey> apiKeys) {
        List<Long> userIds = apiKeys.stream()
                .map(ApiKey::getUserId)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return Map.of();
        }
        List<User> users = userMapper.selectBatchIds(userIds);
        return users.stream().collect(Collectors.toMap(User::getId, u -> u));
    }

}
