package com.ai.system.service.impl;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.json.JSONUtil;
import com.ai.system.config.properties.TyyProperties;
import com.ai.system.mapper.ApiKeyMapper;
import com.ai.system.mapper.ModelMapper;
import com.ai.system.mapper.TokenUsageMapper;
import com.ai.system.mapper.UserMapper;
import com.ai.system.model.dto.token.TokenUsageDO;
import com.ai.system.model.dto.token.TokenUsagePageResultDO;
import com.ai.system.model.entity.ApiKey;
import com.ai.system.model.entity.ModelInfo;
import com.ai.system.model.entity.TokenUsage;
import com.ai.system.model.entity.User;
import com.ai.system.model.vo.token.TokenUsageQueryVO;
import com.ai.system.service.TokenUsageService;
import com.ai.system.util.tyy.TyySignUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ffcs.ebp.ebpsdk.common.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TokenUsageServiceImpl implements TokenUsageService {

    @Resource
    private TyyProperties tyyProperties;

    @Resource
    private TyySignUtil tyySignUtil;

    @Resource
    private ApiKeyMapper apiKeyMapper;

    @Resource
    private TokenUsageMapper tokenUsageMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private ModelMapper modelMapper;

    @Override
    public TokenUsagePageResultDO adminQuery(Long userId, Long apikeyId, Integer groupBy,
                                             String startTime, String endTime, Long page, Long pageSize) {
        List<TokenUsageDO> allData;
        if (isTodayQuery(startTime, endTime)) {
            allData = fetchFromTyyApi(apikeyId, groupBy, startTime, endTime);
        } else {
            allData = queryFromDatabaseAll(null, apikeyId, startTime, endTime);
        }
        allData = enrich(allData);

        if (userId != null) {
            Set<Long> userApikeyIds = getUserApikeyIds(userId);
            allData = allData.stream()
                    .filter(v -> userApikeyIds.contains(v.getApikeyId()))
                    .collect(Collectors.toList());
        }
        return paginate(allData, page, pageSize);
    }

    @Override
    public TokenUsagePageResultDO userQuery(Long userId, Long apikeyId, Integer groupBy,
                                            String startTime, String endTime, Long page, Long pageSize) {
        Set<Long> userApikeyIds = getUserApikeyIds(userId);
        if (userApikeyIds.isEmpty()) {
            return emptyPage(page, pageSize);
        }

        List<TokenUsageDO> allData;
        if (isTodayQuery(startTime, endTime)) {
            allData = new ArrayList<>();
            if (apikeyId != null) {
                if (!userApikeyIds.contains(apikeyId)) {
                    return emptyPage(page, pageSize);
                }
                allData = fetchFromTyyApi(apikeyId, groupBy, startTime, endTime);
            } else {
                for (Long kid : userApikeyIds) {
                    allData.addAll(fetchFromTyyApi(kid, groupBy, startTime, endTime));
                }
            }
        } else {
            allData = queryFromDatabaseAll(userId, apikeyId, startTime, endTime);
        }
        allData = enrich(allData);
        return paginate(allData, page, pageSize);
    }

    @Override
    public List<TokenUsageDO> adminQueryAll(Long userId, Long apikeyId, Integer groupBy,
                                            String startTime, String endTime) {
        List<TokenUsageDO> allData;
        if (isTodayQuery(startTime, endTime)) {
            allData = fetchFromTyyApi(apikeyId, groupBy, startTime, endTime);
        } else {
            allData = queryFromDatabaseAll(null, apikeyId, startTime, endTime);
        }
        allData = enrich(allData);
        if (userId != null) {
            Set<Long> userApikeyIds = getUserApikeyIds(userId);
            allData = allData.stream()
                    .filter(v -> userApikeyIds.contains(v.getApikeyId()))
                    .collect(Collectors.toList());
        }
        return allData;
    }

    @Override
    public List<TokenUsageDO> userQueryAll(Long userId, Long apikeyId, Integer groupBy,
                                           String startTime, String endTime) {
        Set<Long> userApikeyIds = getUserApikeyIds(userId);
        if (userApikeyIds.isEmpty()) return new ArrayList<>();
        List<TokenUsageDO> allData;
        if (isTodayQuery(startTime, endTime)) {
            allData = new ArrayList<>();
            if (apikeyId != null) {
                if (!userApikeyIds.contains(apikeyId)) return new ArrayList<>();
                allData = fetchFromTyyApi(apikeyId, groupBy, startTime, endTime);
            } else {
                for (Long kid : userApikeyIds) {
                    allData.addAll(fetchFromTyyApi(kid, groupBy, startTime, endTime));
                }
            }
        } else {
            allData = queryFromDatabaseAll(userId, apikeyId, startTime, endTime);
        }
        return enrich(allData);
    }

    @Override
    public void syncTodayData() {
        log.info("【Token 用量同步】开始同步当天数据...");
        try {
            String today = LocalDate.now().toString();
            String startTime = today + " 00:00:00";
            String endTime = today + " 23:59:59";
            List<TokenUsageDO> apiData = fetchFromTyyApi(null, 0, startTime, endTime);
            log.info("【Token 用量同步】API 返回 {} 条记录", apiData.size());

            for (TokenUsageDO item : apiData) {
                TokenUsage exist = tokenUsageMapper.selectOne(new LambdaQueryWrapper<TokenUsage>()
                        .eq(TokenUsage::getApikeyId, item.getApikeyId())
                        .eq(TokenUsage::getRecordDate, LocalDate.now()));
                if (exist != null) {
                    exist.setTokens(item.getTokens());
                    exist.setRequestCount(item.getRequest());
                    tokenUsageMapper.updateById(exist);
                } else {
                    TokenUsage record = toEntity(item);
                    tokenUsageMapper.insert(record);
                }
            }
            log.info("【Token 用量同步】同步完成");
        } catch (Exception e) {
            log.error("【Token 用量同步】同步失败", e);
        }
    }

    private List<TokenUsageDO> enrich(List<TokenUsageDO> data) {
        if (data == null || data.isEmpty()) return data != null ? data : new ArrayList<>();
        Set<Long> apikeyIds = data.stream()
                .map(TokenUsageDO::getApikeyId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (apikeyIds.isEmpty()) return data;

        List<ApiKey> keys = apiKeyMapper.selectBatchIds(apikeyIds);
        Map<Long, ApiKey> keyMap = keys.stream().collect(Collectors.toMap(ApiKey::getId, k -> k));

        Set<Long> userIds = keys.stream().map(ApiKey::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> userMap = Collections.emptyMap();
        if (!userIds.isEmpty()) {
            userMap = userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(User::getId, u -> u));
        }

        Set<Long> modelIds = keys.stream().map(ApiKey::getModelId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, ModelInfo> modelMap = Collections.emptyMap();
        if (!modelIds.isEmpty()) {
            modelMap = modelMapper.selectBatchIds(modelIds).stream().collect(Collectors.toMap(ModelInfo::getId, m -> m));
        }

        String today = LocalDate.now().toString();
        for (TokenUsageDO item : data) {
            if (item.getRecordDate() == null) item.setRecordDate(today);
            ApiKey key = keyMap.get(item.getApikeyId());
            if (key != null) {
                item.setApikey(key.getApikey());
                User user = userMap.get(key.getUserId());
                if (user != null) {
                    item.setBusinessName(user.getBusinessName() != null ? user.getBusinessName() : user.getUsername());
                }
                ModelInfo model = modelMap.get(key.getModelId());
                if (model != null) {
                    item.setModelName(model.getModelName());
                }
            }
        }
        return data;
    }

    private TokenUsagePageResultDO paginate(List<TokenUsageDO> allData, Long page, Long pageSize) {
        long p = (page != null && page > 0) ? page : 1;
        long ps = (pageSize != null && pageSize > 0) ? pageSize : 10;
        long total = allData.size();
        int from = (int) ((p - 1) * ps);
        int to = (int) Math.min(from + ps, total);

        TokenUsagePageResultDO result = new TokenUsagePageResultDO();
        result.setTotal(total);
        result.setPage(p);
        result.setPageSize(ps);
        result.setList(from < total ? allData.subList(from, to) : Collections.emptyList());
        return result;
    }

    private TokenUsagePageResultDO emptyPage(Long page, Long pageSize) {
        TokenUsagePageResultDO result = new TokenUsagePageResultDO();
        result.setTotal(0L);
        result.setPage(page != null ? page : 1);
        result.setPageSize(pageSize != null ? pageSize : 10);
        result.setList(Collections.emptyList());
        return result;
    }

    private List<TokenUsageDO> fetchFromTyyApi(Long apikeyId, Integer groupBy, String startTime, String endTime) {
        try {
            TokenUsageQueryVO query = new TokenUsageQueryVO();
            query.setApikeyId(apikeyId);
            query.setGroupBy(groupBy != null ? groupBy : 0);
            query.setStartTime(startTime);
            query.setEndTime(endTime);
            Response resp = tyySignUtil.requestTyyServer(tyyProperties.getTokenUrl(), "POST", JSONUtil.toJsonStr(query));
            String json = JSONUtil.toJsonStr(resp);
            return JSONUtil.toBean(json, new TypeReference<List<TokenUsageDO>>() {}, false);
        } catch (Exception e) {
            log.error("查询 Token 使用量失败", e);
            return new ArrayList<>();
        }
    }

    private List<TokenUsageDO> queryFromDatabaseAll(Long userId, Long apikeyId, String startTime, String endTime) {
        LambdaQueryWrapper<TokenUsage> wrapper = new LambdaQueryWrapper<>();
        if (apikeyId != null) wrapper.eq(TokenUsage::getApikeyId, apikeyId);
        if (userId != null) {
            Set<Long> keyIds = getUserApikeyIds(userId);
            if (keyIds.isEmpty()) return new ArrayList<>();
            wrapper.in(TokenUsage::getApikeyId, keyIds);
        }
        if (startTime != null && endTime != null) {
            wrapper.between(TokenUsage::getRecordDate,
                    LocalDate.parse(startTime.substring(0, 10)),
                    LocalDate.parse(endTime.substring(0, 10)));
        }
        wrapper.orderByDesc(TokenUsage::getRecordDate);
        return tokenUsageMapper.selectList(wrapper).stream().map(this::toDO).collect(Collectors.toList());
    }

    private boolean isTodayQuery(String startTime, String endTime) {
        if (startTime == null || endTime == null) return true;
        try {
            return startTime.startsWith(LocalDate.now().toString()) && endTime.startsWith(LocalDate.now().toString());
        } catch (Exception e) { return true; }
    }

    private TokenUsage toEntity(TokenUsageDO dto) {
        TokenUsage entity = new TokenUsage();
        entity.setAccountId(dto.getAccountId());
        entity.setUserId(dto.getUserId());
        entity.setApikeyId(dto.getApikeyId());
        entity.setTokens(dto.getTokens());
        entity.setRequestCount(dto.getRequest());
        entity.setRecordDate(LocalDate.now());
        entity.setCreateTime(LocalDateTime.now());
        return entity;
    }

    private TokenUsageDO toDO(TokenUsage entity) {
        TokenUsageDO dto = new TokenUsageDO();
        dto.setAccountId(entity.getAccountId());
        dto.setUserId(entity.getUserId());
        dto.setApikeyId(entity.getApikeyId());
        dto.setTokens(entity.getTokens());
        dto.setRequest(entity.getRequestCount());
        if (entity.getRecordDate() != null) dto.setRecordDate(entity.getRecordDate().toString());
        return dto;
    }

    private Set<Long> getUserApikeyIds(Long userId) {
        return apiKeyMapper.selectList(new LambdaQueryWrapper<ApiKey>().eq(ApiKey::getUserId, userId))
                .stream().map(ApiKey::getId).collect(Collectors.toSet());
    }
}
