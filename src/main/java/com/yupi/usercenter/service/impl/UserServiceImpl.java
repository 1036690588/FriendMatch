package com.yupi.usercenter.service.impl;

import ch.qos.logback.classic.spi.EventArgUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import com.yupi.usercenter.common.AlgorithmUtils;
import com.yupi.usercenter.common.ErrorCode;
import com.yupi.usercenter.exception.BusinessException;
import com.yupi.usercenter.model.domain.User;
import com.yupi.usercenter.service.UserService;
import com.yupi.usercenter.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.apache.commons.math3.util.Pair;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.yupi.usercenter.contant.UserConstant.ADMIN_ROLE;
import static com.yupi.usercenter.contant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户服务实现类
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Resource
    private UserMapper userMapper;

    // https://www.code-nav.cn/

    /**
     * 盐值，混淆密码
     */
    private static final String SALT = "yupi";

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @param planetCode    星球编号
     * @return 新用户 id
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword, String planetCode) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (planetCode.length() > 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "星球编号过长");
        }
        // 账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            return -1;
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            return -1;
        }
        // 账户不能重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        // 星球编号不能重复
        queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("planetCode", planetCode);
        count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "编号重复");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 3. 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setPlanetCode(planetCode);
        boolean saveResult = this.save(user);
        if (!saveResult) {
            return -1;
        }
        return user.getId();
    }

    // [加入星球](https://www.code-nav.cn/) 从 0 到 1 项目实战，经验拉满！10+ 原创项目手把手教程、7 日项目提升训练营、60+ 编程经验分享直播、1000+ 项目经验笔记

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            return null;
        }
        if (userAccount.length() < 4) {
            return null;
        }
        if (userPassword.length() < 8) {
            return null;
        }
        // 账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            return null;
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = userMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            return null;
        }
        // 3. 用户脱敏
        User safetyUser = getSafetyUser(user);
        // 4. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, safetyUser);
        return safetyUser;
    }

    /**
     * 用户脱敏
     *
     * @param originUser
     * @return
     */
    @Override
    public User getSafetyUser(User originUser) {
        if (originUser == null) {
            return null;
        }
        User safetyUser = new User();
        safetyUser.setId(originUser.getId());
        safetyUser.setUsername(originUser.getUsername());
        safetyUser.setUserAccount(originUser.getUserAccount());
        safetyUser.setAvatarUrl(originUser.getAvatarUrl());
        safetyUser.setGender(originUser.getGender());
        safetyUser.setPhone(originUser.getPhone());
        safetyUser.setEmail(originUser.getEmail());
        safetyUser.setPlanetCode(originUser.getPlanetCode());
        safetyUser.setUserRole(originUser.getUserRole());
        safetyUser.setUserStatus(originUser.getUserStatus());
        safetyUser.setCreateTime(originUser.getCreateTime());
        safetyUser.setTags(originUser.getTags());
        return safetyUser;
    }

    /**
     * 用户注销
     *
     * @param request
     */
    @Override
    public int userLogout(HttpServletRequest request) {
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return 1;
    }

    @Override
    public List<User> searchUserByTagsInMemory(List<String> tagNameList){
        if(CollectionUtils.isEmpty(tagNameList)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //内存方法
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        List<User> users = userMapper.selectList(queryWrapper);
        Gson gson = new Gson();
        List<User> userList = users.stream().filter(user -> {
            String tags = user.getTags();
            if(StringUtils.isBlank(tags)) return false;
            Set<String> tempTagList = gson.fromJson(tags, new TypeToken<Set<String>>() {}.getType());
            tempTagList = Optional.ofNullable(tempTagList).orElse(new HashSet<>());
            for (String tagName : tagNameList) {
                if (!tempTagList.contains(tagName)) {
                    return false;
                }
            }
            return true;
        }).map(this::getSafetyUser).collect(Collectors.toList());
        return userList;
    }

    @Override
    public List<User> searchUserByTagsInSQL(List<String> tagNameList){
        //sql方法
        if(CollectionUtils.isEmpty(tagNameList)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        for (String tagName : tagNameList){
            queryWrapper = queryWrapper.like("tags", tagName);
        }
        List<User> users = userMapper.selectList(queryWrapper);
        return users.stream().map(this::getSafetyUser).collect(Collectors.toList());

    }

    @Override
    public int updateUser(User user, User loginUser){
        long id = user.getId();
        if(id < 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        if(!isAdmin(loginUser) && id != loginUser.getId()){
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        User user1 = userMapper.selectById(id);
        if(user1 == null){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }

        return userMapper.updateById(user);
    }

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        // 仅管理员可查询
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return user != null && user.getUserRole() == ADMIN_ROLE;
    }

    /**
     * 是否为管理员
     *
     * @param loginUser
     * @return
     */
    @Override
    public boolean isAdmin(User loginUser) {
        // 仅管理员可查询
        return loginUser != null && loginUser.getUserRole() == ADMIN_ROLE;
    }

    /**
     * 获得登录用户
     *
     * @param request
     * @return user
     */
    @Override
    public User getLoginUser(HttpServletRequest request){
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Object user = request.getSession().getAttribute(USER_LOGIN_STATE);
        if(user == null){
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        return (User)user;
    }

    @Override
    public List<User> matchUsers(Integer num, User loginUser) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id","tags");
        queryWrapper.isNotNull("tags");
        List<User> list = this.list(queryWrapper);
        String tags = loginUser.getTags();
        Gson gson = new Gson();
        List<String> tagList = gson.fromJson(tags, new TypeToken<List<String>>() {
        }.getType());
        List<Pair<User, Integer>> pairs = new ArrayList<>();
        for(User user : list){
            String userTags = user.getTags();
            if(StringUtils.isBlank(userTags) || loginUser.getId() == user.getId()){
                continue;
            }
            List<String> userTagList = gson.fromJson(userTags, new TypeToken<List<String>>() {
            }.getType());
            int distance = AlgorithmUtils.minDistance(tagList, userTagList);
            pairs.add(new Pair<>(user, distance));
        }
        // 按编辑距离由小到大排序
        // 原本顺序的 userId 列表
        List<Long> userIdList = pairs.stream().sorted((a, b) -> a.getValue() - b.getValue())
                .map(pair -> pair.getKey().getId())
                .limit(num)
                .collect(Collectors.toList());

        queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", userIdList);
        //in查询之前只能查看用户的id和tags信息，寻找编辑距离从小到大 limit为num的用户全部信息
        // sql语句in查询之后会由id从小到大的顺序排列，这样就打乱了编辑距离的排序，所以需要重新排序
        Map<Long, List<User>> listMap = this.list(queryWrapper).stream()
                .map(this::getSafetyUser)
                .collect(Collectors.groupingBy(User::getId));
        List<User> finalUserList = new ArrayList<>();
        for(Long userId : userIdList){
            finalUserList.add(listMap.get(userId).get(0));
        }
        return finalUserList;
    }


}