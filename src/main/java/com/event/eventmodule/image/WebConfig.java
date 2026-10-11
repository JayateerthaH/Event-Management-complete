package com.event.eventmodule.image;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.images.url-prefix}")
    private String urlPrefix;

    @Value("${app.images.folder}")
    private String folder;

    @Value("${app.images.cache}")
    private Integer cacheTime;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Resolve the folder to an absolute path so Spring can find it regardless
        // of how the application is launched (IDE, jar, Docker, Windows, Linux).
        Path absoluteFolder = Paths.get(folder).toAbsolutePath().normalize();
        String resourceLocation = "file:" + absoluteFolder + "/";

        registry.addResourceHandler(urlPrefix)
                .addResourceLocations(resourceLocation)
                .setCachePeriod(cacheTime);
    }
}
