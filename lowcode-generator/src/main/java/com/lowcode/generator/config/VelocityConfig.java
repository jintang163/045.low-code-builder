package com.lowcode.generator.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.runtime.resource.loader.FileResourceLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Slf4j
@Configuration
public class VelocityConfig {

    @Bean
    public VelocityEngine velocityEngine() {
        log.info("初始化 Velocity 模板引擎");
        Properties properties = new Properties();

        properties.setProperty("resource.loaders", "class, file");

        properties.setProperty("resource.loader.class.class", ClasspathResourceLoader.class.getName());
        properties.setProperty("resource.loader.class.cache", "true");
        properties.setProperty("resource.loader.class.modificationCheckInterval", "2");

        properties.setProperty("resource.loader.file.class", FileResourceLoader.class.getName());
        properties.setProperty("resource.loader.file.path", "templates/");
        properties.setProperty("resource.loader.file.cache", "true");
        properties.setProperty("resource.loader.file.modificationCheckInterval", "2");

        properties.setProperty("input.encoding", "UTF-8");
        properties.setProperty("output.encoding", "UTF-8");
        properties.setProperty("file.resource.loader.unicode", "true");

        properties.setProperty("runtime.log", "");
        properties.setProperty("runtime.log.logsystem.class",
                "org.apache.velocity.runtime.log.LogChuteLogSystem");
        properties.setProperty("runtime.log.logsystem.log4j.category", "velocity");
        properties.setProperty("runtime.log.invalid.reference", "false");

        properties.setProperty("directive.set.null.allowed", "true");
        properties.setProperty("space.gobbling", "bc");
        properties.setProperty("linebreak.string", "\n");

        properties.setProperty("velocimacro.library", "velocity_macro_lib.vm");
        properties.setProperty("velocimacro.permissions.allow.inline", "true");
        properties.setProperty("velocimacro.permissions.allow.inline.to.replace.global", "true");
        properties.setProperty("velocimacro.context.localscope", "true");

        properties.setProperty("parser.pool.size", "20");

        VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.init(properties);

        log.info("Velocity 模板引擎初始化完成，模板加载路径: classpath:/templates/, file:templates/");
        return velocityEngine;
    }
}
