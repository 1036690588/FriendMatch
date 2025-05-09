package com.yupi.usercenter.model.domain.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class TeamQuitRequest implements Serializable {

    private static final long serialVersionUID = -3226721981668765998L;

    /**
     * 队伍id
     */
    private Long teamId;
}
