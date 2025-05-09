package com.yupi.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.usercenter.common.ErrorCode;
import com.yupi.usercenter.common.TeamStatusEnum;
import com.yupi.usercenter.exception.BusinessException;
import com.yupi.usercenter.model.domain.Team;
import com.yupi.usercenter.mapper.TeamMapper;
import com.yupi.usercenter.model.domain.User;
import com.yupi.usercenter.model.domain.UserTeam;
import com.yupi.usercenter.model.domain.request.TeamJoinRequest;
import com.yupi.usercenter.model.domain.request.TeamQuitRequest;
import com.yupi.usercenter.model.domain.request.TeamUpdateRequest;
import com.yupi.usercenter.model.vo.TeamQuery;
import com.yupi.usercenter.model.vo.TeamUserVO;
import com.yupi.usercenter.model.vo.UserVO;
import com.yupi.usercenter.service.TeamService;
import com.yupi.usercenter.service.UserService;
import com.yupi.usercenter.service.UserTeamService;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
* @author admin
* @description 针对表【team(队伍)】的数据库操作Service实现
* @createDate 2025-03-29 10:36:08
*/
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
    implements TeamService{

    @Resource
    private UserTeamService userTeamService;

    @Resource
    private UserService userService;

    @Resource
    private RedissonClient redissonClient;


    @Transactional(rollbackFor = Exception.class)
    @Override
    public long addTeam(Team team, User loginUser) {
//        1. 请求参数是否为空？
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
//        2. 是否登录，未登录不允许创建
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
//        a. 队伍人数 > 1 且 <= 20
        Integer num = Optional.ofNullable(team.getMaxNum()).orElse(0);
        if(num <= 1 || num > 20){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数不满足要求");
        }
//        b. 队伍标题 <= 20
        String name = team.getName();
        if(StringUtils.isBlank(name) || name.length() > 20){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍标题不满足要求");
        }
//        c. 描述 <= 512
        String description = team.getDescription();
        if(StringUtils.isBlank(description) || description.length() > 512){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍描述不满足要求");
        }
//        d. status 是否公开（int）不传默认为 0（公开）
        Integer status = Optional.ofNullable(team.getStatus()).orElse(0);
        TeamStatusEnum enumByValue = TeamStatusEnum.getEnumByValue(status);
        if(enumByValue == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍状态不满足要求");
        }
//        e. 如果 status 是加密状态，一定要有密码，且密码 <= 32
        if(enumByValue == TeamStatusEnum.PRIVATE){
            String password = team.getPassword();
            if(StringUtils.isBlank(password) || password.length() > 32){
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不满足要求");
            }
        }
//        f. 超时时间 > 当前时间
        Date expireTime = team.getExpireTime();
        if(new Date().after(expireTime)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "超时时间不满足要求");
        }
//        g. 校验用户最多创建 5 个队伍
        final long userId = loginUser.getId();
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        long hasTeamCount = this.count(queryWrapper);
        if(hasTeamCount > 5){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户最多创建 5 个队伍");
        }
//        4. 插入队伍信息到队伍表
        team.setId(null);
        team.setUserId(userId);
        boolean save = this.save(team);
        if(!save || team.getId() == null || team.getId() <= 0){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建队伍失败");
        }
//        5. 插入用户 => 队伍关系到关系表
        UserTeam userTeam = new UserTeam();
        userTeam.setTeamId(team.getId());
        userTeam.setUserId(userId);
        userTeam.setJoinTime(new Date());
        boolean save1 = userTeamService.save(userTeam);
        if (!save1){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建用户队伍记录失败");
        }
        return team.getId();
    }

    @Override
    public List<TeamUserVO> listTeam(TeamQuery teamQuery, boolean admin) {
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        //组合查询条件
        if(teamQuery != null){
            Long id = teamQuery.getId();
            if(id != null && id > 0){
                queryWrapper.eq("id", id);
            }
            List<Long> idList = teamQuery.getIdList();
            if(!CollectionUtils.isEmpty(idList)){
                queryWrapper.in("id",idList);
            }
            String name = teamQuery.getName();
            if(StringUtils.isNotBlank(name)){
                queryWrapper.like("name", name);
            }
            String description = teamQuery.getDescription();
            if(StringUtils.isNotBlank(description)){
                queryWrapper.like("description",description);
            }
            String searchText = teamQuery.getSearchText();
            if(StringUtils.isNotBlank(searchText)){
                queryWrapper.and(qw -> {
                    qw.like("name", searchText).or().like("description", searchText);
                });
            }
            Long userId = teamQuery.getUserId();
            if(userId != null && userId > 0){
                queryWrapper.eq("userId", userId);
            }
            Integer maxNum = teamQuery.getMaxNum();
            if(maxNum != null && maxNum > 0){
                queryWrapper.eq("maxNum", maxNum);
            }
            //根据状态来查询
            Integer status = teamQuery.getStatus();
            TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
            if(statusEnum == null){
                if(!admin){
                    statusEnum = TeamStatusEnum.PUBLIC;
                    queryWrapper.eq("status", statusEnum.getValue());
                }
            }else{
                queryWrapper.eq("status", statusEnum.getValue());
            }
            if(!admin && statusEnum.equals(TeamStatusEnum.PRIVATE)){
                throw new BusinessException(ErrorCode.NO_AUTH);
            }


        }
        //不展示已过期的队伍
        queryWrapper.and(qw -> {
            qw.isNull("expireTime").or().gt("expireTime", new Date());
        });
        List<Team> teamList = this.list(queryWrapper);
        if(CollectionUtils.isEmpty(teamList)){
            return new ArrayList<>();
        }
        ArrayList<TeamUserVO> teamUserList = new ArrayList<>();
        //关联查询创建人的用户信息
        for (Team team : teamList){
            Long userId = team.getUserId();
            if(userId == null){
                continue;
            }
            User user = userService.getById(userId);
            TeamUserVO teamUserVO = new TeamUserVO();
            BeanUtils.copyProperties(team, teamUserVO);
            //脱敏用户信息
            if(user != null){
                UserVO userVO = new UserVO();
                BeanUtils.copyProperties(user, userVO);
                teamUserVO.setCreateUser(userVO);
            }
            teamUserList.add(teamUserVO);
        }
        return teamUserList;

    }

    @Override
    public boolean updateTeam(TeamUpdateRequest teamUpdateRequest,User loginUser) {
        if(teamUpdateRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UpdateWrapper<Team> teamUpdateWrapper = new UpdateWrapper<>();
        Long id = teamUpdateRequest.getId();
        Team oldTeam = this.getById(id);
        if(oldTeam == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍不存在");
        }
        if(!userService.isAdmin(loginUser) && oldTeam.getUserId() != loginUser.getId()){
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        String description = teamUpdateRequest.getDescription();
        if(StringUtils.isNotBlank(description) && !description.equals(oldTeam.getDescription())){
            if(description.length() > 512){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍描述过长");
            }
            teamUpdateWrapper.eq("description",description);
        }
        String name = teamUpdateRequest.getName();
        if(StringUtils.isNotBlank(name) && !name.equals(oldTeam.getName())){
            if(name.length() > 20){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍名称过长");
            }
            teamUpdateWrapper.eq("name",name);
        }
        Date expireTime = teamUpdateRequest.getExpireTime();
        if(expireTime != null && expireTime.after(new Date())){
            teamUpdateWrapper.eq("expireTime",expireTime);
        }
        Integer status = teamUpdateRequest.getStatus();
        TeamStatusEnum enumByValue = TeamStatusEnum.getEnumByValue(status);
        if(enumByValue == TeamStatusEnum.SECRET){
            String password = teamUpdateRequest.getPassword();
            if(StringUtils.isBlank(password)){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"加密的队伍必须设置密码");
            }
            if(password.length() > 32){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码过长");
            }
            teamUpdateWrapper.eq("status",status);
            teamUpdateWrapper.eq("password",password);
        }
        Team team = new Team();
        BeanUtils.copyProperties(teamUpdateRequest,team);

        return this.updateById(team);
    }

    @Override
    public boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser) {
        if(teamJoinRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamJoinRequest.getTeamId();
        if(teamId == null || teamId <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = this.getById(teamId);
        if(team == null){
            throw new BusinessException(ErrorCode.NULL_ERROR,"队伍不存在");
        }
        if(team.getExpireTime() != null && team.getExpireTime().before(new Date())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍已过期");
        }
        long userId = loginUser.getId();
        if(team.getUserId() == userId){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"不能加入自己的队伍");
        }
        RLock lock = redissonClient.getLock("FriendMatch-join-team:lock");
        try{
            while(true){
                if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                    //该用户已加入的队伍数量 数据库查询所以放到下面，减少查询时间
                    QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("userId", userId);
                    long count = userTeamService.count(queryWrapper);
                    if(count > 5){
                        throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户最多加入或创建 5 个队伍");
                    }
                    queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("teamId",teamId);
                    count = userTeamService.count(queryWrapper);
                    //已加入队伍的人数
                    if(count >= team.getMaxNum()){
                        throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍已满");
                    }
                    queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("teamId",teamId);
                    queryWrapper.eq("userId",userId);
                    count = userTeamService.count(queryWrapper);
                    //不能重复加入已加入的队伍
                    if(count > 0){
                        throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户已加入该队伍");
                    }
                    TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByValue(team.getStatus());
                    if(TeamStatusEnum.PRIVATE.equals(teamStatusEnum)){
                        throw new BusinessException(ErrorCode.PARAMS_ERROR,"禁止加入私有队伍");
                    }
                    if(TeamStatusEnum.SECRET.equals(teamStatusEnum)){
                        if(StringUtils.isBlank(teamJoinRequest.getPassword()) || !team.getPassword().equals(teamJoinRequest.getPassword())){
                            throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码错误");
                        }
                    }
                    //修改队伍信息
                    UserTeam userTeam = new UserTeam();
                    userTeam.setUserId(userId);
                    userTeam.setTeamId(teamId);
                    userTeam.setJoinTime(new Date());
                    return userTeamService.save(userTeam);
                }
            }

        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
            return false;
        }finally {
            if(lock.isHeldByCurrentThread()){
                lock.unlock();
            }
        }

    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser) {
        if(teamQuitRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamQuitRequest.getTeamId();
        if(teamId == null || teamId <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = this.getById(teamId);
        if(team == null){
            throw new BusinessException(ErrorCode.NULL_ERROR,"队伍不存在");
        }
        QueryWrapper<UserTeam> userTeamqueryWrapper = new QueryWrapper<>();
        long userId = loginUser.getId();
        userTeamqueryWrapper.eq("teamId",teamId);
        userTeamqueryWrapper.eq("userId",userId);
        long count = userTeamService.count(userTeamqueryWrapper);
        if(count == 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"未加入该队伍");
        }
        long haveJoinNum = countTeamUserById(teamId);
        if(haveJoinNum == 1){
            //删除队伍
            this.removeById(teamId);
        }else{
            //队伍人数大于1，退出队伍
            //若是队长
            if(userId == team.getUserId()){
                QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("teamId",teamId);
                queryWrapper.last("order by id asc limit 2");
                List<UserTeam> userTeamList = userTeamService.list(queryWrapper);
                if(CollectionUtils.isEmpty(userTeamList) || userTeamList.size() <= 1){
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR);
                }
                Team updateTeam = new Team();
                updateTeam.setId(teamId);
                updateTeam.setUserId(userTeamList.get(1).getUserId());
                boolean update = this.updateById(updateTeam);
                if(!update){
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR,"更新队伍队长失败");
                }
            }
        }
        return userTeamService.remove(userTeamqueryWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean deleteTeam(long id, User loginUser) {
        //校验队伍是否已经存在
        Team team = this.getById(id);
        if(team == null){
            throw new BusinessException(ErrorCode.NULL_ERROR,"队伍不存在");
        }
        Long userId = team.getUserId();
        Long teamId = team.getId();
        if(userId == null || userId != loginUser.getId()){
            throw new BusinessException(ErrorCode.NO_AUTH,"不是队长,无操作权限");
        }
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teamId",teamId);
        boolean remove = userTeamService.remove(queryWrapper);
        if(!remove){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"删除队伍用户失败");
        }
        return this.removeById(team);
    }

    private long countTeamUserById(Long teamId){
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teamId",teamId);
        return userTeamService.count(queryWrapper);
    }
}




