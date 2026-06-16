package com.ai.system.service.impl;

import cn.hutool.json.JSONUtil;
import com.ai.system.config.properties.TyyProperties;
import com.ai.system.mapper.ApiKeyMapper;
import com.ai.system.mapper.TokenUsageDetailMapper;
import com.ai.system.mapper.TokenUsageDetailResDurationMapper;
import com.ai.system.mapper.TokenUsageDetailResTokenMapper;
import com.ai.system.mapper.TokenUsageDetailStageMapper;
import com.ai.system.mapper.TokenUsageMapper;
import com.ai.system.mapper.UserMapper;
import com.ai.system.model.dto.token.TokenUsageDO;
import com.ai.system.model.dto.token.TokenUsagePageResultDO;
import com.ai.system.model.entity.ApiKey;
import com.ai.system.model.entity.TokenUsage;
import com.ai.system.model.entity.TokenUsageDetail;
import com.ai.system.model.entity.TokenUsageDetailResDuration;
import com.ai.system.model.entity.TokenUsageDetailResToken;
import com.ai.system.model.entity.TokenUsageDetailStage;
import com.ai.system.model.entity.User;

import com.ai.system.model.pojo.TyyResponse;
import com.ai.system.model.pojo.TyyTokenUsageResponse;
import com.ai.system.service.TokenUsageService;
import com.ai.system.util.tyy.TyySignUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
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
    private TokenUsageDetailMapper tokenUsageDetailMapper;

    @Resource
    private TokenUsageDetailStageMapper tokenUsageDetailStageMapper;

    @Resource
    private TokenUsageDetailResDurationMapper tokenUsageDetailResDurationMapper;

    @Resource
    private TokenUsageDetailResTokenMapper tokenUsageDetailResTokenMapper;

    @Resource
    private UserMapper userMapper;

    @Resource(name = "masterThreadPool")
    private ThreadPoolTaskExecutor masterThreadPool;

    @Override
    public TokenUsagePageResultDO adminQuery(Long userId, Long apikeyId, Integer groupBy,
                                             String startTime, String endTime, Long page, Long pageSize) {
        List<TokenUsageDO> allData = new ArrayList<>();
        if (containsToday(startTime, endTime)) {
            // 当天数据调天翼云API
            List<TokenUsageDO> apiData = fetchFromTyyApiBatch(userId, apikeyId, groupBy, startTime, endTime);
            allData.addAll(apiData);
            // API 有返回数据时排除今天的 DB 记录，否则降级用 DB 数据
            allData.addAll(queryFromDatabaseAll(null, apikeyId, startTime, endTime, !apiData.isEmpty()));
        } else {
            allData = queryFromDatabaseAll(null, apikeyId, startTime, endTime, false);
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

        List<TokenUsageDO> allData = new ArrayList<>();
        if (containsToday(startTime, endTime)) {
            List<TokenUsageDO> apiData = new ArrayList<>();
            if (apikeyId != null) {
                if (!userApikeyIds.contains(apikeyId)) {
                    return emptyPage(page, pageSize);
                }
                apiData = fetchFromTyyApiBatch(null, apikeyId, groupBy, startTime, endTime);
            } else {
                for (Long kid : userApikeyIds) {
                    apiData.addAll(fetchFromTyyApiBatch(null, kid, groupBy, startTime, endTime));
                }
            }
            allData.addAll(apiData);
            allData.addAll(queryFromDatabaseAll(userId, apikeyId, startTime, endTime, !apiData.isEmpty()));
        } else {
            allData = queryFromDatabaseAll(userId, apikeyId, startTime, endTime, false);
        }
        allData = enrich(allData);
        return paginate(allData, page, pageSize);
    }

    @Override
    public List<TokenUsageDO> adminQueryAll(Long userId, Long apikeyId, Integer groupBy,
                                            String startTime, String endTime) {
        List<TokenUsageDO> allData = new ArrayList<>();
        if (containsToday(startTime, endTime)) {
            List<TokenUsageDO> apiData = fetchFromTyyApiBatch(userId, apikeyId, groupBy, startTime, endTime);
            allData.addAll(apiData);
            allData.addAll(queryFromDatabaseAll(null, apikeyId, startTime, endTime, !apiData.isEmpty()));
        } else {
            allData = queryFromDatabaseAll(null, apikeyId, startTime, endTime, false);
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
        List<TokenUsageDO> allData = new ArrayList<>();
        if (containsToday(startTime, endTime)) {
            List<TokenUsageDO> apiData = new ArrayList<>();
            if (apikeyId != null) {
                if (!userApikeyIds.contains(apikeyId)) return new ArrayList<>();
                apiData = fetchFromTyyApiBatch(null, apikeyId, groupBy, startTime, endTime);
            } else {
                for (Long kid : userApikeyIds) {
                    apiData.addAll(fetchFromTyyApiBatch(null, kid, groupBy, startTime, endTime));
                }
            }
            allData.addAll(apiData);
            allData.addAll(queryFromDatabaseAll(userId, apikeyId, startTime, endTime, !apiData.isEmpty()));
        } else {
            allData = queryFromDatabaseAll(userId, apikeyId, startTime, endTime, false);
        }
        return enrich(allData);
    }

    @Override
    public void syncTodayData() {
        log.info("【Token 用量同步】开始同步当天数据...");
        long startMs = System.currentTimeMillis();
        try {
            String today = LocalDate.now().toString();
            String startTime = today + " 00:00:00";
            String endTime = today + " 23:59:59";

            // 获取所有用户的 apikey，按用户分组
            List<ApiKey> allKeys = apiKeyMapper.selectList(null);
            Map<Long, List<ApiKey>> userKeyMap = allKeys.stream()
                    .collect(Collectors.groupingBy(ApiKey::getUserId));

            // 收集所有异步任务
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (Map.Entry<Long, List<ApiKey>> entry : userKeyMap.entrySet()) {
                User user = userMapper.selectById(entry.getKey());
                if (user == null) continue;
                String ctyunUserId = user.getUserId();
                String accountId = user.getAccountId();
                if (ctyunUserId == null || accountId == null) continue;

                for (ApiKey key : entry.getValue()) {
                    futures.add(CompletableFuture.runAsync(() -> {
                        syncOneKey(ctyunUserId, accountId, key, startTime, endTime);
                    }, masterThreadPool).exceptionally(ex -> {
                        log.error("【Token 用量同步】KeyId: {} 同步异常", key.getId(), ex);
                        return null;
                    }));
                }
            }

            // 等待所有任务完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            long costMs = System.currentTimeMillis() - startMs;
            log.info("【Token 用量同步】同步完成, 共 {} 个Key, 耗时 {}ms", futures.size(), costMs);
        } catch (Exception e) {
            log.error("【Token 用量同步】同步失败", e);
        }
    }

    /**
     * 同步单个 API Key 的当天用量
     */
    private void syncOneKey(String ctyunUserId, String accountId, ApiKey key,
                            String startTime, String endTime) {
        List<TokenUsageDO> items = fetchFromTyyApi(ctyunUserId, accountId,
                key.getId(), 0, startTime, endTime);
        for (TokenUsageDO item : items) {
            TokenUsage exist = tokenUsageMapper.selectOne(new LambdaQueryWrapper<TokenUsage>()
                    .eq(TokenUsage::getApikeyId, item.getApikeyId())
                    .eq(TokenUsage::getRecordDate, LocalDate.now()));
            if (exist != null) {
                exist.setTokens(item.getTokens());
                exist.setInputTokens(item.getInputTokens());
                exist.setOutputTokens(item.getOutputTokens());
                exist.setRequestCount(item.getRequest());
                exist.setTotalDuration(item.getTotalDuration());
                exist.setTotalAmount(item.getTotalAmount());
                tokenUsageMapper.updateById(exist);
                if (item.getDetail() != null) {
                    saveDetail(exist.getId(), item.getDetail());
                }
            } else {
                TokenUsage record = toEntity(item);
                tokenUsageMapper.insert(record);
                if (item.getDetail() != null && record.getId() != null) {
                    saveDetail(record.getId(), item.getDetail());
                }
            }
        }
    }

    /**
     * 批量查询：根据地 userId(系统) 过滤后可批量查多个 apikey
     */
    private List<TokenUsageDO> fetchFromTyyApiBatch(Long userId, Long apikeyId, Integer groupBy,
                                                     String startTime, String endTime) {
        List<TokenUsageDO> result = new ArrayList<>();
        if (apikeyId != null) {
            // 指定了apikey，获取其所属用户的context
            ApiKey key = apiKeyMapper.selectById(apikeyId);
            if (key != null) {
                User user = userMapper.selectById(key.getUserId());
                String ctyunUserId = user != null ? user.getUserId() : null;
                String accountId = user != null ? user.getAccountId() : null;
                result.addAll(fetchFromTyyApi(ctyunUserId, accountId, apikeyId, groupBy, startTime, endTime));
            }
        } else if (userId != null) {
            // 根据系统userId查该用户所有apikey
            Set<Long> keyIds = getUserApikeyIds(userId);
            User user = userMapper.selectById(userId);
            String ctyunUserId = user != null ? user.getUserId() : null;
            String accountId = user != null ? user.getAccountId() : null;
            for (Long kid : keyIds) {
                result.addAll(fetchFromTyyApi(ctyunUserId, accountId, kid, groupBy, startTime, endTime));
            }
        } else {
            // 查全部：按用户分组
            List<ApiKey> allKeys = apiKeyMapper.selectList(null);
            Map<Long, User> userMap = new HashMap<>();
            for (ApiKey k : allKeys) {
                User u = userMap.computeIfAbsent(k.getUserId(), uid -> userMapper.selectById(uid));
                String ctyunUserId = u != null ? u.getUserId() : null;
                String accountId = u != null ? u.getAccountId() : null;
                result.addAll(fetchFromTyyApi(ctyunUserId, accountId, k.getId(), groupBy, startTime, endTime));
            }
        }
        return result;
    }

    /**
     * 调用天翼云新接口 POST /openapi/v1/tokenUsage/query
     */
    private List<TokenUsageDO> fetchFromTyyApi(String ctyunUserId, String accountId, Long apikeyId,
                                               Integer groupBy, String startTime, String endTime) {
        try {
            log.info("【用户:{},账户:{},Token天翼云查询服务流程start】", ctyunUserId,accountId);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("apikeyId", java.util.Collections.singletonList(apikeyId));
            body.put("groupBy", groupBy != null ? groupBy : 0);
            body.put("startTime", startTime);
            body.put("endTime", endTime);

            TyyResponse resp = tyySignUtil.requestTyyServer(tyyProperties.getTokenUrl(), "POST",
                    tyyProperties.getAccessKey(), tyyProperties.getSecurityKey(),
                    JSONUtil.toJsonStr(body));
            log.info("Token查询天翼云响应: {}", resp.getBody());
            String respBody = resp.getBody();
            TyyTokenUsageResponse tokenUsageResponse = cn.hutool.json.JSONUtil.toBean(respBody, TyyTokenUsageResponse.class);
            if (200 == tokenUsageResponse.getStatusCode()) {
                List<TokenUsageDO> list = new ArrayList<>();
                List<TokenUsageDO> returnObj = tokenUsageResponse.getReturnObj();
                for (TokenUsageDO data : returnObj) {
                    TokenUsageDO dto = new TokenUsageDO();
                    dto.setAccountId(data.getAccountId());
                    dto.setUserId(data.getUserId());
                    dto.setApikeyId(data.getApikeyId());
                    dto.setTokens(data.getTokens());
                    dto.setInputTokens(data.getInputTokens());
                    dto.setOutputTokens(data.getOutputTokens());
                    dto.setRequest(data.getRequest());
                    dto.setTotalDuration(dto.getTotalDuration());
                    dto.setTotalAmount(dto.getTotalAmount());
                    // detail
                    List<TokenUsageDO.DetailItem> detailList = data.getDetail();
                    @SuppressWarnings("unchecked")
                    List<TokenUsageDO.DetailItem> details = new ArrayList<>();
                    for (TokenUsageDO.DetailItem detailItem : detailList) {
                        TokenUsageDO.DetailItem di = new TokenUsageDO.DetailItem();
                        di.setName(detailItem.getName());
                        di.setAmount(detailItem.getAmount());
                        di.setAmountRequest(detailItem.getAmountRequest());
                        // stage
                        List<TokenUsageDO.StageItem> stageList = detailItem.getStage();
                        @SuppressWarnings("unchecked")
                        List<TokenUsageDO.StageItem> stages = new ArrayList<>();
                        for (TokenUsageDO.StageItem stageItems : stageList) {
                            TokenUsageDO.StageItem si = new TokenUsageDO.StageItem();
                            si.setInputTokens(stageItems.getInputTokens());
                            si.setOutputTokens(stageItems.getOutputTokens());
                            si.setMinContext(stageItems.getMinContext());
                            si.setMaxContext(stageItems.getMaxContext());
                            stages.add(si);
                        }
                        di.setStage(stages);
                        // resolutionDuration
                        di.setResolutionDuration(parseResDuration(detailItem.getResolutionDuration()));
                        // resolutionToken
                        di.setResolutionToken(parseResToken(detailItem.getResolutionToken()));
                        details.add(di);
                    }
                    dto.setDetail(details);
                    list.add(dto);
                }
                log.info("【用户:{},账户:{},Token天翼云查询服务流程end】", ctyunUserId,accountId);
                return list;
            }
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("查询 Token 使用量失败", e);
            return new ArrayList<>();
        }
    }

    private String toString(Object obj) {
        return obj != null ? obj.toString() : null;
    }

    private Long toLong(Object obj) {
        if (obj instanceof Number) return ((Number) obj).longValue();
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<TokenUsageDO.ResolutionDurationItem> parseResDuration(Object obj) {
        if (!(obj instanceof List)) return null;
        List<Map<String, Object>> list = (List<Map<String, Object>>) obj;
        List<TokenUsageDO.ResolutionDurationItem> result = new ArrayList<>();
        for (Map<String, Object> m : list) {
            TokenUsageDO.ResolutionDurationItem item = new TokenUsageDO.ResolutionDurationItem();
            item.setResolution(toString(m.get("resolution")));
            item.setCnt(toLong(m.get("cnt")));
            Object rc = m.get("requestCount");
            if (rc instanceof Number) item.setRequestCount(((Number) rc).intValue());
            result.add(item);
        }
        return result.isEmpty() ? null : result;
    }

    @SuppressWarnings("unchecked")
    private List<TokenUsageDO.ResolutionTokenItem> parseResToken(Object obj) {
        if (!(obj instanceof List)) return null;
        List<Map<String, Object>> list = (List<Map<String, Object>>) obj;
        List<TokenUsageDO.ResolutionTokenItem> result = new ArrayList<>();
        for (Map<String, Object> m : list) {
            TokenUsageDO.ResolutionTokenItem item = new TokenUsageDO.ResolutionTokenItem();
            item.setResolution(toString(m.get("resolution")));
            item.setVideoModeOutputToken(toLong(m.get("videoModeOutputToken")));
            item.setVideoLessModeOutputToken(toLong(m.get("videoLessModeOutputToken")));
            Object rc = m.get("requestCount");
            if (rc instanceof Number) item.setRequestCount(((Number) rc).intValue());
            result.add(item);
        }
        return result.isEmpty() ? null : result;
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

    private List<TokenUsageDO> queryFromDatabaseAll(Long userId, Long apikeyId, String startTime, String endTime, boolean excludeToday) {
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
            if (excludeToday) {
                wrapper.ne(TokenUsage::getRecordDate, LocalDate.now());
            }
        }
        wrapper.orderByDesc(TokenUsage::getRecordDate);
        List<TokenUsage> entities = tokenUsageMapper.selectList(wrapper);
        List<TokenUsageDO> dtoList = entities.stream().map(this::toDO).collect(Collectors.toList());
        // 批量加载 detail
        List<Long> usageIds = entities.stream().map(TokenUsage::getId).collect(Collectors.toList());
        Map<Long, List<TokenUsageDO.DetailItem>> detailMap = loadDetails(usageIds);
        for (int i = 0; i < dtoList.size(); i++) {
            Long uid = usageIds.get(i);
            dtoList.get(i).setDetail(detailMap.get(uid));
        }
        return dtoList;
    }

    private boolean containsToday(String startTime, String endTime) {
        if (startTime == null || endTime == null) return true;
        try {
            LocalDate today = LocalDate.now();
            LocalDate start = LocalDate.parse(startTime.substring(0, 10));
            LocalDate end = LocalDate.parse(endTime.substring(0, 10));
            return !today.isBefore(start) && !today.isAfter(end);
        } catch (Exception e) { return true; }
    }

    private TokenUsage toEntity(TokenUsageDO dto) {
        TokenUsage entity = new TokenUsage();
        entity.setAccountId(dto.getAccountId());
        entity.setUserId(dto.getUserId());
        entity.setApikeyId(dto.getApikeyId());
        entity.setTokens(dto.getTokens());
        entity.setInputTokens(dto.getInputTokens());
        entity.setOutputTokens(dto.getOutputTokens());
        entity.setRequestCount(dto.getRequest());
        entity.setTotalDuration(dto.getTotalDuration());
        entity.setTotalAmount(dto.getTotalAmount());
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
        dto.setInputTokens(entity.getInputTokens());
        dto.setOutputTokens(entity.getOutputTokens());
        dto.setRequest(entity.getRequestCount());
        dto.setTotalDuration(entity.getTotalDuration());
        dto.setTotalAmount(entity.getTotalAmount());
        if (entity.getRecordDate() != null) dto.setRecordDate(entity.getRecordDate().toString());
        return dto;
    }

    private Set<Long> getUserApikeyIds(Long userId) {
        return apiKeyMapper.selectList(new LambdaQueryWrapper<ApiKey>().eq(ApiKey::getUserId, userId))
                .stream().map(ApiKey::getId).collect(Collectors.toSet());
    }

    /**
     * 保存 detail 到范式化表（先删后插，实现 upsert）
     */
    private void saveDetail(Long usageId, List<TokenUsageDO.DetailItem> detail) {
        if (usageId == null || detail == null || detail.isEmpty()) return;

        // 删除该 usage 已有的旧 detail 及子记录
        List<TokenUsageDetail> oldDetails = tokenUsageDetailMapper.selectList(
                new LambdaQueryWrapper<TokenUsageDetail>().eq(TokenUsageDetail::getUsageId, usageId));
        for (TokenUsageDetail old : oldDetails) {
            tokenUsageDetailStageMapper.delete(
                    new LambdaQueryWrapper<TokenUsageDetailStage>().eq(TokenUsageDetailStage::getDetailId, old.getId()));
            tokenUsageDetailResDurationMapper.delete(
                    new LambdaQueryWrapper<TokenUsageDetailResDuration>().eq(TokenUsageDetailResDuration::getDetailId, old.getId()));
            tokenUsageDetailResTokenMapper.delete(
                    new LambdaQueryWrapper<TokenUsageDetailResToken>().eq(TokenUsageDetailResToken::getDetailId, old.getId()));
            tokenUsageDetailMapper.deleteById(old.getId());
        }

        // 插入新的 detail + 子记录
        for (TokenUsageDO.DetailItem di : detail) {
            TokenUsageDetail d = new TokenUsageDetail();
            d.setUsageId(usageId);
            d.setName(di.getName());
            d.setAmount(di.getAmount());
            d.setAmountRequest(di.getAmountRequest());
            tokenUsageDetailMapper.insert(d);

            if (di.getStage() != null) {
                for (TokenUsageDO.StageItem si : di.getStage()) {
                    TokenUsageDetailStage s = new TokenUsageDetailStage();
                    s.setDetailId(d.getId());
                    s.setInputTokens(si.getInputTokens());
                    s.setOutputTokens(si.getOutputTokens());
                    s.setMinContext(si.getMinContext());
                    s.setMaxContext(si.getMaxContext());
                    tokenUsageDetailStageMapper.insert(s);
                }
            }
            if (di.getResolutionDuration() != null) {
                for (TokenUsageDO.ResolutionDurationItem rd : di.getResolutionDuration()) {
                    TokenUsageDetailResDuration r = new TokenUsageDetailResDuration();
                    r.setDetailId(d.getId());
                    r.setResolution(rd.getResolution());
                    r.setCnt(rd.getCnt());
                    r.setRequestCount(rd.getRequestCount());
                    tokenUsageDetailResDurationMapper.insert(r);
                }
            }
            if (di.getResolutionToken() != null) {
                for (TokenUsageDO.ResolutionTokenItem rt : di.getResolutionToken()) {
                    TokenUsageDetailResToken r = new TokenUsageDetailResToken();
                    r.setDetailId(d.getId());
                    r.setResolution(rt.getResolution());
                    r.setVideoModeOutputToken(rt.getVideoModeOutputToken());
                    r.setVideoLessModeOutputToken(rt.getVideoLessModeOutputToken());
                    r.setRequestCount(rt.getRequestCount());
                    tokenUsageDetailResTokenMapper.insert(r);
                }
            }
        }
    }

    /**
     * 批量加载 detail，返回 usageId -> List<DetailItem> 映射
     */
    private Map<Long, List<TokenUsageDO.DetailItem>> loadDetails(List<Long> usageIds) {
        if (usageIds == null || usageIds.isEmpty()) return Collections.emptyMap();

        // 查询所有 detail 记录
        List<TokenUsageDetail> details = tokenUsageDetailMapper.selectList(
                new LambdaQueryWrapper<TokenUsageDetail>().in(TokenUsageDetail::getUsageId, usageIds));
        if (details.isEmpty()) {
            log.debug("loadDetails: 未找到 detail 记录, usageIds={}", usageIds);
            return Collections.emptyMap();
        }
        log.debug("loadDetails: 找到 {} 条 detail 记录, usageIds={}", details.size(), usageIds);

        // 收集所有 detail ID
        List<Long> detailIds = details.stream().map(TokenUsageDetail::getId).collect(Collectors.toList());

        // 批量查询子表
        Map<Long, List<TokenUsageDetailStage>> stageMap = tokenUsageDetailStageMapper.selectList(
                        new LambdaQueryWrapper<TokenUsageDetailStage>().in(TokenUsageDetailStage::getDetailId, detailIds))
                .stream().collect(Collectors.groupingBy(TokenUsageDetailStage::getDetailId));

        Map<Long, List<TokenUsageDetailResDuration>> durMap = tokenUsageDetailResDurationMapper.selectList(
                        new LambdaQueryWrapper<TokenUsageDetailResDuration>().in(TokenUsageDetailResDuration::getDetailId, detailIds))
                .stream().collect(Collectors.groupingBy(TokenUsageDetailResDuration::getDetailId));

        Map<Long, List<TokenUsageDetailResToken>> tokMap = tokenUsageDetailResTokenMapper.selectList(
                        new LambdaQueryWrapper<TokenUsageDetailResToken>().in(TokenUsageDetailResToken::getDetailId, detailIds))
                .stream().collect(Collectors.groupingBy(TokenUsageDetailResToken::getDetailId));

        // 组装：usageId -> List<DetailItem>
        Map<Long, List<TokenUsageDO.DetailItem>> result = new HashMap<>();
        for (TokenUsageDetail d : details) {
            TokenUsageDO.DetailItem di = new TokenUsageDO.DetailItem();
            di.setName(d.getName());
            di.setAmount(d.getAmount());
            di.setAmountRequest(d.getAmountRequest());

            List<TokenUsageDetailStage> stages = stageMap.get(d.getId());
            if (stages != null && !stages.isEmpty()) {
                di.setStage(stages.stream().map(s -> {
                    TokenUsageDO.StageItem si = new TokenUsageDO.StageItem();
                    si.setInputTokens(s.getInputTokens());
                    si.setOutputTokens(s.getOutputTokens());
                    si.setMinContext(s.getMinContext());
                    si.setMaxContext(s.getMaxContext());
                    return si;
                }).collect(Collectors.toList()));
            }

            List<TokenUsageDetailResDuration> durs = durMap.get(d.getId());
            if (durs != null && !durs.isEmpty()) {
                di.setResolutionDuration(durs.stream().map(rd -> {
                    TokenUsageDO.ResolutionDurationItem rdi = new TokenUsageDO.ResolutionDurationItem();
                    rdi.setResolution(rd.getResolution());
                    rdi.setCnt(rd.getCnt());
                    rdi.setRequestCount(rd.getRequestCount());
                    return rdi;
                }).collect(Collectors.toList()));
            }

            List<TokenUsageDetailResToken> toks = tokMap.get(d.getId());
            if (toks != null && !toks.isEmpty()) {
                di.setResolutionToken(toks.stream().map(rt -> {
                    TokenUsageDO.ResolutionTokenItem rti = new TokenUsageDO.ResolutionTokenItem();
                    rti.setResolution(rt.getResolution());
                    rti.setVideoModeOutputToken(rt.getVideoModeOutputToken());
                    rti.setVideoLessModeOutputToken(rt.getVideoLessModeOutputToken());
                    rti.setRequestCount(rt.getRequestCount());
                    return rti;
                }).collect(Collectors.toList()));
            }

            result.computeIfAbsent(d.getUsageId(), k -> new ArrayList<>()).add(di);
        }
        return result;
    }
}
