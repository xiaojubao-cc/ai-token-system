package com.ai.system.controller.apikey;

import com.ai.system.mapper.UserMapper;
import com.ai.system.model.dto.apikey.ApikeyPageResultDO;
import com.ai.system.model.entity.User;
import com.ai.system.model.pojo.CommonResult;
import com.ai.system.controller.apikey.vo.ApikeyPageQuery;
import com.ai.system.service.ApiKeyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/apikey")
public class UserApiKeyController {

    @Resource
    private ApiKeyService apiKeyService;

    @Resource
    private UserMapper userMapper;

    @GetMapping("/page")
    public CommonResult<ApikeyPageResultDO> page(ApikeyPageQuery query) {
        if (query.getPage() == null) query.setPage(1L);
        if (query.getPageSize() == null) query.setPageSize(10L);
        Long userId = getCurrentUserId();
        return CommonResult.success(apiKeyService.userPageQuery(userId, query.getPage(), query.getPageSize()));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, auth.getName()));
        return user != null ? user.getId() : 0L;
    }
}
