package cn.edu.sdua.jiankang.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 向量检索 / 搜索 DTO
 */
public class SearchDto {

    @Data
    public static class SearchRequest {
        @NotBlank(message = "搜索关键词不能为空")
        private String query;
        private String category;
        private Integer topK;
    }

    /** 搜索结果条目:命中片段 + 高亮关键词位置 */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchItem {
        private String chunkId;
        private Long documentId;
        private String fileName;
        private String category;
        private String content;
        private double score;
        /** 前端可直接渲染的 HTML 片段(含 <mark> 高亮) */
        private String highlighted;
    }

    @Data
    public static class SearchResponse {
        private String query;
        private int total;
        private List<SearchItem> items;
    }
}
