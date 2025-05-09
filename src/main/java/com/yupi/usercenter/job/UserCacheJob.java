package com.yupi.usercenter.job;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.usercenter.model.domain.User;
import com.yupi.usercenter.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class UserCacheJob {

    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RedissonClient redissonClient;

    private List<Long> mainUserList = Arrays.asList(1L);

    @Scheduled(cron = "0 45 21 26 3  *")
    public void refreshUserCache() {
        RLock lock = redissonClient.getLock("FriendMatch-refreshUserChche:lock");
        try {
            //只有一个线程能够得到锁，抢到锁并返回true
            //trylock第一个参数为等待时间，若为0，则该线程只抢一次，抢不到就放弃
            //第二个参数为超时时间，如果为-1，则触发redisson看门狗机制（方法未执行完续期）
            if(lock.tryLock(0,-1,TimeUnit.MILLISECONDS)){
                for(Long userId : mainUserList){
                    String RedisKey = String.format("FriendMatch-recommondUser-%s", userId);

                    QueryWrapper<User> queryWrapper = new QueryWrapper<>();
                    Page<User> page = userService.page(new Page<User>(1, 8), queryWrapper);
                    ValueOperations<String, Object> opsForValue = redisTemplate.opsForValue();
                    //redis缓存
                    try {
                        opsForValue.set(RedisKey, page,300, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        log.error("Redis error" + e.getMessage());
                    }
                }
            }
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }finally {
            if(lock.isHeldByCurrentThread()){
                lock.unlock();
            }
        }


    }
}
