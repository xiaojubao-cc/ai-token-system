package com.ai.system.controller.user;

import com.ai.system.mapper.UserMapper;
import com.ai.system.model.dto.token.TokenUsageDO;
import com.ai.system.model.dto.token.TokenUsageExportDO;
import com.ai.system.model.dto.token.TokenUsagePageResultDO;
import com.ai.system.model.entity.User;
import com.ai.system.model.pojo.CommonResult;
import com.ai.system.model.vo.token.TokenUsageQueryVO;
import com.ai.system.service.TokenUsageService;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user/token-usage")
@Slf4j
public class UserTokenController {

    @Resource
    private TokenUsageService tokenUsageService;

    @Resource
    private UserMapper userMapper;

    @PostMapping("/query")
    public CommonResult<TokenUsagePageResultDO> query(@Valid @RequestBody TokenUsageQueryVO query) {
        Long userId = getCurrentUserId();
        return CommonResult.success(tokenUsageService.userQuery(
                userId, query.getApikeyId(), query.getGroupBy(),
                query.getStartTime(), query.getEndTime(),
                query.getPage(), query.getPageSize()));
    }

    @PostMapping("/export")
    public void export(@Valid @RequestBody TokenUsageQueryVO query, HttpServletResponse response) throws IOException {
        Long userId = getCurrentUserId();

        List<TokenUsageDO> allData = tokenUsageService.userQueryAll(
                userId, query.getApikeyId(), query.getGroupBy(),
                query.getStartTime(), query.getEndTime());

        List<TokenUsageExportDO> exportList = allData.stream().map(d -> {
            TokenUsageExportDO e = new TokenUsageExportDO();
            e.setBusinessName(d.getBusinessName());
            e.setApikey(d.getApikey());
            e.setTokens(d.getTokens());
            e.setInputTokens(d.getInputTokens());
            e.setOutputTokens(d.getOutputTokens());
            e.setRequest(d.getRequest());
            e.setTotalDuration(d.getTotalDuration());
            e.setTotalAmount(d.getTotalAmount());
            e.setRecordDate(d.getRecordDate());
            return e;
        }).collect(Collectors.toList());

        // 先写入内存缓冲区，确保 Excel 生成成功后再设置响应头
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        try {
            EasyExcel.write(byteOut, TokenUsageExportDO.class)
                    .sheet("Token用量")
                    .doWrite(exportList);
        } catch (Exception e) {
            log.error("生成Excel文件失败", e);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":500,\"message\":\"导出失败\"}");
            return;
        }

        String fileName = "Token用量导出.xlsx";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);

        byteOut.writeTo(response.getOutputStream());
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, auth.getName()));
        return user != null ? user.getId() : 0L;
    }
}
