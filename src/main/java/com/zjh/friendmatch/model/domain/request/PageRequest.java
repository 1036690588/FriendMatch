package com.zjh.friendmatch.model.domain.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class PageRequest implements Serializable {
    private static final long serialVersionUID = -665416387865387L;
    //当前页
    private int pageNum = 1;
    //页面大小
    private int pageSize = 10;
}
