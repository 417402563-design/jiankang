package cn.edu.sdua.jiankang.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 知识库文档元数据
 */
@Entity
@Table(name = "kb_document")
@Getter
@Setter
public class KnowledgeDoc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String fileName;

    /** 分类,如 犬科/猫科/喂养/疾病 */
    @Column(length = 80)
    private String category;

    /** 文件类型:pdf/docx/xlsx/txt */
    @Column(length = 30)
    private String fileType;

    private Long fileSize;

    /** 存储路径 */
    @Column(length = 500)
    private String storagePath;

    /** 解析出的纯文本 */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String rawText;

    /** 切片数量 */
    private Integer chunkCount = 0;

    /** 处理状态:PENDING/PROCESSING/DONE/FAILED */
    @Column(length = 20)
    private String status = "PENDING";

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocChunk> chunks = new ArrayList<>();
}
