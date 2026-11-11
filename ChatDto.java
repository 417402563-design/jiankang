package cn.edu.sdua.jiankang.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 问答相关 DTO
 */
public class ChatDto {

    @Data
    public static class ChatRequest {
        private String sessionId;
        @NotBlank(message = "问题不能为空")
        private String question;
        /** 可选:限定检索分类 */
        private String category;
        /** 可选:检索条数 */
        private Integer topK;
    }

    /** 检索命中的知识片段(问答引用 / 搜索结果通用) */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetrievedChunk {
        private String chunkId;
        private Long documentId;
        private String fileName;
        private String category;
        private String content;
        /** 相似度得分(0~1,越大越相似) */
        private double score;
    }

    @Data
    public static class ChatResponse {
        private String sessionId;
        private String answer;
        private List<RetrievedChunk> references;
    }
}
