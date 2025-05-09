package com.yupi.usercenter.model.domain.request;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class TeamJoinRequest implements Serializable {
    private static final long serialVersionUID = 4259761116537619603L;
    /**
     * Team id
     */
    private Long teamId;

    /**
     * 队伍密码
     */
    private String password;
}
