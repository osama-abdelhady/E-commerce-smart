package com.tailoredplatform.ecommerce.config;

import com.tailoredplatform.ecommerce.security.jwt.CookieProperties;
import com.tailoredplatform.ecommerce.security.jwt.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ JwtProperties.class, CookieProperties.class, CorsProperties.class })
public class PropertiesConfig {
}
