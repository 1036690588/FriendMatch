package com.zjh.friendmatch.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zjh.friendmatch.model.domain.Team;
import com.zjh.friendmatch.model.domain.User;

/**
* @author admin
* @description 针对表【team(队伍)】的数据库操作Service
* @createDate 2025-03-29 10:36:08
*/
public interface TeamService extends IService<Team> {

    public long addTeam(Team team, User loginUser);

}
