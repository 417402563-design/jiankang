package cn.edu.sdua.jiankang.service;

import java.util.List;
import java.util.Map;

/**
 * 向量存储与检索模块抽象接口(Chroma / Milvus 通用)
 */
public interface VectorStoreService {

    /** 写入或更新一条向量,附带原文与元数据 */
    void upsert(String id, float[] vector, String content, Map<String, Object> metadata);

    /** 删除指定向量 */
    void delete(String id);

    /** 按元数据过滤删除(如按 documentId 清理整篇文档向量) */
    void deleteByMetadata(Map<String, Object> filter);

    /** 向量相似度检索,filter 可为空 */
    List<SearchHit> query(float[] vector, int topK, Map<String, Object> filter);

    /** 检索命中结果 */
    record SearchHit(String id, double score, String content, Map<String, Object> metadata) {
    }
}
