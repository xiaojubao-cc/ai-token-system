package com.ai.system.model.dto.user;

import lombok.Data;
import java.util.List;

@Data
public class UserPageResultDO {
    private Long total;
    private Long page;
    private Long pageSize;
    private List<UserDetailDO> list;
}
