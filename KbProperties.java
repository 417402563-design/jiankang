package cn.edu.sdua.jiankang.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 知识库切片与检索配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "kb")
public class KbProperties {

    private Chunk chunk = new Chunk();
    private Retrieval retrieval = new Retrieval();

    @Data
    public static class Chunk {
        private int maxChars = 500;
        private int overlap = 80;
    }

    @Data
    public static class Retrieval {
        private int topK = 5;
    }
}
