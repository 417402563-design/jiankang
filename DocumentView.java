package cn.edu.sdua.jiankang.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 知识库文档展示 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentView {

    private Long id;
    private String fileName;
    private String category;
    private String fileType;
    private Long fileSize;
    private Integer chunkCount;
    private String status;
    private String owner;
    private Instant createdAt;
}
