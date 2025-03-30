package com.zjh.friendmatch.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjh.friendmatch.model.domain.UserTeam;
import com.zjh.friendmatch.mapper.UserTeamMapper;
import com.zjh.friendmatch.service.UserTeamService;
import org.springframework.stereotype.Service;

/**
* @author admin
* @description 针对表【user_team(用户队伍关系)】的数据库操作Service实现
* @createDate 2025-03-29 10:38:45
*/
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
    implements UserTeamService{

}




