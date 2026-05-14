package cn.xeblog.xechat.controller;

import cn.xeblog.xechat.cache.UserCache;
import cn.xeblog.xechat.constant.RobotConstant;
import cn.xeblog.xechat.domain.mo.User;
import cn.xeblog.xechat.domain.ro.FriendAddRO;
import cn.xeblog.xechat.domain.vo.MessageVO;
import cn.xeblog.xechat.domain.vo.ResponseVO;
import cn.xeblog.xechat.enums.CodeEnum;
import cn.xeblog.xechat.enums.MessageTypeEnum;
import cn.xeblog.xechat.service.AuthService;
import cn.xeblog.xechat.service.FriendService;
import cn.xeblog.xechat.service.MessageService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friend")
public class FriendController {

    @Resource
    private FriendService friendService;
    @Resource
    private AuthService authService;
    @Resource
    private MessageService messageService;

    @PostMapping("/add")
    public ResponseVO add(@RequestBody FriendAddRO ro) {
        if (ro == null || StringUtils.isBlank(ro.getUserId()) || StringUtils.isBlank(ro.getFriendUsername())) {
            return new ResponseVO(CodeEnum.INVALID_PARAMETERS);
        }
        CodeEnum code = friendService.addFriendByUsername(ro.getUserId(), ro.getFriendUsername());
        if (code != CodeEnum.SUCCESS) {
            return new ResponseVO(code);
        }

        User me = authService.findByUserId(ro.getUserId());
        User friend = authService.findByUsername(ro.getFriendUsername());
        User robot = UserCache.getUser(RobotConstant.key);
        if (robot != null && me != null && friend != null) {
            try {
                MessageVO toMe = new MessageVO();
                toMe.setUser(robot);
                toMe.setType(MessageTypeEnum.SYSTEM);
                toMe.setMessage("[FRIEND_REFRESH]你已与 " + friend.getUsername() + " 成为好友");
                messageService.sendMessageToUser(new String[]{me.getUserId()}, toMe);

                MessageVO toFriend = new MessageVO();
                toFriend.setUser(robot);
                toFriend.setType(MessageTypeEnum.SYSTEM);
                toFriend.setMessage("[FRIEND_REFRESH]你已与 " + me.getUsername() + " 成为好友");
                messageService.sendMessageToUser(new String[]{friend.getUserId()}, toFriend);
            } catch (Exception e) {
            }
        }
        return new ResponseVO(new HashMap<>());
    }

    @GetMapping("/list")
    public ResponseVO list(@RequestParam("userId") String userId) {
        if (StringUtils.isBlank(userId)) {
            return new ResponseVO(CodeEnum.INVALID_PARAMETERS);
        }
        List<User> list = friendService.listFriends(userId);
        Map<String, Object> data = new HashMap<>(4, 1.0f);
        data.put("list", list);
        return new ResponseVO(data);
    }
}
