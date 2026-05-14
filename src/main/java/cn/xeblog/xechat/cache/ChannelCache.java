package cn.xeblog.xechat.cache;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChannelCache {

    private static final Set<String> CHANNEL_IDS = ConcurrentHashMap.newKeySet();

    static {
        CHANNEL_IDS.add("lobby");
    }

    public static boolean exists(String channelId) {
        if (channelId == null) {
            return false;
        }
        String id = channelId.trim();
        if (id.isEmpty()) {
            return false;
        }
        return CHANNEL_IDS.contains(id);
    }

    public static boolean create(String channelId) {
        if (channelId == null) {
            return false;
        }
        String id = channelId.trim();
        if (id.isEmpty()) {
            return false;
        }
        return CHANNEL_IDS.add(id);
    }
}
