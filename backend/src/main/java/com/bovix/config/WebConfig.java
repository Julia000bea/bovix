package com.bovix.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Expõe a pasta de uploads (fotos dos animais) como arquivos estáticos,
 * acessíveis em http://localhost:8080/uploads/animais/<arquivo>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${bovix.uploads-dir}")
    private String uploadsDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/animais/**")
                .addResourceLocations("file:" + uploadsDir + "/");
    }
}
