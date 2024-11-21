package com.learn.summer.boot;

import com.learn.summer.io.PropertyResolver;
import com.learn.summer.utils.ClassPathUtils;
import com.learn.summer.web.ContextLoaderInitializer;
import com.learn.summer.web.utils.WebUtils;
import org.apache.catalina.Context;
import org.apache.catalina.Server;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.nio.file.Paths;
import java.util.Set;

public class SummerApplication {
    final Logger logger = LoggerFactory.getLogger(SummerApplication.class);

    public static void run(String webDir, String baseDir, Class<?> configClass, String... args) throws Exception{
        new SummerApplication().start(webDir, baseDir, configClass, args);
    }

    public void start(String webDir, String baseDir, Class<?> configClass, String... args) throws Exception{
        printBanner();

        // start info
        final long startTime = System.currentTimeMillis();
        final int javaVersion = Runtime.version().feature();
        final long pid = ManagementFactory.getRuntimeMXBean().getPid();
        final String user = System.getProperty("user.name");
        final String pwd = Paths.get("").toAbsolutePath().toString();
        logger.info("Starting {} using Java {} with PID {} (started by {} in {})",
                configClass.getSimpleName(), javaVersion, pid, user, pwd);

        // 读取 application.yml 或 application.properties 配置
        var propertyResolver = WebUtils.createPropertyResolver();
        // 创建 Tomcat 服务器
        var server = startTomcat(webDir, baseDir, configClass, propertyResolver);

        // started info
        final long endTime = System.currentTimeMillis();
        final String appTime = String.format("%.3f", (endTime - startTime) / 1000.0);
        final String jvmTime = String.format("%.3f", ManagementFactory.getRuntimeMXBean().getUptime() / 1000.0);
        logger.info("Started {} in {} seconds (process running for {})",
                configClass.getSimpleName(), appTime, jvmTime);

        // 等待服务器结束
        server.await();
    }

    protected Server startTomcat(String webDir, String baseDir, Class<?> configClass,
                                 PropertyResolver propertyResolver) throws Exception{
        int port = propertyResolver.getProperty("${server.port:8080}", int.class);
        // 实例化Tomcat Server
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);
        // 设置 Connector
        tomcat.getConnector().setThrowOnFailure(true);
        // 添加一个默认的 webapp，挂载在 '/'
        Context ctx = tomcat.addWebapp("", new File(webDir).getAbsolutePath());
        // 设置应用程序目录
        WebResourceRoot resources = new StandardRoot(ctx);
        resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes",
                new File(baseDir).getAbsolutePath(), "/"));
        ctx.setResources(resources);
        // 设置 ServletContainerInitializer 监听器
        ctx.addServletContainerInitializer(new ContextLoaderInitializer(configClass, propertyResolver), Set.of());
        // 启动服务器
        tomcat.start();
        return tomcat.getServer();
    }

    protected void printBanner() {
        String banner = ClassPathUtils.readString("/banner.txt");
        banner.lines().forEach(System.out::println);
    }
}
