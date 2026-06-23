package io.subbu.ai.firedrill.config;

import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Configuration for SPA (Single Page Application) routing.
 * Forwards all non-API, non-static paths to index.html for client-side routing.
 */
@Configuration
public class SpaWebConfig {

    /**
     * Fallback: catch any 404 that wasn't handled by explicit mappings below.
     */
    @Bean
    public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> containerCustomizer() {
        return container -> {
            container.addErrorPages(new ErrorPage(HttpStatus.NOT_FOUND, "/notFound"));
        };
    }

    @Controller
    public static class SpaViewController {

        /**
         * Explicit SPA routes — served directly without generating a 404 first.
         * These are all the known React Router paths in the frontend.
         */
        @RequestMapping(value = {
            "/",
            "/login",
            "/dashboard",
            "/candidates",
            "/jobs",
            "/upload",
            "/matching",
            "/skills",
            "/users",
            "/employees",
            "/admin",
            "/admin/**",
            "/unauthorized",
        })
        public String spa() {
            return "forward:/index.html";
        }

        /** Catch-all for any remaining 404 forwards from ErrorPage. */
        @RequestMapping("/notFound")
        public String forward() {
            return "forward:/index.html";
        }
    }
}
