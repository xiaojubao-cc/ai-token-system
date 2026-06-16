package com.ai.system.util.tyy;

import cn.hutool.json.JSONUtil;
import com.ai.system.config.properties.TyyProperties;
import com.ai.system.model.pojo.Response;
import com.ai.system.model.pojo.TyyResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@Slf4j
public class TyySignUtil {

    @Resource
    private TyyProperties tyyProperties;

    /**
     * 天翼云网关服务请求
     * @param url 请求路径
     * @param method 请求方式 GET POST...
     * @param jsonBody 请求体 json格式
     * @return
     */
    public TyyResponse requestTyyServer(String url, String method, String accessKey, String secretKey, String jsonBody){
        String requestId = UUID.randomUUID().toString();
        String requestUrl = tyyProperties.getBaseUrl().concat(url);
        log.info("【请求天翼云网关服务:url:{},method:{},body:{}】",requestUrl,method,jsonBody);
        YunSign yunSign = new YunSign(requestUrl, accessKey, secretKey,
                requestId, jsonBody, 0, "application/json", (String)null, null);
        TyyResponse response = yunSign.toDo(method);
        log.info("【请求天翼云网关服务响应体:{}】", response.getBody());
        if(StringUtils.isNotBlank(response.getBody())){
            byte[] raw = response.getBody().getBytes(StandardCharsets.ISO_8859_1);
            String bodyString = new String(raw, StandardCharsets.UTF_8);
            response.setBody(bodyString);
            return response;
        }
        return response;
    }

}
