package cn.edu.sdua.jiankang.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Sentence-BERT 向量化 API 配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "embedding.api")
public class EmbeddingProperties {

    /** 向量化服务端点,接收 {text:[...]} 返回向量数组 */
    private String url = "http://localhost:8000/encode";
    private int timeoutMs = 30000;
    private int dimensions = 384;
}
