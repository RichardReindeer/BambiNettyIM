package com.bambi.client.controller;

import com.bambi.bean.entity.BambiNode;
import com.bambi.bean.entity.LoginBack;
import com.bambi.client.command.IBaseCommand;
import com.bambi.client.command.impl.ChatCommand;
import com.bambi.client.command.impl.CommandMenu;
import com.bambi.client.command.impl.LoginCommand;
import com.bambi.client.command.impl.LogoutCommand;
import com.bambi.client.feign.ShortSocketUtil;
import com.bambi.client.sender.impl.ChatSender;
import com.bambi.client.sender.impl.LoginSender;
import com.bambi.client.session.ClientSession;
import com.bambi.im.common.bean.dto.UserDTO;
import com.bambi.utils.JsonUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.stereotype.Controller;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 描述：
 * 命令控制器<br>
 * 用来接收对应命令并执行相关操作 {@link IBaseCommand}<br>
 * 以及对netty客户端的启动 {@link BambiClient}
 * <p>
 * 客户端核心类之一<br>
 * 在启动类中执行的方法: {@link CommandController#initCommandMap()} ,{@link CommandController#startCommandThread()};<br>
 * <b>执行</b>
 * <ol>
 *     <li>
 *         初始化指令映射
 *    </li>
 *    <li>
 *       调用startCommandThread 启动线程 执行客户端逻辑<ul><li>在执行登录逻辑时需要为netty客户端设定监听器</li><li>并完成对应的server节点的负载均衡逻辑</li></ul>
 *    </li>
 * </ol>
 *
 *
 * <pre>
 * HISTORY
 * ****************************************************************************
 *  ID     DATE          PERSON          REASON
 *  1      2023/2/27 5:34    Bambi        Create
 * ****************************************************************************
 * </pre>
 *
 * @author Bambi
 * @since 1.0
 */
@AutoConfigureAfter(BambiClient.class)
@Controller("commandController")
public class CommandController {
    private static Logger logger = LoggerFactory.getLogger(CommandController.class);

    @Autowired
    private LoginCommand loginCommand;
    @Autowired
    private LogoutCommand logoutCommand;
    @Autowired
    private ChatCommand chatCommand;
    @Autowired
    private CommandMenu commandMenu;
    @Autowired
    private BambiClient bambiClient;
    @Autowired
    private ChatSender chatSender;
    @Autowired
    private LoginSender loginSender;

    private ClientSession clientSession;
    private String menuString; // 菜单指令拼接字符串
    private Map<String, IBaseCommand> commandMap; // 指令集字符串 key - IBaseCommand

    private Channel channel;
    private boolean connectFlag = false;
    private Integer reConnectedTimes = 0; // 连接重试次数
    private UserDTO userDTO;

    private Scanner scanner; // 接收用户控制台输入

    /**
     * 如果连接失败，则重新尝试连接,参考zk的重连策略
     */
    private GenericFutureListener<ChannelFuture> connectListener = new GenericFutureListener<ChannelFuture>() {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            final EventLoop eventLoop = future.channel().eventLoop();
            if(future.isSuccess()){
                connectFlag = true;
                logger.info("连接成功");
                channel = future.channel();
                clientSession = new ClientSession(channel);
                clientSession.setConnected(true);
                channel.closeFuture().addListener(closeListener);
                notifyCommandThread();
            }else if(!future.isSuccess() && ++reConnectedTimes<3){
                connectFlag = false;
                logger.info("连接是失败，正在进行第 {} 次尝试",reConnectedTimes);
                eventLoop.schedule(()->{
                    bambiClient.startConnect();
                },10, TimeUnit.SECONDS);
            }else {
                logger.error("服务器连接失败");
                connectFlag = false;
                notifyCommandThread();
            }
        }
    };

    private GenericFutureListener<ChannelFuture> closeListener = new GenericFutureListener<ChannelFuture>() {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            logger.info("正在断开连接");
            channel = future.channel();
            ClientSession session = channel.attr(ClientSession.SESSION_KEY).get();
            session.close();
            notifyCommandThread();
        }
    };
    /**
     * 填充map集合，并整合显示在客户端控制台中
     */
    public void initCommandMap() {

        commandMap = new HashMap<>();
        commandMap.put(commandMenu.getKey(), commandMenu);
        commandMap.put(chatCommand.getKey(), chatCommand);
        commandMap.put(loginCommand.getKey(), loginCommand);
        commandMap.put(logoutCommand.getKey(), logoutCommand);

        setAllCommandMenu(commandMap);
    }

    public void startCommandThread() {
        scanner = new Scanner(System.in);
        Thread.currentThread().setName("客户端启动类线程");
        while (true) {
            while (connectFlag == false) {
                // 还没有登录
                userLoginAndConnecting();
            }
            while (clientSession != null) {
                // 执行聊天逻辑
                ChatCommand chatCommand = (ChatCommand) commandMap.get(ChatCommand.KEY);
                chatCommand.doAction(scanner);
                startChatting(chatCommand);
                commandMenu.doAction(scanner);
                String commandInput = commandMenu.getCommandInput();
                IBaseCommand IBaseCommand = commandMap.get(commandInput);
                if (IBaseCommand == null) {
                    logger.error("无法识别指令，请重新输入");
                }
            }
        }
    }

    /**
     * 开始连接服务器<br>
     * 拼装对应的用户信息<br>
     * 调用feign执行短链接的登录逻辑，获取到服务器节点列表<br>
     * 筛选最优节点<br>
     * 为netty客户端装填监听器端口等，并启动客户端连接<br>
     */
    private void userLoginAndConnecting() {
        if (connectFlag == true) {
            logger.info("已经成功登录，不需要再次登录");
            return;
        }
        LoginCommand login = (LoginCommand) commandMap.get(LoginCommand.KEY);
        login.doAction(scanner);

        UserDTO user = new UserDTO();
        user.setUserId(login.getUsername());
        user.setToken(login.getPassword());
        user.setDevId("11111111");
        logger.info("开始登录EzGate");
        LoginBack loginBack = ShortSocketUtil.login(user.getUserId(), user.getToken());
        List<BambiNode> bambiNodeList = loginBack.getbambiNodeList();
        logger.info("获取到的服务器节点列表为 {}", JsonUtil.pojoToJsonByGson(bambiNodeList));

        BambiNode bestNode = null;
        if (!bambiNodeList.isEmpty()) {
            // 排序时利用    ImNode的比较逻辑
            Collections.sort(bambiNodeList);
        }

        bambiClient.setConnectListener(connectListener);
        for (int i = 0; i < bambiNodeList.size(); i++) {
            bestNode = bambiNodeList.get(i);
            bambiClient.setServerHost(bestNode.getNettyHost());
            bambiClient.setServerPort(bestNode.getNettyPort());
            bambiClient.startConnect();
            waitThread();
            if (connectFlag) {
                break;
            }
            if (i == bambiNodeList.size()) {
                logger.error("尝试连接服务器节点失败");
                return;
            }
        }

        logger.info("服务器节点连接成功！！！！");

        this.userDTO = user;
        clientSession.setUserDTO(user);
        loginSender.setUserDTO(user);
        loginSender.setClientSession(clientSession);
        loginSender.sendLoginMsg();
        waitThread();
        connectFlag = true;
    }

    private void startChatting(ChatCommand chatCommand) {
        if (!isLogin()) {
            logger.error("请先登录！！！");
            return;
        }
        chatSender.setClientSession(clientSession);
        chatSender.setUserDTO(userDTO);
        chatSender.sendChatMessage(chatCommand.getUserId(), chatCommand.getChatMessage());
    }

    /**
     * 如果session为空返回null<br>
     * 不然返回{@link ClientSession} 的login判断结果
     *
     * @return
     */
    private boolean isLogin() {
        if (clientSession == null) {
            return false;
        }
        return clientSession.isLogin();
    }


    /**
     * 拼装客户端显示菜单
     *
     * @param commandMap
     */
    private void setAllCommandMenu(Map<String, IBaseCommand> commandMap) {
        Set<Map.Entry<String, IBaseCommand>> entries = commandMap.entrySet();
        Iterator<Map.Entry<String, IBaseCommand>> iterator = entries.iterator();
        StringBuilder menu = new StringBuilder();
        while (iterator.hasNext()) {
            IBaseCommand value = iterator.next().getValue();
            menu.append(value.getKey())
                    .append("-->")
                    .append(value.getTips())
                    .append(" | ");
        }
        menuString = menu.toString();
        commandMenu.setAllCommandMenu(menuString);
    }


    /**
     * 唤醒当前命令线程
     */
    public synchronized void notifyCommandThread() {
        this.notify();
    }

    /**
     * 等待
     */
    private synchronized void waitThread() {
        try {
            this.wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public LoginCommand getLoginCommand() {
        return loginCommand;
    }

    public void setLoginCommand(LoginCommand loginCommand) {
        this.loginCommand = loginCommand;
    }

    public LogoutCommand getLogoutCommand() {
        return logoutCommand;
    }

    public void setLogoutCommand(LogoutCommand logoutCommand) {
        this.logoutCommand = logoutCommand;
    }

    public ChatCommand getChatCommand() {
        return chatCommand;
    }

    public void setChatCommand(ChatCommand chatCommand) {
        this.chatCommand = chatCommand;
    }

    public CommandMenu getCommandMenu() {
        return commandMenu;
    }

    public void setCommandMenu(CommandMenu commandMenu) {
        this.commandMenu = commandMenu;
    }

    public BambiClient getBambiClient() {
        return bambiClient;
    }

    public void setBambiClient(BambiClient bambiClient) {
        this.bambiClient = bambiClient;
    }

    public ChatSender getChatSender() {
        return chatSender;
    }

    public void setChatSender(ChatSender chatSender) {
        this.chatSender = chatSender;
    }

    public LoginSender getLoginSender() {
        return loginSender;
    }

    public void setLoginSender(LoginSender loginSender) {
        this.loginSender = loginSender;
    }

    public ClientSession getClientSession() {
        return clientSession;
    }

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    public String getMenuString() {
        return menuString;
    }

    public void setMenuString(String menuString) {
        this.menuString = menuString;
    }

    public Map<String, IBaseCommand> getCommandMap() {
        return commandMap;
    }

    public void setCommandMap(Map<String, IBaseCommand> commandMap) {
        this.commandMap = commandMap;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public boolean isConnectFlag() {
        return connectFlag;
    }

    public void setConnectFlag(boolean connectFlag) {
        this.connectFlag = connectFlag;
    }

    public Integer getReConnectedTimes() {
        return reConnectedTimes;
    }

    public void setReConnectedTimes(Integer reConnectedTimes) {
        this.reConnectedTimes = reConnectedTimes;
    }

    public UserDTO getUserDTO() {
        return userDTO;
    }

    public void setUserDTO(UserDTO userDTO) {
        this.userDTO = userDTO;
    }

    public Scanner getScanner() {
        return scanner;
    }

    public void setScanner(Scanner scanner) {
        this.scanner = scanner;
    }
}
