package cn.xeblog.xechat.service;

import cn.xeblog.xechat.domain.mo.User;
import cn.xeblog.xechat.enums.CodeEnum;

import java.util.List;

public interface FriendService {

    CodeEnum addFriendByUsername(String userId, String friendUsername);

    boolean isFriend(String userId, String friendUserId);

    List<User> listFriends(String userId);
}
