package cn.xeblog.xechat.service.impl;

import cn.xeblog.xechat.cache.UserCache;
import cn.xeblog.xechat.domain.mo.User;
import cn.xeblog.xechat.enums.CodeEnum;
import cn.xeblog.xechat.service.AuthService;
import cn.xeblog.xechat.service.FriendService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class FriendServiceImpl implements FriendService {

    private final Object lock = new Object();

    @Value("${friend.storePath:}")
    private String storePath;

    @Resource
    private AuthService authService;

    @Override
    public CodeEnum addFriendByUsername(String userId, String friendUsername) {
        if (StringUtils.isBlank(userId) || StringUtils.isBlank(friendUsername)) {
            return CodeEnum.INVALID_PARAMETERS;
        }
        String uid = userId.trim();
        String username = friendUsername.trim();

        User me = authService.findByUserId(uid);
        if (me == null) {
            return CodeEnum.INVALID_TOKEN;
        }
        if (me.getUsername() != null && me.getUsername().equalsIgnoreCase(username)) {
            return CodeEnum.FRIEND_CANNOT_ADD_SELF;
        }

        User friend = authService.findByUsername(username);
        if (friend == null) {
            return CodeEnum.FRIEND_NOT_FOUND;
        }
        if (uid.equals(friend.getUserId())) {
            return CodeEnum.FRIEND_CANNOT_ADD_SELF;
        }

        synchronized (lock) {
            JSONObject root = loadRoot();
            Set<String> a = getSet(root, uid);
            Set<String> b = getSet(root, friend.getUserId());
            if (a.contains(friend.getUserId()) && b.contains(uid)) {
                return CodeEnum.FRIEND_ALREADY_ADDED;
            }
            a.add(friend.getUserId());
            b.add(uid);
            root.put(uid, new ArrayList<>(a));
            root.put(friend.getUserId(), new ArrayList<>(b));
            saveRoot(root);
        }
        return CodeEnum.SUCCESS;
    }

    @Override
    public boolean isFriend(String userId, String friendUserId) {
        if (StringUtils.isBlank(userId) || StringUtils.isBlank(friendUserId)) {
            return false;
        }
        String uid = userId.trim();
        String fid = friendUserId.trim();
        synchronized (lock) {
            JSONObject root = loadRoot();
            Set<String> set = getSet(root, uid);
            return set.contains(fid);
        }
    }

    @Override
    public List<User> listFriends(String userId) {
        if (StringUtils.isBlank(userId)) {
            return new ArrayList<>();
        }
        String uid = userId.trim();
        synchronized (lock) {
            JSONObject root = loadRoot();
            Set<String> set = getSet(root, uid);
            List<User> list = new ArrayList<>();
            for (String fid : set) {
                User user = UserCache.getUser(fid);
                if (user == null) {
                    user = authService.findByUserId(fid);
                }
                if (user != null) {
                    list.add(user);
                }
            }
            return list;
        }
    }

    private File getStoreFile() {
        if (!StringUtils.isBlank(storePath)) {
            return new File(storePath.trim());
        }
        return new File(System.getProperty("user.dir") + File.separator + "data" + File.separator + "friends.json");
    }

    private JSONObject loadRoot() {
        File file = getStoreFile();
        if (!file.exists() || file.isDirectory()) {
            return new JSONObject();
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
        } catch (Exception e) {
            return new JSONObject();
        }
        String json = sb.toString().trim();
        if (json.isEmpty()) {
            return new JSONObject();
        }
        JSONObject obj = JSON.parseObject(json);
        return obj == null ? new JSONObject() : obj;
    }

    private void saveRoot(JSONObject root) {
        File file = getStoreFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        String json = JSON.toJSONString(root == null ? new JSONObject() : root);
        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false), StandardCharsets.UTF_8))) {
            out.write(json);
        } catch (Exception e) {
        }
    }

    private Set<String> getSet(JSONObject root, String userId) {
        if (root == null || StringUtils.isBlank(userId)) {
            return new LinkedHashSet<>();
        }
        Object val = root.get(userId);
        if (val == null) {
            return new LinkedHashSet<>();
        }
        List<String> list;
        if (val instanceof List) {
            list = (List<String>) val;
        } else {
            list = JSON.parseArray(JSON.toJSONString(val), String.class);
        }
        if (list == null) {
            return new LinkedHashSet<>();
        }
        Set<String> set = new LinkedHashSet<>();
        for (String item : list) {
            if (item != null && !item.trim().isEmpty()) {
                set.add(item.trim());
            }
        }
        return set;
    }
}
