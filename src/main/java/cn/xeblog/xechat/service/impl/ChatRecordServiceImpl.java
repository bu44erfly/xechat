package cn.xeblog.xechat.service.impl;

import cn.xeblog.xechat.constant.DateConstant;
import cn.xeblog.xechat.domain.dto.ChatRecordDTO;
import cn.xeblog.xechat.domain.mo.User;
import cn.xeblog.xechat.enums.MessageTypeEnum;
import cn.xeblog.xechat.service.ChatRecordService;
import cn.xeblog.xechat.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author yanpanyi
 * @date 2019/4/4
 */
@Slf4j
@Service
public class ChatRecordServiceImpl implements ChatRecordService {

    @Value("${chatrecord.path}")
    private String path;
    @Value("${chatrecord.accessAddress}")
    private String accessAddress;

    /**
     * 生成的文件后缀
     */
    private static final String FILE_SUFFIX = ".md";

    @Async("xechatTaskExecutor")
    @Override
    public void addRecord(ChatRecordDTO chatRecordDTO) {
        File file = new File(createFileName());
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true),
                "UTF-8"))) {
            out.write(formatContent(chatRecordDTO));
        } catch (IOException e) {
            log.error("添加聊天记录异常，error ->", e);
        }
    }

    /**
     * 创建文件名
     *
     * @return 文件名
     */
    private String createFileName() {
        Calendar calendar = Calendar.getInstance();
        StringBuffer sb = new StringBuffer();
        sb.append(path);
        sb.append(calendar.get(Calendar.YEAR));
        sb.append(File.separator);
        sb.append(calendar.get(Calendar.MONTH) + 1);
        sb.append(File.separator);
        sb.append(DateUtils.getDate(calendar.getTime(), DateConstant.CHAT_RECORD_FILE_NAME));
        sb.append(FILE_SUFFIX);

        return sb.toString();
    }

    /**
     * 格式化内容
     *
     * @param chatRecordDTO 聊天记录对象
     * @return 格式化后的字符串
     */
    private String formatContent(ChatRecordDTO chatRecordDTO) {
        if (null == chatRecordDTO) {
            return "";
        }

        StringBuffer sb = new StringBuffer();

        User user = chatRecordDTO.getUser();
        switch (chatRecordDTO.getType()) {
            case ROBOT:
            case USER:
                formatUserMsg(sb, chatRecordDTO);
                break;
            case SYSTEM:
                formatSystemMsg(sb, chatRecordDTO);
                break;
            case REVOKE:
                chatRecordDTO.setMessage(user.getUsername() + "撤回了一条消息！");
                formatSystemMsg(sb, chatRecordDTO);
                break;
            default:
                break;
        }

        return sb.toString();
    }

    @Override
    public List<HashMap<String, Object>> listRecord(String directoryName) {
        File file = new File(path + directoryName);
        if (!file.exists()) {
            return null;
        }

        String[] tempList = file.list();
        if (tempList == null || tempList.length < 1) {
            return null;
        }

        List<HashMap<String, Object>> list = new ArrayList<>(tempList.length);
        HashMap<String, Object> map;
        String url = null;
        for (String name : tempList) {
            map = new HashMap<>(3, 1.0f);
            // 是否是文件
            boolean isFile = name.lastIndexOf(FILE_SUFFIX) != -1;
            if (isFile) {
                // 文件访问地址
                url = accessAddress + directoryName + name;
            }
            map.put("name", name);
            map.put("url", url);
            map.put("file", isFile);

            list.add(map);
        }

        return list;
    }

    @Override
    public List<HashMap<String, Object>> searchRecord(String keyword, Integer limit) {
        if (StringUtils.isBlank(keyword)) {
            return Collections.emptyList();
        }

        int resultLimit = normalizeLimit(limit);
        File root = new File(path);
        if (!root.exists() || !root.isDirectory()) {
            return Collections.emptyList();
        }

        List<File> markdownFiles = new ArrayList<>();
        collectMarkdownFiles(root, markdownFiles);

        List<HashMap<String, Object>> result = new ArrayList<>();
        for (File file : markdownFiles) {
            searchInSingleFile(file, keyword, resultLimit, result, root);
            if (result.size() >= resultLimit) {
                break;
            }
        }

        return result;
    }

    /**
     * 格式化系统类型的消息
     *
     * @param sb StringBuffer对象
     * @param chatRecordDTO 聊天记录对象
     */
    private void formatSystemMsg(StringBuffer sb, ChatRecordDTO chatRecordDTO) {
        sb.append("#### [");
        sb.append(chatRecordDTO.getSendTime());
        sb.append("] 系统消息：\r\n");
        sb.append("> ");
        sb.append(chatRecordDTO.getMessage());
        sb.append("\r\n");
    }

    /**
     * 格式化用户类型的消息
     *
     * @param sb StringBuffer对象
     * @param chatRecordDTO 聊天记录对象
     */
    private void formatUserMsg(StringBuffer sb, ChatRecordDTO chatRecordDTO) {
        final User user = chatRecordDTO.getUser();
        String tag = chatRecordDTO.getType() == MessageTypeEnum.ROBOT ? "[系统机器人] " : "";
        sb.append("#### [");
        sb.append(chatRecordDTO.getSendTime());
        sb.append("] ");
        sb.append(tag);
        sb.append(user.getUsername());
        sb.append("(");
        sb.append(user.getAddress());
        sb.append(")：\r\n");

        if (!StringUtils.isEmpty(chatRecordDTO.getImage())) {
            sb.append("> ![](");
            sb.append(chatRecordDTO.getImage());
            sb.append(")\r\n");
        }
        if (!StringUtils.isEmpty(chatRecordDTO.getMessage())) {
            sb.append("> ");
            sb.append(StringEscapeUtils.escapeHtml4(chatRecordDTO.getMessage()));
            sb.append("\r\n");
        }
    }

    /**
     * 递归收集 markdown 文件
     */
    private void collectMarkdownFiles(File directory, List<File> files) {
        File[] children = directory.listFiles();
        if (children == null || children.length == 0) {
            return;
        }

        for (File child : children) {
            if (child.isDirectory()) {
                collectMarkdownFiles(child, files);
                continue;
            }

            if (child.isFile() && child.getName().endsWith(FILE_SUFFIX)) {
                files.add(child);
            }
        }
    }

    /**
     * 在单个文件中检索关键词
     */
    private void searchInSingleFile(File file, String keyword, int limit, List<HashMap<String, Object>> result,
                                    File rootDirectory) {
        int lineNumber = 0;
        String currentSender = "未知";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (isHeaderLine(line)) {
                    String sender = extractSenderFromHeader(line);
                    if (StringUtils.isNotBlank(sender)) {
                        currentSender = sender;
                    }
                }

                if (!containsIgnoreCase(line, keyword)) {
                    continue;
                }

                HashMap<String, Object> map = new HashMap<>(8, 1.0f);
                map.put("name", file.getName());
                map.put("lineNumber", lineNumber);
                map.put("sender", currentSender);
                map.put("content", normalizeRecordContent(line));
                map.put("relativePath", toRelativePath(file, rootDirectory));
                map.put("url", buildFileAccessUrl(file, rootDirectory));
                result.add(map);

                if (result.size() >= limit) {
                    return;
                }
            }
        } catch (IOException e) {
            log.error("搜索聊天记录异常，file -> {}", file.getAbsolutePath(), e);
        }
    }

    /**
     * 判断是否为记录头行
     */
    private boolean isHeaderLine(String line) {
        return StringUtils.isNotBlank(line) && line.startsWith("#### [");
    }

    /**
     * 从markdown头中提取发送者
     */
    private String extractSenderFromHeader(String line) {
        int endIndex = line.indexOf("] ");
        if (endIndex < 0 || endIndex + 2 >= line.length()) {
            return "";
        }

        String sender = line.substring(endIndex + 2).trim();
        if (sender.endsWith("：") || sender.endsWith(":")) {
            sender = sender.substring(0, sender.length() - 1);
        }
        return sender;
    }

    /**
     * 清洗markdown行内容，提取消息正文
     */
    private String normalizeRecordContent(String line) {
        if (line == null) {
            return "";
        }
        String content = line.trim();
        if (content.startsWith(">")) {
            content = content.substring(1).trim();
        }
        if (content.startsWith("![](")) {
            return "[图片]";
        }
        if (content.startsWith("#### [")) {
            return "";
        }
        return content;
    }

    /**
     * 构建文件访问地址
     */
    private String buildFileAccessUrl(File file, File rootDirectory) {
        String relativePath = toRelativePath(file, rootDirectory);
        String prefix = accessAddress.endsWith("/") ? accessAddress : accessAddress + "/";
        return prefix + relativePath;
    }

    /**
     * 获取相对路径（统一使用 / 分隔）
     */
    private String toRelativePath(File file, File rootDirectory) {
        Path rootPath = Paths.get(rootDirectory.getAbsolutePath());
        Path filePath = Paths.get(file.getAbsolutePath());
        return rootPath.relativize(filePath).toString().replace(File.separatorChar, '/');
    }

    /**
     * 忽略大小写查找
     */
    private boolean containsIgnoreCase(String source, String keyword) {
        if (source == null || keyword == null) {
            return false;
        }
        return source.toLowerCase().contains(keyword.toLowerCase());
    }

    /**
     * 限制最大返回条数，防止一次性读取过多
     */
    private int normalizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return 50;
        }
        return Math.min(limit, 200);
    }
}
