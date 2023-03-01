package com.bambi.ezgate;

import com.bambi.bean.entity.BambiNode;
import com.bambi.ezgate.loadBalance.LoadBalance;
import com.bambi.ezgate.starter.EzGateApplication;
import com.bambi.utils.JsonUtil;
import jakarta.annotation.Resource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * 描述：
 *      <br><b>测试zk连接</b><br>
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/2/25 4:53    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = EzGateApplication.class)
public class TestZKCon {
    private static Logger logger = LoggerFactory.getLogger(TestZKCon.class);

    @Resource
    private LoadBalance loadBalance;

    @Test
    public void testBalanceChoose() throws Exception{
        BambiNode bestWorker = loadBalance.getBestWorker();
        logger.info("找到最合适的节点信息 {}", JsonUtil.pojoToJsonByGson(bestWorker));
        Thread.sleep(Integer.MAX_VALUE);
    }
}
