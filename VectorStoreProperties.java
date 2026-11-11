package cn.edu.sdua.jiankang.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 向量库配置(Chroma / Milvus)
 */
@Data
@Component
@ConfigurationProperties(prefix = "vector.store")
public class VectorStoreProperties {

    /** chroma 或 milvus */
    private String type = "chroma";
    private Chroma chroma = new Chroma();
    private Milvus milvus = new Milvus();
    private String collection = "pet_health_kb";

    @Data
    public static class Chroma {
        private String url = "http://localhost:8001";
    }

    @Data
    public static class Milvus {
        private String host = "localhost";
        private int port = 19530;
    }
}
