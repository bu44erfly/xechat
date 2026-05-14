package cn.xeblog.xechat.cache;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class UserBlockCache {

    private static final Set<String> DISABLED_USERNAMES = ConcurrentHashMap.newKeySet();

    public static boolean isDisabled(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        return DISABLED_USERNAMES.contains(username.trim());
    }

    public static void disable(String username) {
        if (username == null || username.trim().isEmpty()) {
            return;
        }
        DISABLED_USERNAMES.add(username.trim());
    }

    public static void enable(String username) {
        if (username == null || username.trim().isEmpty()) {
            return;
        }
        DISABLED_USERNAMES.remove(username.trim());
    }
}

