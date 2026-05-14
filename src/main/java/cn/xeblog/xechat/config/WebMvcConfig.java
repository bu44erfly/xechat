package cn.xeblog.xechat.config;

import cn.xeblog.xechat.interceptor.AdminAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * mvc配置
 *
 * @author yanpanyi
 * @date 2019/03/27
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final FileConfig fileConfig;
    private final AdminAuthInterceptor adminAuthInterceptor;

    public WebMvcConfig(FileConfig fileConfig, AdminAuthInterceptor adminAuthInterceptor) {
        this.fileConfig = fileConfig;
        this.adminAuthInterceptor = adminAuthInterceptor;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(fileConfig.getStaticAccessPath())
                .addResourceLocations("classpath:/META-INF/resources/", "classpath:/resources/", "classpath:/static/",
                        "file:" + fileConfig.getDirectoryMapping());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/login");
    }

}
