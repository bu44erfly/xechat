package cn.xeblog.xechat.service;

import cn.xeblog.xechat.domain.mo.User;
import cn.xeblog.xechat.domain.ro.AuthRegisterRO;

public interface AuthService {

    User register(AuthRegisterRO registerRO);

    User login(String account, String password);

    boolean exists(String account);

    boolean existsUsername(String username);

    User findByUsername(String username);

    User findByUserId(String userId);
}
