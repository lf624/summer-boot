package com.learn.summer.web;

import com.learn.summer.context.AnnotationConfigApplicationContext;
import com.learn.summer.context.ApplicationContext;
import com.learn.summer.io.PropertyResolver;
import com.learn.summer.web.utils.WebUtils;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class ContextLoaderInitializer implements ServletContainerInitializer {
    final Logger logger = LoggerFactory.getLogger(getClass());

    final Class<?> configClass;
    final PropertyResolver propertyResolver;

    public ContextLoaderInitializer(Class<?> configClass, PropertyResolver propertyResolver) {
        this.configClass = configClass;
        this.propertyResolver = propertyResolver;
    }

    @Override
    public void onStartup(Set<Class<?>> set, ServletContext ctx) throws ServletException {
        logger.info("Servlet container start, ServletContext={}", ctx);

        String encoding = propertyResolver.getProperty("${summer.web.character-encoding:UTF-8}");
        ctx.setRequestCharacterEncoding(encoding);
        ctx.setResponseCharacterEncoding(encoding);

        // 设置ServletContext
        WebMvcConfiguration.setServletContext(ctx);
        // 启动 IoC 容器
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(
                this.configClass, this.propertyResolver);
        logger.info("Application context created: {}", applicationContext);
        // 注册 Filter 和 DispatcherServlet
        WebUtils.registerFilters(ctx);
        WebUtils.registerDispatcherServlet(ctx, this.propertyResolver);
    }
}
