package com.bambi.client.feign;

import com.bambi.bean.entity.LoginBack;
import com.bambi.config.SystemConfig;
import com.bambi.utils.JsonUtil;
import feign.Feign;
import feign.codec.StringDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 描述：
 *      <br><b>实现连接跳转</b><br>
 *      使用feign访问短链接网关，并获取对应的服务器节点列表
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
public class ShortSocketUtil {
    private static Logger logger = LoggerFactory.getLogger(ShortSocketUtil.class);

    public static LoginBack login(String username , String password){
        UserRequest userRequest = Feign.builder()
                .decoder(new StringDecoder())
                .target(UserRequest.class, SystemConfig.WEB_URL);
        String login = userRequest.login(username, password);
        LoginBack loginBack = JsonUtil.jsonToPojoByGson(login, LoginBack.class);
        return loginBack;

    }
}