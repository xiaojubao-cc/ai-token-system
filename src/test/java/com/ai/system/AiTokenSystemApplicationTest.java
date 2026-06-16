package com.ai.system;

import cn.hutool.json.JSONUtil;
import com.ai.system.config.properties.TyyProperties;
import com.ai.system.model.dto.token.TokenUsageDO;
import com.ai.system.model.pojo.TyyApiKeyResponse;
import com.ai.system.model.pojo.Response;
import com.ai.system.model.pojo.TyyResponse;
import com.ai.system.model.pojo.TyyTokenUsageResponse;
import com.ai.system.util.tyy.TyySignUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.rowset.spi.SyncResolver;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
@Slf4j
public class AiTokenSystemApplicationTest {

    @Resource
    private TyyProperties tyyProperties;
    @Resource
    private TyySignUtil tyySignUtil;

    @Test
    public void contextLoads() {
        System.out.println(tyyProperties.getAccessKey());
    }

    @Test
    public void sendTestMail() {
//        "{\n" +
//                "  \"returnObj\" : {\n" +
//                "    \"apikey\" : \"IGe3gE/LpeD4AtMltfoB176pVC1CfvTrf3odechccc01veC8h1SIXx6U3gPVa8n4qsl3yICfv3sHzp1VEh2zHw==\",\n" +
//                "    \"id\" : 1986\n" +
//                "  },\n" +
//                "  \"message\" : \"success\",\n" +
//                "  \"statusCode\" : 200\n" +
//                "}";
        String  str = "{\"returnObj\":{\"apikey\":\"rKq5S7xd/VVknPBBZT+8QrAAKpinSAZX+riVyzl2G2gV3QLeqAWWsQEMFgnd3YdoZCSn+X1dmb7W9sHyaRmnXQ==\",\"id\":1982},\"message\":\"success\",\"statusCode\":200}";
        if(StringUtils.isNotBlank(str)){
            byte[] raw = str.getBytes(StandardCharsets.ISO_8859_1);
            String bodyString = new String(raw, StandardCharsets.UTF_8);
            TyyApiKeyResponse dto = cn.hutool.json.JSONUtil.toBean(bodyString, TyyApiKeyResponse.class);
            System.out.println(dto);
        }
    }

    @Test
    public void test(){
        List<TokenUsageDO> tokenUsageDOS = fetchFromTyyApi((String) null, null, 1982L, 0, "2026-06-16 00:00:00", "2026-06-16 10:23:40");
        System.out.println(tokenUsageDOS);
    }


    private List<TokenUsageDO> fetchFromTyyApi(String ctyunUserId, String accountId, Long apikeyId,
                                               Integer groupBy, String startTime, String endTime) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            Map<String, Object> context = new LinkedHashMap<>();
            context.put("ctyunUserId", "ae5a81318ee74a44ae44d345038cc3f1");//ctyunUserId);
            context.put("account_id", "14d73a061e4f4859959f3f89b09be9e3");//accountId);
            //body.put("context", context);

            Map<String, Object> request = new LinkedHashMap<>();
            if (apikeyId != null) {
                request.put("apikeyId", java.util.Collections.singletonList(1982));//java.util.Collections.singletonList(apikeyId)
            }
            request.put("groupBy", groupBy != null ? groupBy : 0);
            request.put("startTime", startTime);
            request.put("endTime", endTime);
            //body.put("request", request);
            body.put("apikeyId", java.util.Collections.singletonList(1982));
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
                        //di.setResolutionDuration(parseResDuration(detailItem.getResolutionDuration()));
                        // resolutionToken
                        //di.setResolutionToken(parseResToken(detailItem.getResolutionToken()));
                        details.add(di);
                    }
                    dto.setDetail(details);
                    list.add(dto);
                }
                return list;
            }
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("查询 Token 使用量失败", e);
            return new ArrayList<>();
        }
    }


}
