package com.bambi.bean.entity;

import com.bambi.im.common.bean.dto.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 描述：
 *      <br><b>短链接请求封装类</b><br>
 *      在登录逻辑中客户端使用feign发起远程调用，去调用短链接网关 EZGate模块，模块将服务器节点、用户对象以及对应的令牌封装<br>
 *      然后返回，具体可以看登录逻辑文档
 *
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/3/1 15:08    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
public class LoginBack {
    private static Logger logger = LoggerFactory.getLogger(LoginBack.class);
    List<BambiNode> imNodeList;

    private String token;

    private UserDTO userDTO;

    // getter setter
    public List<BambiNode> getImNodeList() {
        return imNodeList;
    }

    public void setImNodeList(List<BambiNode> imNodeList) {
        this.imNodeList = imNodeList;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserDTO getUserDTO() {
        return userDTO;
    }

    public void setUserDTO(UserDTO userDTO) {
        this.userDTO = userDTO;
    }
}
