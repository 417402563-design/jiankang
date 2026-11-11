package cn.edu.sdua.jiankang.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 应用级配置:JWT / CORS / 上传目录
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();
    private String uploadDir = "./data/uploads";

    @Data
    public static class Jwt {
        private String secret;
        private long expirationMs = 86400000L;
    }

    @Data
    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:5173");
    }
}
