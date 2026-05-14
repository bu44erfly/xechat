package cn.xeblog.xechat.cache;

import cn.xeblog.xechat.domain.vo.MessageVO;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class OfflineMessageCache {

    private static final int MAX_MESSAGES_PER_USER = 200;

    private static final ConcurrentHashMap<String, Deque<MessageVO>> USER_MESSAGES = new ConcurrentHashMap<>(64);

    public static void add(String userId, MessageVO messageVO) {
        if (userId == null || userId.trim().isEmpty() || messageVO == null) {
            return;
        }
        Deque<MessageVO> deque = USER_MESSAGES.computeIfAbsent(userId, k -> new ArrayDeque<>());
        synchronized (deque) {
            while (deque.size() >= MAX_MESSAGES_PER_USER) {
                deque.pollFirst();
            }
            deque.addLast(messageVO);
        }
    }

    public static List<MessageVO> drain(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        Deque<MessageVO> deque = USER_MESSAGES.remove(userId);
        if (deque == null) {
            return new ArrayList<>();
        }
        List<MessageVO> list = new ArrayList<>();
        synchronized (deque) {
            while (!deque.isEmpty()) {
                list.add(deque.pollFirst());
            }
        }
        return list;
    }
}

