package cn.xeblog.xechat.controller;

import cn.xeblog.xechat.cache.ChannelCache;
import cn.xeblog.xechat.domain.vo.ResponseVO;
import cn.xeblog.xechat.enums.CodeEnum;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/channel")
public class ChannelController {

    @PostMapping("/create")
    public ResponseVO create(@RequestBody JSONObject body) {
        String channelId = body == null ? null : body.getString("channelId");
        if (StringUtils.isBlank(channelId)) {
            return new ResponseVO(CodeEnum.INVALID_PARAMETERS);
        }
        String id = channelId.trim();
        if (!isValidId(id)) {
            return new ResponseVO(CodeEnum.INVALID_PARAMETERS);
        }
        if (ChannelCache.exists(id)) {
            return new ResponseVO(CodeEnum.CHANNEL_ALREADY_EXISTS);
        }
        ChannelCache.create(id);
        JSONObject data = new JSONObject();
        data.put("channelId", id);
        return new ResponseVO(data);
    }

    @PostMapping("/join")
    public ResponseVO join(@RequestBody JSONObject body) {
        String channelId = body == null ? null : body.getString("channelId");
        if (StringUtils.isBlank(channelId)) {
            return new ResponseVO(CodeEnum.INVALID_PARAMETERS);
        }
        String id = channelId.trim();
        if (!isValidId(id)) {
            return new ResponseVO(CodeEnum.INVALID_PARAMETERS);
        }
        if (!ChannelCache.exists(id)) {
            return new ResponseVO(CodeEnum.CHANNEL_NOT_FOUND);
        }
        JSONObject data = new JSONObject();
        data.put("channelId", id);
        return new ResponseVO(data);
    }

    private boolean isValidId(String id) {
        if (id == null) {
            return false;
        }
        if (id.length() < 1 || id.length() > 32) {
            return false;
        }
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')
                    || c == '_' || c == '-';
            if (!ok) {
                return false;
            }
        }
        return true;
    }
}
