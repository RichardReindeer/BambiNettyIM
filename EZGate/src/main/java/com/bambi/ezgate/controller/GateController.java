package com.bambi.ezgate.controller;

import com.bambi.bean.entity.BambiNode;
import com.bambi.bean.entity.LoginBack;
import com.bambi.ezgate.loadBalance.LoadBalance;
import com.bambi.ezgate.pojo.UserPojo;
import com.bambi.im.common.bean.dto.UserDTO;
import com.bambi.utils.JsonUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import org.checkerframework.checker.units.qual.A;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 描述：
 *      <br><b>登录控制器</b><br>
 *      主要负责用户客户端发送过来的登录逻辑<br>
 *      组装{@link LoginBack} 并返回给客户端
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/3/1 15:36    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
@RestController
@RequestMapping(value = "/user" ,produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
// @Api("User相关测试 Api")
public class GateController {
    private static Logger logger = LoggerFactory.getLogger(GateController.class);

    @Resource
    private LoadBalance loadBalance;

    /**
     * 客户端使用fegin调用web的登录逻辑<br>
     * 接收Login回调函数之后，从中解析出需要连接的服务器信息
     * @param username
     * @param password
     * @return
     */
    // @ApiOperation("登录逻辑")
    @RequestMapping(value = "/login/{username}/{password}",method = RequestMethod.GET)
    public String login(
            @PathVariable String username,
            @PathVariable String password
    ){
        logger.info("login is starting !!!! ");
        UserPojo userPojo = new UserPojo();
        userPojo.setUserName(username);
        userPojo.setPassWord(password);
        userPojo.setUserId(userPojo.getUserName());

        // 创建登录回调
        LoginBack back = new LoginBack();

        // 获取最佳服务器
        List<BambiNode> workers = loadBalance.getWorkers();
        back.setbambiNodeList(workers);
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(userPojo,userDTO);
        back.setUserDTO(userDTO);
        back.setToken(userPojo.getUserId().toString());
        String result = JsonUtil.pojoToJsonByGson(back);
        return result;
    }
}
