package com.bambi.client.feign;

import feign.Param;
import feign.RequestLine;

/**
 * 描述：
 *      <br><b>Feign跳转接口</b><br>
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/2/27 6:13    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
public interface UserRequest {
    /**
     * 用户登录请求设计
     * @param username
     * @param password
     * @return
     */
    @RequestLine("GET /user/login/{username}/{password}")
    public String login(
            @Param("username") String username,
            @Param("password") String password
    );


    /**
     * 测试类使用<br>
     * 尝试获取用户信息
     * @param userId
     * @return
     */
    @RequestLine("GET /{userid}")
    public String getById(
            @Param("userid") String userId
    );
}
