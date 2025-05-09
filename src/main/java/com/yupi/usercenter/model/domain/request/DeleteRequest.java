package com.yupi.usercenter.model.domain.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class DeleteRequest implements Serializable {
    private static final long serialVersionUID = 5475628717821918063L;
    /**
     * id
     */
    private Long id;
}
