package com.bambi.bean.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

/**
 * 描述：
 *      <br><b>节点类</b><br>
 *      用来存储服务器节点基础信息，实现序列化接口，可以持久化或者序列化传输。<br>
 *      内部含有基础的host、端口等信息，并记录当前节点的负载，用于进行负载均衡筛选
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/3/1 15:05    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
public class BambiNode implements Comparable<BambiNode>, Serializable {
    private static Logger logger = LoggerFactory.getLogger(BambiNode.class);

    private static final long serialVersionUID = -499010884211304846L;
    private long id; // worker节点获取到的id，即使用zk的命名服务生成的ID
    private Integer connectedBalance = 0;
    // 可以读配置
    private String nettyHost = "127.0.0.1";
    private Integer nettyPort = 8954;

    public BambiNode() {
    }

    public BambiNode(String nettyHost, Integer nettyPort) {
        this.nettyHost = nettyHost;
        this.nettyPort = nettyPort;
    }

    @Override
    public String toString() {
        return "bambiNode{" +
                "id=" + id +
                ", connectedBalance=" + connectedBalance +
                ", nettyHost='" + nettyHost + '\'' +
                ", nettyPort=" + nettyPort +
                '}';
    }

    /**
     * 对比当前连接数，返回连接数比较后的大小结果
     *
     * @param node the object to be compared.
     * @return
     */
    @Override
    public int compareTo(BambiNode node) {
        Integer connectedBalance1 = node.connectedBalance;
        Integer connectedBalance2 = this.connectedBalance;
        if (connectedBalance2 > connectedBalance1) {
            return 1;
        } else if (connectedBalance1 > connectedBalance2) {
            return -1;
        }
        return 0;
    }

    public Integer increaseConnected() {
        connectedBalance++;
        return connectedBalance;
    }

    public Integer decreaseConnected() {
        connectedBalance--;
        return connectedBalance;
    }

    // getter setter

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Integer getConnectedBalance() {
        return connectedBalance;
    }

    public void setConnectedBalance(Integer connectedBalance) {
        this.connectedBalance = connectedBalance;
    }

    public String getNettyHost() {
        return nettyHost;
    }

    public void setNettyHost(String nettyHost) {
        this.nettyHost = nettyHost;
    }

    public Integer getNettyPort() {
        return nettyPort;
    }

    public void setNettyPort(Integer nettyPort) {
        this.nettyPort = nettyPort;
    }
}
