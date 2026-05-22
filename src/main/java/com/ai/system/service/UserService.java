package com.ai.system.service;

import com.ai.system.controller.auth.vo.ForgetPasswordVO;
import com.ai.system.controller.auth.vo.UpdatePasswordVO;
import com.ai.system.controller.auth.vo.UserRegisterVO;
import com.ai.system.controller.user.vo.ApiKeyCreateVO;
import com.ai.system.controller.user.vo.UserCreateVO;
import com.ai.system.controller.user.vo.UserPageQueryVO;
import com.ai.system.controller.user.vo.UserUpdateVO;
import com.ai.system.model.dto.user.UserApiKeyDO;
import com.ai.system.model.dto.user.UserPageResultDO;

import java.util.List;

public interface UserService {

    Long register(UserRegisterVO req);

    Boolean forgetPassword(ForgetPasswordVO req);

    Boolean changePassword(UpdatePasswordVO updatePasswordReq);

    Long createUser(UserCreateVO req);

    Boolean updateUser(UserUpdateVO req);

    UserPageResultDO pageQuery(UserPageQueryVO query);

    List<UserApiKeyDO> getUserApiKeys(Long userId);

    void addUserApiKey(Long userId, ApiKeyCreateVO req);

    void deleteApiKey(Long apikeyId);
}
