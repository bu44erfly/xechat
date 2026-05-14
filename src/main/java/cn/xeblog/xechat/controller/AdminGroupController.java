package cn.xeblog.xechat.controller;

import cn.xeblog.xechat.cache.GroupCache;
import cn.xeblog.xechat.cache.UserCache;
import cn.xeblog.xechat.constant.RobotConstant;
import cn.xeblog.xechat.constant.StompConstant;
import cn.xeblog.xechat.domain.mo.User;
import cn.xeblog.xechat.domain.vo.DynamicMsgVo;
import cn.xeblog.xechat.domain.vo.ResponseVO;
import cn.xeblog.xechat.service.MessageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.*;

@RestController
@RequestMapping("/api/admin/groups")
public class AdminGroupController {

    @Resource
    private MessageService messageService;

    @GetMapping
    public ResponseVO list() {
        Map<String, Object> group = new HashMap<>(6, 1.0f);
        group.put("groupId", "chatRoom");
        group.put("groupName", "群聊大厅");
        group.put("enabled", GroupCache.isChatRoomEnabled());
        return new ResponseVO(Collections.singletonList(group));
    }

    @PostMapping("/chatRoom/disable")
    public ResponseVO disableChatRoom() throws Exception {
        GroupCache.setChatRoomEnabled(false);
        sendStatusUpdate("管理员已关闭群聊大厅");
        return new ResponseVO(Collections.singletonMap("enabled", false));
    }

    @PostMapping("/chatRoom/enable")
    public ResponseVO enableChatRoom() throws Exception {
        GroupCache.setChatRoomEnabled(true);
        sendStatusUpdate("管理员已开启群聊大厅");
        return new ResponseVO(Collections.singletonMap("enabled", true));
    }

    @PostMapping("/chatRoom/dissolve")
    public ResponseVO dissolveChatRoom() throws Exception {
        GroupCache.setChatRoomEnabled(false);
        sendStatusUpdate("管理员已解散群聊大厅");
        return new ResponseVO(Collections.singletonMap("enabled", false));
    }

    private void sendStatusUpdate(String message) throws Exception {
        User robot = UserCache.getUser(RobotConstant.key);
        DynamicMsgVo dynamicMsgVo = new DynamicMsgVo();
        dynamicMsgVo.setUser(robot);
        dynamicMsgVo.setMessage(message);
        dynamicMsgVo.setOnlineCount(UserCache.getOnlineCount());
        dynamicMsgVo.setOnlineUserList(UserCache.listUser());
        messageService.sendMessage(StompConstant.SUB_STATUS, dynamicMsgVo);
    }
}

