package com.yupi.usercenter.service.impl;

import com.yupi.usercenter.model.domain.User;
import com.yupi.usercenter.service.UserService;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserServiceImplTest {

    @Autowired
    private UserService userService;

    @Test
    void searchUserByTags() {
        List<String> tagNameList = Arrays.asList("java", "python");
        List<User> userList = userService.searchUserByTagsInMemory(tagNameList);
        Assert.assertNotNull(userList);
    }
}