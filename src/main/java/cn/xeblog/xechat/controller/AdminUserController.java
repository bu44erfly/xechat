package cn.xeblog.xechat.controller;

import cn.xeblog.xechat.cache.UserBlockCache;
import cn.xeblog.xechat.cache.UserCache;
import cn.xeblog.xechat.constant.RobotConstant;
import cn.xeblog.xechat.constant.StompConstant;
import cn.xeblog.xechat.constant.UserStatusConstant;
import cn.xeblog.xechat.domain.mo.User;
import cn.xeblog.xechat.domain.vo.DynamicMsgVo;
import cn.xeblog.xechat.domain.vo.ResponseVO;
import cn.xeblog.xechat.enums.CodeEnum;
import cn.xeblog.xechat.exception.ErrorCodeException;
import cn.xeblog.xechat.service.MessageService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    @Resource
    private MessageService messageService;

    @GetMapping
    public ResponseVO list(@RequestParam(value = "keyword", required = false) String keyword) {
        List<User> users = UserCache.listUser();
        List<Map<String, Object>> result = new ArrayList<>();
        for (User user : users) {
            if (user == null || RobotConstant.key.equals(user.getUserId())) {
                continue;
            }
            if (StringUtils.isNotBlank(keyword) && (user.getUsername() == null || !user.getUsername().contains(keyword))) {
                continue;
            }
            Map<String, Object> map = new HashMap<>(8, 1.0f);
            map.put("userId", user.getUserId());
            map.put("username", user.getUsername());
            map.put("avatar", user.getAvatar());
            map.put("address", user.getAddress());
            map.put("status", user.getStatus());
            map.put("disabled", UserBlockCache.isDisabled(user.getUsername()));
            result.add(map);
        }
        return new ResponseVO(result);
    }

    @PostMapping("/{userId}/disable")
    public ResponseVO disable(@PathVariable("userId") String userId) throws Exception {
        User user = UserCache.getUser(userId);
        if (user == null || RobotConstant.key.equals(userId)) {
            return new ResponseVO(CodeEnum.FAILED);
        }
        UserBlockCache.disable(user.getUsername());
        kickInternal(user);
        return new ResponseVO(Collections.singletonMap("disabled", true));
    }

    @PostMapping("/{userId}/enable")
    public ResponseVO enable(@PathVariable("userId") String userId) throws Exception {
        User user = UserCache.getUser(userId);
        if (user == null || RobotConstant.key.equals(userId)) {
            return new ResponseVO(CodeEnum.FAILED);
        }
        UserBlockCache.enable(user.getUsername());
        sendStatusUpdate("管理员已恢复账号：" + user.getUsername());
        return new ResponseVO(Collections.singletonMap("disabled", false));
    }

    @PostMapping("/{userId}/kick")
    public ResponseVO kick(@PathVariable("userId") String userId) throws Exception {
        User user = UserCache.getUser(userId);
        if (user == null || RobotConstant.key.equals(userId)) {
            return new ResponseVO(CodeEnum.FAILED);
        }
        kickInternal(user);
        return new ResponseVO(Collections.singletonMap("kicked", true));
    }

    private void kickInternal(User user) throws Exception {
        if (user == null) {
            throw new ErrorCodeException(CodeEnum.INVALID_PARAMETERS);
        }
        messageService.sendErrorMessage(CodeEnum.USER_FORCED_OFFLINE, user);
        user.setStatus(UserStatusConstant.OFFLINE);
        UserCache.removeUser(user.getUserId());
        sendStatusUpdate("管理员已将用户强制下线：" + user.getUsername());
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

