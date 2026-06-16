package com.ai.system.controller.user;

import com.ai.system.controller.user.vo.ApiKeyCreateVO;
import com.ai.system.controller.user.vo.UserCreateVO;
import com.ai.system.controller.user.vo.UserPageQueryVO;
import com.ai.system.controller.user.vo.UserUpdateVO;
import com.ai.system.mapper.UserMapper;
import com.ai.system.model.dto.user.UserApiKeyDO;
import com.ai.system.model.dto.user.UserListDO;
import com.ai.system.model.dto.user.UserPageResultDO;
import com.ai.system.model.entity.User;
import com.ai.system.model.pojo.CommonResult;
import com.ai.system.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private UserMapper userMapper;

    /**
     * 获取全部生效用户（下拉选择用，保留兼容）
     */
    @GetMapping("/users")
    public CommonResult<List<UserListDO>> listUsers() {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 1)
                .orderByAsc(User::getCreateTime));
        List<UserListDO> list = users.stream().map(u -> {
            UserListDO dto = new UserListDO();
            dto.setId(u.getId());
            dto.setUsername(u.getUsername());
            dto.setBusinessName(u.getBusinessName());
            return dto;
        }).collect(Collectors.toList());
        return CommonResult.success(list);
    }

    /**
     * 分页查询用户列表（管理员）
     */
    @GetMapping("/users/page")
    public CommonResult<UserPageResultDO> page(UserPageQueryVO query) {
        if (query.getPage() == null) query.setPage(1L);
        if (query.getPageSize() == null) query.setPageSize(10L);
        return CommonResult.success(userService.pageQuery(query));
    }

    /**
     * 新增用户（管理员）
     */
    @PostMapping("/users/create")
    public CommonResult<Long> create(@RequestBody UserCreateVO req) {
        return CommonResult.success(userService.createUser(req));
    }

    /**
     * 更新用户（管理员）
     */
    @PostMapping("/users/update")
    public CommonResult<Boolean> update(@RequestBody UserUpdateVO req) {
        return CommonResult.success(userService.updateUser(req));
    }

    /**
     * 查看用户的 API Key 列表
     */
    @GetMapping("/apikey/list")
    public CommonResult<List<UserApiKeyDO>> userApiKeys(@RequestParam Long userId) {
        return CommonResult.success(userService.getUserApiKeys(userId));
    }

    /**
     * 为用户新增 API Key
     */
    @PostMapping("/apikey/create")
    public CommonResult<?> addApiKey(@RequestBody Map<String, Object> req) {
        Long userId = ((Number) req.get("userId")).longValue();
        ApiKeyCreateVO vo = new ApiKeyCreateVO();
        Object statusObj = req.get("status");
        if (statusObj instanceof Number) {
            vo.setStatus(((Number) statusObj).intValue());
        }
        Object apikeyObj = req.get("apikey");
        if (apikeyObj instanceof String) {
            vo.setApikey((String) apikeyObj);
        }
        userService.addUserApiKey(userId, vo);
        return CommonResult.success(null);
    }

    /**
     * 删除 API Key（管理员）
     */
    @PostMapping("/apikey/delete")
    public CommonResult<?> deleteApiKey(@RequestBody Map<String, Long> req) {
        userService.deleteApiKey(req.get("id"));
        return CommonResult.success(null);
    }

    /**
     * 更新 API Key 状态（调用天翼云接口）
     */
    @PostMapping("/apikey/update")
    public CommonResult<?> updateApiKeyStatus(@RequestBody Map<String, Object> req) {
        Long id = ((Number) req.get("id")).longValue();
        Integer useStatus = ((Number) req.get("useStatus")).intValue();
        userService.updateApiKeyStatus(id, useStatus);
        return CommonResult.success(null);
    }

    /**
     * 更新 API Key 明文（仅本地数据库，不调天翼云）
     */
    @PostMapping("/apikey/updatePlaintext")
    public CommonResult<?> updateApiKeyPlaintext(@RequestBody Map<String, Object> req) {
        Long id = ((Number) req.get("id")).longValue();
        String apikey = (String) req.get("apikey");
        userService.updateApiKeyPlaintext(id, apikey);
        return CommonResult.success(null);
    }

    /**
     * 查询用户可用的模型列表（调用天翼云接口）
     */
    @GetMapping("/models/available")
    public CommonResult<List<Map<String, Object>>> queryAvailableModels(@RequestParam Long userId) {
        return CommonResult.success(userService.queryAvailableModels(userId));
    }
}
