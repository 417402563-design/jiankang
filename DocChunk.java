package cn.edu.sdua.jiankang.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 文档切片(向量化单元),记录在向量库中的主键
 */
@Entity
@Table(name = "kb_chunk")
@Getter
@Setter
public class DocChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private KnowledgeDoc document;

    /** 切片在文档内的序号 */
    private Integer chunkIndex;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /** 向量库(Chroma/Milvus)中对应的向量 ID */
    @Column(length = 120)
    private String vectorId;
}
