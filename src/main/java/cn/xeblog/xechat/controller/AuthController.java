package cn.xeblog.xechat.controller;

import cn.xeblog.xechat.cache.UserBlockCache;
import cn.xeblog.xechat.domain.mo.User;
import cn.xeblog.xechat.domain.ro.AuthLoginRO;
import cn.xeblog.xechat.domain.ro.AuthRegisterRO;
import cn.xeblog.xechat.domain.vo.ResponseVO;
import cn.xeblog.xechat.enums.CodeEnum;
import cn.xeblog.xechat.service.AuthService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Resource
    private AuthService authService;

    @PostMapping("/register")
    public ResponseVO register(@RequestBody AuthRegisterRO registerRO) {
        if (registerRO == null || StringUtils.isBlank(registerRO.getAccount()) || StringUtils.isBlank(registerRO.getPassword())
                || StringUtils.isBlank(registerRO.getUsername())) {
            return new ResponseVO(CodeEnum.INVALID_PARAMETERS);
        }
        if (authService.exists(registerRO.getAccount())) {
            return new ResponseVO(CodeEnum.USER_ALREADY_EXISTS);
        }
        if (authService.existsUsername(registerRO.getUsername())) {
            return new ResponseVO(CodeEnum.USERNAME_ALREADY_EXISTS);
        }
        User user = authService.register(registerRO);
        if (user == null) {
            return new ResponseVO(CodeEnum.FAILED);
        }
        if (UserBlockCache.isDisabled(user.getUsername())) {
            return new ResponseVO(CodeEnum.USER_DISABLED);
        }
        return new ResponseVO(user);
    }

    @PostMapping("/login")
    public ResponseVO login(@RequestBody AuthLoginRO loginRO) {
        if (loginRO == null || StringUtils.isBlank(loginRO.getAccount()) || StringUtils.isBlank(loginRO.getPassword())) {
            return new ResponseVO(CodeEnum.INVALID_PARAMETERS);
        }
        User user = authService.login(loginRO.getAccount(), loginRO.getPassword());
        if (user == null) {
            return new ResponseVO(CodeEnum.USER_LOGIN_FAILED);
        }
        if (UserBlockCache.isDisabled(user.getUsername())) {
            return new ResponseVO(CodeEnum.USER_DISABLED);
        }
        return new ResponseVO(user);
    }
}
