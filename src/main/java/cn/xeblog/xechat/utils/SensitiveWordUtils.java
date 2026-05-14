package cn.xeblog.xechat.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 敏感词处理
 *
 * @author yanpanyi
 * @date 2019/4/4
 */
@Slf4j
@Configuration
public class SensitiveWordUtils {

    private static final String REPLACE_WORD = "***";
    private static final Pattern INVALID_PATTERN = Pattern.compile("[`~!@#$%^&*()+=|{}':;,\\[\\].<>/?！￥…（）—【】｛｝｜／《》‘；：＋——＊&……％$＃@！～”“’。，、？·\\s\t\r\n]");

    /**
     * 敏感词库
     */
    private static Set<String> keyWords;
    private static volatile boolean initialized = false;
    private static final List<AcNode> AC = new ArrayList<>();

    /**
     * 读取敏感词
     */
    private static void readSensitiveWords() {
        keyWords = new HashSet<>();
        try (InputStream is = SensitiveWordUtils.class.getResourceAsStream("/sensitive-word.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            if (is == null) {
                log.warn("未找到敏感词文件 sensitive-word.txt");
                return;
            }
            String line;
            while ((line = reader.readLine()) != null) {
                String word = invalidClear(line);
                if (StringUtils.hasText(word)) {
                    keyWords.add(word);
                }
            }
        } catch (Exception e) {
            log.error("读取敏感词库出现异常！ error -> {}", e);
        }
    }

    /**
     * 初始化敏感词库
     */
    private static void init() {
        if (initialized) {
            return;
        }
        synchronized (SensitiveWordUtils.class) {
            if (initialized) {
                return;
            }
            if (keyWords == null) {
                // 读取敏感词库
                readSensitiveWords();
                log.info("初始化敏感词库，共有{}个敏感词", keyWords.size());
            }

            buildAcAutomaton();
            initialized = true;
            log.info("初始化AC自动机完成，节点数: {}", AC.size());
        }
    }

    /**
     * 构建AC自动机
     */
    private static void buildAcAutomaton() {
        AC.clear();
        AC.add(new AcNode());

        for (String keyWord : keyWords) {
            insertWord(keyWord);
        }

        Queue<Integer> queue = new ArrayDeque<>();
        for (Map.Entry<Character, Integer> entry : AC.get(0).next.entrySet()) {
            int child = entry.getValue();
            AC.get(child).fail = 0;
            queue.offer(child);
        }

        while (!queue.isEmpty()) {
            int u = queue.poll();
            AcNode node = AC.get(u);
            for (Map.Entry<Character, Integer> entry : node.next.entrySet()) {
                char c = entry.getKey();
                int v = entry.getValue();

                int f = node.fail;
                while (f != 0 && !AC.get(f).next.containsKey(c)) {
                    f = AC.get(f).fail;
                }
                if (AC.get(f).next.containsKey(c)) {
                    AC.get(v).fail = AC.get(f).next.get(c);
                } else {
                    AC.get(v).fail = 0;
                }
                queue.offer(v);
            }
        }
    }

    /**
     * 向Trie中插入关键词
     */
    private static void insertWord(String keyWord) {
        int u = 0;
        for (int i = 0; i < keyWord.length(); i++) {
            char c = keyWord.charAt(i);
            Integer next = AC.get(u).next.get(c);
            if (next == null) {
                next = AC.size();
                AC.get(u).next.put(c, next);
                AC.add(new AcNode());
            }
            u = next;
        }
        AC.get(u).wordLength = keyWord.length();
    }

    /**
     * 判断是否存在敏感词
     *
     * @param text
     * @return true:存在敏感词 false:未存在敏感词
     */
    public static boolean hasSensitiveWord(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }

        if (!initialized) {
            init();
        }

        // 清除非法字符后匹配
        String normalizedText = invalidClear(text);
        if (!StringUtils.hasText(normalizedText)) {
            return false;
        }

        List<int[]> ranges = matchSensitiveRanges(normalizedText);
        if (!ranges.isEmpty()) {
            int[] first = ranges.get(0);
            String hit = normalizedText.substring(first[0], first[1] + 1);
            log.info("[{}] => 存在敏感词 -> {}", normalizedText, hit);
            return true;
        }
        return false;
    }

    /**
     * 匹配归一化文本中的敏感词区间
     */
    private static List<int[]> matchSensitiveRanges(String normalizedText) {
        List<int[]> ranges = new ArrayList<>();
        int state = 0;
        for (int i = 0; i < normalizedText.length(); i++) {
            char c = normalizedText.charAt(i);
            while (state != 0 && !AC.get(state).next.containsKey(c)) {
                state = AC.get(state).fail;
            }
            if (AC.get(state).next.containsKey(c)) {
                state = AC.get(state).next.get(c);
            }

            int t = state;
            while (t != 0) {
                if (AC.get(t).wordLength > 0) {
                    int start = i - AC.get(t).wordLength + 1;
                    if (start >= 0) {
                        ranges.add(new int[]{start, i});
                    }
                }
                t = AC.get(t).fail;
            }
        }

        return ranges;
    }

    @PostConstruct
    public void initData() {
        reload();
    }

    /**
     * 强制重载敏感词并重建AC自动机
     */
    public static void reload() {
        synchronized (SensitiveWordUtils.class) {
            initialized = false;
            keyWords = null;
            AC.clear();
        }
        init();
    }

    /**
     * 敏感词替换（保持旧接口不变）
     *
     * @param text
     * @return 如果存在敏感词则将敏感词替换为***，否则返回原内容
     */
    public static String loveChina(String text) {
        return replaceSensitiveWord(text, REPLACE_WORD);
    }

    /**
     * 使用指定替换词处理敏感词
     */
    public static String replaceSensitiveWord(String text, String replaceWord) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        if (!initialized) {
            init();
        }
        if (AC.isEmpty()) {
            return text;
        }

        String normalizedText = invalidClear(text);
        if (!StringUtils.hasText(normalizedText)) {
            return text;
        }

        List<Integer> normalizedToOriginal = new ArrayList<>(normalizedText.length());
        StringBuilder normalizedBuilder = new StringBuilder(normalizedText.length());
        for (int i = 0; i < text.length(); i++) {
            String c = String.valueOf(text.charAt(i));
            Matcher matcher = INVALID_PATTERN.matcher(c);
            if (matcher.matches()) {
                continue;
            }
            normalizedBuilder.append(text.charAt(i));
            normalizedToOriginal.add(i);
        }
        String normalized = normalizedBuilder.toString();
        if (!StringUtils.hasText(normalized)) {
            return text;
        }

        List<int[]> ranges = matchSensitiveRanges(normalized);
        if (ranges.isEmpty()) {
            return text;
        }

        boolean[] mark = new boolean[text.length()];
        for (int[] range : ranges) {
            int start = normalizedToOriginal.get(range[0]);
            int end = normalizedToOriginal.get(range[1]);
            for (int i = start; i <= end; i++) {
                mark[i] = true;
            }
        }

        String replacement = StringUtils.hasText(replaceWord) ? replaceWord : REPLACE_WORD;
        StringBuilder result = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            if (!mark[i]) {
                result.append(text.charAt(i));
                i++;
                continue;
            }
            result.append(replacement);
            while (i < text.length() && mark[i]) {
                i++;
            }
        }

        return result.toString();
    }

    /**
     * 清除非法字符
     *
     * @param str
     * @return 返回清除非法字符后的结果
     */
    private static String invalidClear(String str) {
        Matcher m = INVALID_PATTERN.matcher(str);
        return m.replaceAll("").trim();
    }

    private static class AcNode {
        private final Map<Character, Integer> next = new HashMap<>();
        private int fail = 0;
        private int wordLength = 0;
    }
}
