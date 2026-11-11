package cn.edu.sdua.jiankang.service;

import cn.edu.sdua.jiankang.config.KbProperties;
import cn.edu.sdua.jiankang.dto.ChatDto;
import cn.edu.sdua.jiankang.dto.SearchDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 基于向量相似度的检索服务:向量化查询词 -> 向量库检索 -> 结果映射与关键词高亮
 */
@Service
public class RetrievalService {

    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final KbProperties kbProperties;

    public RetrievalService(EmbeddingService embeddingService,
                            VectorStoreService vectorStoreService,
                            KbProperties kbProperties) {
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.kbProperties = kbProperties;
    }

    /** 通用向量检索,返回命中片段 */
    public List<ChatDto.RetrievedChunk> retrieve(String query, String category, Integer topK) {
        int k = topK != null && topK > 0 ? topK : kbProperties.getRetrieval().getTopK();
        float[] vector = embeddingService.embedOne(query);
        Map<String, Object> filter = new HashMap<>();
        if (category != null && !category.isBlank()) {
            filter.put("category", category);
        }
        List<VectorStoreService.SearchHit> hits =
                vectorStoreService.query(vector, k, filter.isEmpty() ? null : filter);
        List<ChatDto.RetrievedChunk> chunks = new ArrayList<>();
        for (VectorStoreService.SearchHit h : hits) {
            Map<String, Object> meta = h.metadata() == null ? Map.of() : h.metadata();
            Long docId = toLong(meta.get("documentId"));
            chunks.add(new ChatDto.RetrievedChunk(
                    h.id(), docId,
                    str(meta.get("fileName")),
                    str(meta.get("category")),
                    h.content(),
                    round(h.score())));
        }
        return chunks;
    }

    /** 搜索:检索 + 关键词高亮 */
    public SearchDto.SearchResponse search(String query, String category, Integer topK) {
        List<ChatDto.RetrievedChunk> chunks = retrieve(query, category, topK);
        List<SearchDto.SearchItem> items = new ArrayList<>();
        for (ChatDto.RetrievedChunk c : chunks) {
            String highlighted = highlight(c.getContent(), query);
            items.add(new SearchDto.SearchItem(c.getChunkId(), c.getDocumentId(),
                    c.getFileName(), c.getCategory(), c.getContent(), c.getScore(), highlighted));
        }
        SearchDto.SearchResponse resp = new SearchDto.SearchResponse();
        resp.setQuery(query);
        resp.setTotal(items.size());
        resp.setItems(items);
        return resp;
    }

    /** 将查询词中的关键词在文本中用 <mark> 高亮 */
    private String highlight(String text, String query) {
        if (text == null || text.isBlank()) return "";
        String escaped = escapeHtml(text);
        List<String> terms = extractTerms(query);
        for (String term : terms) {
            if (term.length() < 2) continue;
            Pattern p = Pattern.compile(Pattern.quote(term),
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            escaped = p.matcher(escaped).replaceAll(m -> "<mark>" + m.group() + "</mark>");
        }
        return escaped;
    }

    private List<String> extractTerms(String query) {
        List<String> terms = new ArrayList<>();
        if (query == null) return terms;
        String q = query.trim();
        terms.add(q);
        for (String part : q.split("[\\s,，。.、;；]+")) {
            if (!part.isBlank() && !terms.contains(part)) {
                terms.add(part);
            }
        }
        return terms;
    }

    private String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private double round(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }

    private Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(o));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
