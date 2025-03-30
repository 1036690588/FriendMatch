package com.zjh.friendmatch.service.impl;

import com.zjh.friendmatch.model.domain.User;
import com.zjh.friendmatch.service.UserService;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

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