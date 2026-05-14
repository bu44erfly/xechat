package cn.xeblog.xechat.controller;

import cn.xeblog.xechat.cache.AdminTokenCache;
import cn.xeblog.xechat.domain.ro.AdminLoginRO;
import cn.xeblog.xechat.domain.vo.ResponseVO;
import cn.xeblog.xechat.enums.CodeEnum;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Value("${admin.username:admin}")
    private String adminUsername;

    @Value("${admin.passwordMd5:}")
    private String adminPasswordMd5;

    @PostMapping("/login")
    public ResponseVO login(@RequestBody AdminLoginRO loginRO) {
        if (loginRO == null || StringUtils.isBlank(loginRO.getUsername()) || StringUtils.isBlank(loginRO.getPassword())) {
            return new ResponseVO(CodeEnum.INVALID_PARAMETERS);
        }

        boolean usernameOk = adminUsername.equals(loginRO.getUsername().trim());
        boolean passwordOk = !StringUtils.isBlank(adminPasswordMd5)
                && adminPasswordMd5.equalsIgnoreCase(DigestUtils.md5Hex(loginRO.getPassword()));

        if (!usernameOk || !passwordOk) {
            return new ResponseVO(CodeEnum.INVALID_TOKEN);
        }

        String token = AdminTokenCache.createToken();
        JSONObject data = new JSONObject();
        data.put("token", token);
        data.put("expireAt", AdminTokenCache.getExpireAt(token));
        return new ResponseVO(data);
    }
}

