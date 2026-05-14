package cn.xeblog.xechat.interceptor;

import cn.xeblog.xechat.cache.AdminTokenCache;
import cn.xeblog.xechat.enums.CodeEnum;
import cn.xeblog.xechat.exception.ErrorCodeException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    private static final String ADMIN_TOKEN = "X-Admin-Token";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader(ADMIN_TOKEN);
        if (AdminTokenCache.isValid(token)) {
            return true;
        }
        throw new ErrorCodeException(CodeEnum.INVALID_TOKEN);
    }
}

