package edu.bjfu.journal.config;

import edu.bjfu.journal.security.PermInterceptor;
import edu.bjfu.journal.security.SessionAuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private SessionAuthInterceptor sessionAuthInterceptor;

    @Autowired
    private PermInterceptor permInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sessionAuthInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/auth/login", "/error", "/static/**");

        registry.addInterceptor(permInterceptor)
                .addPathPatterns("/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 如需静态资源，可放 src/main/resources/static
        registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
    }
}
