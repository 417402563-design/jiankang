package cn.edu.sdua.jiankang.service;

import cn.edu.sdua.jiankang.config.EmbeddingProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 文本向量化模块 —— 调用 Sentence-BERT 编码服务
 * 请求体:{"text": ["句子1", "句子2"]}
 * 期望响应:[ [向量1], [向量2] ] 或 {"embeddings": [[...]]}
 * 当远程服务不可用时,降级为基于哈希的确定性伪向量,保证系统离线可演示。
 */
@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private final EmbeddingProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EmbeddingService(EmbeddingProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder().build();
    }

    public List<float[]> embed(List<String> texts) {
        List<float[]> result = new ArrayList<>();
        try {
            String body = objectMapper.writeValueAsString(Map.of("text", texts));
            String resp = restClient.post()
                    .uri(properties.getUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(resp);
            JsonNode arr = root.isArray() ? root : root.path("embeddings");
            for (JsonNode vec : arr) {
                float[] v = new float[vec.size()];
                for (int i = 0; i < vec.size(); i++) {
                    v[i] = (float) vec.get(i).asDouble();
                }
                result.add(v);
            }
            if (result.size() != texts.size()) {
                throw new IllegalStateException("返回向量数量与输入不一致");
            }
            return result;
        } catch (Exception e) {
            log.warn("Sentence-BERT API 不可用,使用本地降级向量: {}", e.getMessage());
            for (String t : texts) {
                result.add(fallbackEmbedding(t));
            }
            return result;
        }
    }

    public float[] embedOne(String text) {
        return embed(List.of(text)).get(0);
    }

    /** 离线降级:用文本哈希生成稳定的伪向量并做归一化 */
    private float[] fallbackEmbedding(String text) {
        int dim = properties.getDimensions();
        float[] v = new float[dim];
        String s = text == null ? "" : text;
        java.util.Random rnd = new java.util.Random(s.hashCode() * 31L + s.length());
        for (int i = 0; i < dim; i++) {
            v[i] = rnd.nextFloat() * 2 - 1;
        }
        return normalize(v);
    }

    private float[] normalize(float[] v) {
        double sum = 0;
        for (float x : v) sum += x * x;
        double norm = Math.sqrt(sum);
        if (norm == 0) return v;
        for (int i = 0; i < v.length; i++) v[i] = (float) (v[i] / norm);
        return v;
    }
}
