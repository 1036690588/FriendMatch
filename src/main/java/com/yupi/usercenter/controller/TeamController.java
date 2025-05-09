package com.yupi.usercenter.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.usercenter.common.BaseResponse;
import com.yupi.usercenter.common.ErrorCode;
import com.yupi.usercenter.common.ResultUtils;
import com.yupi.usercenter.exception.BusinessException;
import com.yupi.usercenter.model.domain.Team;
import com.yupi.usercenter.model.domain.User;
import com.yupi.usercenter.model.domain.UserTeam;
import com.yupi.usercenter.model.domain.request.*;
import com.yupi.usercenter.model.vo.TeamQuery;
import com.yupi.usercenter.model.vo.TeamUserVO;
import com.yupi.usercenter.service.TeamService;
import com.yupi.usercenter.service.UserService;
import com.yupi.usercenter.service.UserTeamService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.yupi.usercenter.contant.UserConstant.USER_LOGIN_STATE;

/**
 * 队伍接口
 *
 */
@RestController
@RequestMapping("/team")
@CrossOrigin
@Slf4j
public class TeamController {

    @Resource
    private TeamService teamService;

    @Resource
    private UserService userService;

    @Resource
    private UserTeamService userTeamService;

    @Resource
    private RedisTemplate<String,Object> redisTemplate;

    @PostMapping("add")
    public BaseResponse<Long> addTeam(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest request){
        if(teamAddRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Team team = new Team();
        BeanUtils.copyProperties(teamAddRequest,team);
        Long result = teamService.addTeam(team, loginUser);
        return ResultUtils.success(result);
    }

    @PostMapping("update")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest,HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        boolean update = teamService.updateTeam(teamUpdateRequest,loginUser);
        if(!update){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"更新失败");
        }
        return ResultUtils.success(true);
    }

    @GetMapping("get")
    public BaseResponse<Team> getTeamById(Long id){
        if(id <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = teamService.getById(id);
        if(team == null){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        return ResultUtils.success(team);
    }

    @GetMapping("list")
    public BaseResponse<List<TeamUserVO>> list(TeamQuery teamQuery,HttpServletRequest request){
        if(teamQuery == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean admin = userService.isAdmin(request);
        List<TeamUserVO> teamList = teamService.listTeam(teamQuery,admin);
        final List<Long> teamIdList = teamList.stream().map(TeamUserVO::getId).collect(Collectors.toList());
        // 判断当前用户是否已加入队伍, 为teamList每个TeamUserVO中对已加入的队伍在hasJoin为true
        try{
            QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
            User loginUser = userService.getLoginUser(request);
            queryWrapper.eq("userId",loginUser.getId());
            queryWrapper.in("teamId",teamIdList);
            List<UserTeam> userTeamList = userTeamService.list(queryWrapper);
            Set<Long> teamIdSet = userTeamList.stream().map(UserTeam::getTeamId).collect(Collectors.toSet());
            teamList.forEach(team -> {
                boolean hasJoin = teamIdSet.contains(team.getId());
                team.setHasJoin(hasJoin);
            });
        }catch (Exception e){
            e.getMessage();
        }
        //查询已加入队伍的人数，teamList每个TeamUserVO中显示已加入的人数
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("teamId",teamIdList);
        List<UserTeam> userTeamList = userTeamService.list(queryWrapper);
        Map<Long, List<UserTeam>> listMap = userTeamList.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
        for(TeamUserVO teamUserVO : teamList){
            teamUserVO.setHasJoinNum(listMap.getOrDefault(teamUserVO.getId(),new ArrayList<>()).size());
        }
        return ResultUtils.success(teamList);
    }

    @GetMapping("list/page")
    public BaseResponse<IPage<Team>> pageList(TeamQuery teamQuery){
        if(teamQuery == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = new Team();
        BeanUtils.copyProperties(teamQuery,team);
        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>();
        Page<Team> teamPage = new Page<>(teamQuery.getPageNum(), teamQuery.getPageSize());
        IPage<Team> page = teamService.page(teamPage, teamQueryWrapper);
        return ResultUtils.success(page);
    }

    /**
     * 用户加入队伍
     * @param teamJoinRequest
     * @param request
     * @return
     */
    @PostMapping("join")
    public BaseResponse<Boolean> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest,HttpServletRequest request){
        if(teamJoinRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.joinTeam(teamJoinRequest, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 用户退出队伍
     * @param teamQuitRequest
     * @param request
     * @return
     */
    @PostMapping("quit")
    public BaseResponse<Boolean> quitTeam(@RequestBody TeamQuitRequest teamQuitRequest,HttpServletRequest request){
        if(teamQuitRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.quitTeam(teamQuitRequest, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 队长解散队伍
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody DeleteRequest deleteRequest,HttpServletRequest request){
        if(deleteRequest == null || deleteRequest.getId() < 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.deleteTeam(id, loginUser);
        if(!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return ResultUtils.success(true);
    }

    /**
     * 查找我创建的队伍
     * @param teamQuery
     * @param request
     * @return
     */
    @GetMapping("list/my/create")
    public BaseResponse<List<TeamUserVO>> listMyCreateTeam(TeamQuery teamQuery,HttpServletRequest request){
        if(teamQuery == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        teamQuery.setUserId(loginUser.getId());
        List<TeamUserVO> teamList = teamService.listTeam(teamQuery,true);
        return ResultUtils.success(teamList);
    }

    /**
     * 查找我加入的队伍
     * @param teamQuery
     * @param request
     * @return
     */

    @GetMapping("list/my/join")
    public BaseResponse<List<TeamUserVO>> listMyJoinTeam(TeamQuery teamQuery,HttpServletRequest request){
        if(teamQuery == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId",loginUser.getId());
        List<UserTeam> userTeamList = userTeamService.list(queryWrapper);
        Map<Long, List<UserTeam>> listMap = userTeamList.stream()
                .collect(Collectors.groupingBy(UserTeam::getTeamId));
        Set<Long> set = listMap.keySet();
        List<Long> idList = new ArrayList<>(set);
        teamQuery.setIdList(idList);
        List<TeamUserVO> result = teamService.listTeam(teamQuery, true);
        return ResultUtils.success(result);
    }




}
