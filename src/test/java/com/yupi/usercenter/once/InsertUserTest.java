package com.yupi.usercenter.once;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;


import com.yupi.usercenter.model.domain.User;
import com.yupi.usercenter.service.UserService;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class InsertUserTest {


    @Autowired
    private UserService userService;

//    @Test
//    public void insertUser() {
//
//        final int TOTAL_NUM = 10000;
//        int batch = 1000;
//
//        StopWatch stopWatch = new StopWatch();
//        stopWatch.start();
//        ArrayList<CompletableFuture<Void>> futures = new ArrayList<>();
//        for (int i = 0; i < TOTAL_NUM / batch; i++) {
//
//            futures.add(CompletableFuture.runAsync(() -> {
//                int j = 0;
//                List<User> userList = Collections.synchronizedList(new ArrayList<>());
//                while(true){
//                    j++;
//                    User user = new User();
//                    user.setUsername("假用户");
//                    user.setUserAccount("123456789");
//                    user.setAvatarUrl("https://tse1-mm.cn.bing.net/th/id/OIP-C.Knh5i_ceDHm_cwzEcKFJ2gAAAA?w=207&h=207&c=7&r=0&o=5&dpr=1.5&pid=1.7");
//                    user.setGender(0);
//                    user.setUserPassword("123456");
//                    user.setPhone("123456789");
//                    user.setEmail("1123456@qq.com");
//                    user.setTags("[]");
//                    user.setPlanetCode("007");
//
//                    userList.add(user);
//                    if(j == batch) break;
//                }
//                System.out.println("ThreadName：" + Thread.currentThread().getName());
//                userService.saveBatch(userList,batch);
//            }));
//        }
//
//        CompletableFuture.allOf(futures.toArray(new CompletableFuture[]{})).join();
//        stopWatch.stop();
//        System.out.println("总耗时：" + stopWatch.getTime());
//
//    }


}
