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
    @GetMapping("/users/{userId}/apikeys")
    public CommonResult<List<UserApiKeyDO>> userApiKeys(@PathVariable Long userId) {
        return CommonResult.success(userService.getUserApiKeys(userId));
    }

    /**
     * 为用户新增 API Key
     */
    @PostMapping("/users/{userId}/apikeys/create")
    public CommonResult<?> addApiKey(@PathVariable Long userId, @RequestBody ApiKeyCreateVO req) {
        userService.addUserApiKey(userId, req);
        return CommonResult.success(null);
    }

    /**
     * 删除 API Key（管理员）
     */
    @PostMapping("/apikeys/delete")
    public CommonResult<?> deleteApiKey(@RequestBody Map<String, Long> req) {
        userService.deleteApiKey(req.get("id"));
        return CommonResult.success(null);
    }
}
