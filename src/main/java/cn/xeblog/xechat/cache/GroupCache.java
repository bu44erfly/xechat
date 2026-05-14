package cn.xeblog.xechat.cache;

import java.util.concurrent.atomic.AtomicBoolean;

public class GroupCache {

    private static final AtomicBoolean CHAT_ROOM_ENABLED = new AtomicBoolean(true);

    public static boolean isChatRoomEnabled() {
        return CHAT_ROOM_ENABLED.get();
    }

    public static void setChatRoomEnabled(boolean enabled) {
        CHAT_ROOM_ENABLED.set(enabled);
    }
}

