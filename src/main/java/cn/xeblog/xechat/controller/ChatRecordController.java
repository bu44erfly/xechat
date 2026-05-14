package cn.xeblog.xechat.controller;

import cn.xeblog.xechat.domain.vo.ResponseVO;
import cn.xeblog.xechat.service.ChatRecordService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 聊天记录
 *
 * @author yanpanyi
 * @date 2019/4/4
 */
@RestController
@RequestMapping("/api/record")
public class ChatRecordController {

    @Resource
    private ChatRecordService chatRecordService;

    /**
     * 聊天记录列表
     *
     * @param directoryName 目录名
     * @return ResponseVO
     */
    @GetMapping
    public ResponseVO listChatRecord(@RequestParam(required = false, defaultValue = "") String directoryName) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("list", chatRecordService.listRecord(directoryName));

        return new ResponseVO(jsonObject);
    }

    /**
     * 聊天记录搜索
     *
     * @param keyword 关键词
     * @param limit   返回数量限制
     * @return ResponseVO
     */
    @GetMapping("/search")
    public ResponseVO searchChatRecord(@RequestParam String keyword,
                                       @RequestParam(required = false, defaultValue = "50") Integer limit) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("list", chatRecordService.searchRecord(keyword, limit));
        return new ResponseVO(jsonObject);
    }
}
