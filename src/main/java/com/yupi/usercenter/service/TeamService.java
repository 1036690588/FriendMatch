package com.yupi.usercenter.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.usercenter.model.domain.Team;
import com.yupi.usercenter.model.domain.User;
import com.yupi.usercenter.model.domain.request.TeamJoinRequest;
import com.yupi.usercenter.model.domain.request.TeamQuitRequest;
import com.yupi.usercenter.model.domain.request.TeamUpdateRequest;
import com.yupi.usercenter.model.vo.TeamQuery;
import com.yupi.usercenter.model.vo.TeamUserVO;

import java.util.List;

/**
* @author admin
* @description 针对表【team(队伍)】的数据库操作Service
* @createDate 2025-03-29 10:36:08
*/
public interface TeamService extends IService<Team> {

    public long addTeam(Team team, User loginUser);

    List<TeamUserVO> listTeam(TeamQuery teamQuery, boolean admin);

    boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser);

    boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser);

    boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser);

    boolean deleteTeam(long id, User loginUser);
}
