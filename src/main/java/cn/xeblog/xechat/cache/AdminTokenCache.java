package cn.xeblog.xechat.cache;

import cn.xeblog.xechat.utils.UUIDUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AdminTokenCache {

    private static final Map<String, Long> TOKEN_EXPIRE_AT = new ConcurrentHashMap<>(16);

    private static final long DEFAULT_EXPIRE_MILLIS = 2 * 60 * 60 * 1000L;

    public static String createToken() {
        String token = UUIDUtils.create();
        TOKEN_EXPIRE_AT.put(token, System.currentTimeMillis() + DEFAULT_EXPIRE_MILLIS);
        return token;
    }

    public static boolean isValid(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        Long expireAt = TOKEN_EXPIRE_AT.get(token);
        if (expireAt == null) {
            return false;
        }
        if (System.currentTimeMillis() > expireAt) {
            TOKEN_EXPIRE_AT.remove(token);
            return false;
        }
        return true;
    }

    public static long getExpireAt(String token) {
        Long expireAt = TOKEN_EXPIRE_AT.get(token);
        return expireAt == null ? 0L : expireAt;
    }

    public static void remove(String token) {
        if (token == null) {
            return;
        }
        TOKEN_EXPIRE_AT.remove(token);
    }
}

