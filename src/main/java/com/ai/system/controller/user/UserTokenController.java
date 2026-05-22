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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user/token-usage")
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
            e.setModelName(d.getModelName());
            e.setTokens(d.getTokens());
            e.setRequest(d.getRequest());
            e.setRecordDate(d.getRecordDate());
            return e;
        }).collect(Collectors.toList());

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("Token用量导出", StandardCharsets.UTF_8).replace("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), TokenUsageExportDO.class).sheet("Token用量").doWrite(exportList);
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, auth.getName()));
        return user != null ? user.getId() : 0L;
    }
}
