package cn.edu.sdua.jiankang.service;

import cn.edu.sdua.jiankang.config.VectorStoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 向量存储实现:优先使用 Chroma REST API,不可用时降级为进程内向量库(余弦相似度)。
 * 通过 vector.store.type=milvus 可切换思路一致(此处保留 Chroma 实现作为默认)。
 */
@Service
public class ChromaVectorStoreService implements VectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(ChromaVectorStoreService.class);

    private final VectorStoreProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 缓存 collection 内部 id */
    private volatile String collectionId;
    /** 进程内降级存储 */
    private final Map<String, Stored> localStore = new ConcurrentHashMap<>();

    public ChromaVectorStoreService(VectorStoreProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public void upsert(String id, float[] vector, String content, Map<String, Object> metadata) {
        localStore.put(id, new Stored(vector, content, metadata));
        try {
            String cid = ensureCollection();
            Map<String, Object> body = Map.of(
                    "ids", List.of(id),
                    "embeddings", List.of(toList(vector)),
                    "documents", List.of(content == null ? "" : content),
                    "metadatas", List.of(metadata == null ? Map.of() : metadata));
            postJson("/api/v1/collections/" + cid + "/upsert", body);
        } catch (Exception e) {
            log.warn("Chroma upsert 失败,已写入本地向量库: {}", e.getMessage());
        }
    }

    @Override
    public void delete(String id) {
        localStore.remove(id);
        try {
            String cid = ensureCollection();
            postJson("/api/v1/collections/" + cid + "/delete", Map.of("ids", List.of(id)));
        } catch (Exception e) {
            log.warn("Chroma delete 失败,仅删除本地: {}", e.getMessage());
        }
    }

    @Override
    public void deleteByMetadata(Map<String, Object> filter) {
        localStore.entrySet().removeIf(e -> matches(e.getValue().metadata, filter));
        // Chroma v1 支持 where 删除的部分版本可用,此处以本地清理为主
    }

    @Override
    public List<SearchHit> query(float[] vector, int topK, Map<String, Object> filter) {
        try {
            String cid = ensureCollection();
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("query_embeddings", List.of(toList(vector)));
            body.put("n_results", topK);
            if (filter != null && !filter.isEmpty()) {
                body.put("where", filter);
            }
            String resp = postJson("/api/v1/collections/" + cid + "/query", body);
            return parseQueryResponse(resp);
        } catch (Exception e) {
            log.warn("Chroma query 失败,使用本地向量检索: {}", e.getMessage());
            return localQuery(vector, topK, filter);
        }
    }

    // ===== 内部方法 =====

    private String ensureCollection() throws Exception {
        if (collectionId != null) {
            return collectionId;
        }
        String base = properties.getChroma().getUrl();
        String name = properties.getCollection();
        // 尝试获取已存在集合
        try {
            String get = restClient.get()
                    .uri(base + "/api/v1/collections/" + name + "/get")
                    .retrieve().body(String.class);
            collectionId = objectMapper.readTree(get).path("id").asText(name);
            return collectionId;
        } catch (Exception ignore) {
            // 不存在则创建
        }
        String created = restClient.post()
                .uri(base + "/api/v1/collections")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("name", name, "get_or_create", true,
                        "metadata", Map.of("hnsw:space", "cosine")))
                .retrieve().body(String.class);
        JsonNode node = objectMapper.readTree(created);
        collectionId = node.path("id").asText(name);
        return collectionId;
    }

    private String postJson(String path, Map<String, Object> body) {
        return restClient.post()
                .uri(properties.getChroma().getUrl() + path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);
    }

    private List<SearchHit> parseQueryResponse(String resp) throws Exception {
        JsonNode root = objectMapper.readTree(resp);
        JsonNode ids = root.path("ids").path(0);
        JsonNode distances = root.path("distances").path(0);
        JsonNode documents = root.path("documents").path(0);
        JsonNode metadatas = root.path("metadatas").path(0);
        List<SearchHit> hits = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            double dist = distances.path(i).asDouble(1.0);
            double score = 1.0 - dist; // 余弦距离转相似度
            Map<String, Object> meta = metadatas.path(i).isObject()
                    ? objectMapper.convertValue(metadatas.get(i), Map.class) : Map.of();
            hits.add(new SearchHit(ids.get(i).asText(), score,
                    documents.path(i).asText(""), meta));
        }
        return hits;
    }

    private List<SearchHit> localQuery(float[] vector, int topK, Map<String, Object> filter) {
        return localStore.entrySet().stream()
                .filter(e -> matches(e.getValue().metadata, filter))
                .map(e -> new SearchHit(e.getKey(), cosine(vector, e.getValue().vector),
                        e.getValue().content, e.getValue().metadata))
                .sorted(Comparator.comparingDouble(SearchHit::score).reversed())
                .limit(topK)
                .collect(Collectors.toList());
    }

    private boolean matches(Map<String, Object> meta, Map<String, Object> filter) {
        if (filter == null || filter.isEmpty()) return true;
        if (meta == null) return false;
        for (Map.Entry<String, Object> f : filter.entrySet()) {
            Object v = meta.get(f.getKey());
            if (v == null || !String.valueOf(v).equals(String.valueOf(f.getValue()))) {
                return false;
            }
        }
        return true;
    }

    private double cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return 0;
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    private List<Float> toList(float[] v) {
        List<Float> list = new ArrayList<>(v.length);
        for (float x : v) list.add(x);
        return list;
    }

    private record Stored(float[] vector, String content, Map<String, Object> metadata) {
    }
}
