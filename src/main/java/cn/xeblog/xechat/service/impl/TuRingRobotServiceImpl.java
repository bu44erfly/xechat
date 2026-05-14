package cn.xeblog.xechat.service.impl;

import cn.xeblog.xechat.service.RobotService;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;

/**
 * 使用 DeepSeek 聊天接口的实现
 *
 * @author yanpanyi
 * @date 2019/4/9
 */
@Service
@Slf4j
public class TuRingRobotServiceImpl implements RobotService {

    @Resource
    private RestTemplate restTemplate;

    /**
     * api地址
     */
    @Value("${deepseek.apiUrl}")
    private String apiUrl;

    /**
     * apikey
     */
    @Value("${deepseek.apiKey}")
    private String apiKey;

    /**
     * 模型名称
     */
    @Value("${deepseek.model:deepseek-v4-pro}")
    private String model;

    @Override
    public String sendMessage(String userId, String text) {
        ResponseEntity<JSONObject> resp = restTemplate.exchange(apiUrl, HttpMethod.POST,
                buildHttpEntity(text), JSONObject.class);

        return parseData(resp);
    }

    /**
     * 构建请求实体
     *
     * @param text 消息内容
     * @return HttpEntity
     */
    private HttpEntity buildHttpEntity(String text) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "You are a helpful assistant.");

        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", text);

        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("messages", new Object[]{systemMessage, userMessage});
        body.put("thinking", new JSONObject() {{
            put("type", "enabled");
        }});
        body.put("reasoning_effort", "high");
        body.put("stream", false);

        return new HttpEntity(body, headers);
    }

    /**
     * 解析响应数据
     *
     * @param resp 响应数据
     * @return 解析后的字符串
     */
    private String parseData(ResponseEntity<JSONObject> resp) {
        if (resp.getStatusCodeValue() != HttpStatus.SC_OK) {
            return "机器人暂时无法响应，请稍后再试。";
        }

        JSONObject data = resp.getBody();
        log.debug("data -> {}", data);
        if (data == null) {
            return "机器人暂时无法响应，请稍后再试。";
        }

        if (data.getJSONArray("choices") == null || data.getJSONArray("choices").isEmpty()) {
            return "机器人暂时无法响应，请稍后再试。";
        }

        JSONObject firstChoice = data.getJSONArray("choices").getJSONObject(0);
        if (firstChoice == null || firstChoice.getJSONObject("message") == null) {
            return "机器人暂时无法响应，请稍后再试。";
        }

        String content = firstChoice.getJSONObject("message").getString("content");
        if (content == null || content.trim().isEmpty()) {
            return "机器人暂时无法响应，请稍后再试。";
        }
        return content;
    }
}
