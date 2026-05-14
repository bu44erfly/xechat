package cn.xeblog.xechat.service.impl;

import cn.xeblog.xechat.domain.mo.AuthUserRecord;
import cn.xeblog.xechat.domain.mo.User;
import cn.xeblog.xechat.domain.ro.AuthRegisterRO;
import cn.xeblog.xechat.service.AuthService;
import cn.xeblog.xechat.utils.UUIDUtils;
import com.alibaba.fastjson.JSON;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {

    private final Object lock = new Object();

    @Value("${auth.userStorePath:}")
    private String userStorePath;

    @Override
    public User register(AuthRegisterRO registerRO) {
        if (registerRO == null || StringUtils.isBlank(registerRO.getAccount()) || StringUtils.isBlank(registerRO.getPassword())
                || StringUtils.isBlank(registerRO.getUsername())) {
            return null;
        }

        String account = registerRO.getAccount().trim();
        String passwordMd5 = DigestUtils.md5Hex(registerRO.getPassword());

        synchronized (lock) {
            List<AuthUserRecord> records = loadAll();
            for (AuthUserRecord record : records) {
                if (record != null && account.equalsIgnoreCase(record.getAccount())) {
                    return null;
                }
            }

            AuthUserRecord record = new AuthUserRecord();
            record.setUserId(UUIDUtils.create());
            record.setAccount(account);
            record.setPasswordMd5(passwordMd5);
            record.setUsername(registerRO.getUsername().trim());
            record.setAvatar(registerRO.getAvatar());
            record.setAddress(registerRO.getAddress());
            record.setCreatedAt(System.currentTimeMillis());

            records.add(record);
            saveAll(records);

            User user = new User();
            user.setUserId(record.getUserId());
            user.setUsername(record.getUsername());
            user.setAvatar(record.getAvatar());
            user.setAddress(record.getAddress());
            return user;
        }
    }

    @Override
    public User login(String account, String password) {
        if (StringUtils.isBlank(account) || StringUtils.isBlank(password)) {
            return null;
        }
        String passwordMd5 = DigestUtils.md5Hex(password);
        synchronized (lock) {
            List<AuthUserRecord> records = loadAll();
            for (AuthUserRecord record : records) {
                if (record == null) {
                    continue;
                }
                if (!account.trim().equalsIgnoreCase(record.getAccount())) {
                    continue;
                }
                if (!passwordMd5.equalsIgnoreCase(record.getPasswordMd5())) {
                    return null;
                }
                User user = new User();
                user.setUserId(record.getUserId());
                user.setUsername(record.getUsername());
                user.setAvatar(record.getAvatar());
                user.setAddress(record.getAddress());
                return user;
            }
        }
        return null;
    }

    @Override
    public boolean exists(String account) {
        if (StringUtils.isBlank(account)) {
            return false;
        }
        synchronized (lock) {
            List<AuthUserRecord> records = loadAll();
            for (AuthUserRecord record : records) {
                if (record != null && account.trim().equalsIgnoreCase(record.getAccount())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean existsUsername(String username) {
        if (StringUtils.isBlank(username)) {
            return false;
        }
        String name = username.trim();
        synchronized (lock) {
            List<AuthUserRecord> records = loadAll();
            for (AuthUserRecord record : records) {
                if (record != null && name.equalsIgnoreCase(record.getUsername())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public User findByUsername(String username) {
        if (StringUtils.isBlank(username)) {
            return null;
        }
        String name = username.trim();
        synchronized (lock) {
            List<AuthUserRecord> records = loadAll();
            for (AuthUserRecord record : records) {
                if (record == null) {
                    continue;
                }
                if (!name.equalsIgnoreCase(record.getUsername())) {
                    continue;
                }
                User user = new User();
                user.setUserId(record.getUserId());
                user.setUsername(record.getUsername());
                user.setAvatar(record.getAvatar());
                user.setAddress(record.getAddress());
                return user;
            }
        }
        return null;
    }

    @Override
    public User findByUserId(String userId) {
        if (StringUtils.isBlank(userId)) {
            return null;
        }
        String id = userId.trim();
        synchronized (lock) {
            List<AuthUserRecord> records = loadAll();
            for (AuthUserRecord record : records) {
                if (record == null) {
                    continue;
                }
                if (!id.equals(record.getUserId())) {
                    continue;
                }
                User user = new User();
                user.setUserId(record.getUserId());
                user.setUsername(record.getUsername());
                user.setAvatar(record.getAvatar());
                user.setAddress(record.getAddress());
                return user;
            }
        }
        return null;
    }

    private File getStoreFile() {
        if (!StringUtils.isBlank(userStorePath)) {
            return new File(userStorePath.trim());
        }
        return new File(System.getProperty("user.dir") + File.separator + "data" + File.separator + "users.json");
    }

    private List<AuthUserRecord> loadAll() {
        File file = getStoreFile();
        if (!file.exists() || file.isDirectory()) {
            return new ArrayList<>();
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
        } catch (Exception e) {
            return new ArrayList<>();
        }
        String json = sb.toString().trim();
        if (json.isEmpty()) {
            return new ArrayList<>();
        }
        List<AuthUserRecord> list = JSON.parseArray(json, AuthUserRecord.class);
        return list == null ? new ArrayList<>() : list;
    }

    private void saveAll(List<AuthUserRecord> records) {
        File file = getStoreFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        String json = JSON.toJSONString(records == null ? new ArrayList<>() : records);
        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false), StandardCharsets.UTF_8))) {
            out.write(json);
        } catch (Exception e) {
        }
    }
}
