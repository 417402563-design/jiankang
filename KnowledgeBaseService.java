package cn.edu.sdua.jiankang.service;

import cn.edu.sdua.jiankang.config.AppProperties;
import cn.edu.sdua.jiankang.config.KbProperties;
import cn.edu.sdua.jiankang.dto.DocumentView;
import cn.edu.sdua.jiankang.entity.DocChunk;
import cn.edu.sdua.jiankang.entity.KnowledgeDoc;
import cn.edu.sdua.jiankang.entity.User;
import cn.edu.sdua.jiankang.repository.DocChunkRepository;
import cn.edu.sdua.jiankang.repository.KnowledgeDocRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 知识库管理:文档上传 -> 解析 -> 切片 -> 向量化 -> 入向量库(编排各模块)
 */
@Service
public class KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseService.class);

    private final DocumentParsingService parsingService;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final KnowledgeDocRepository docRepository;
    private final DocChunkRepository chunkRepository;
    private final KbProperties kbProperties;
    private final AppProperties appProperties;

    public KnowledgeBaseService(DocumentParsingService parsingService,
                                EmbeddingService embeddingService,
                                VectorStoreService vectorStoreService,
                                KnowledgeDocRepository docRepository,
                                DocChunkRepository chunkRepository,
                                KbProperties kbProperties,
                                AppProperties appProperties) {
        this.parsingService = parsingService;
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.docRepository = docRepository;
        this.chunkRepository = chunkRepository;
        this.kbProperties = kbProperties;
        this.appProperties = appProperties;
    }

    @Transactional
    public DocumentView ingest(MultipartFile file, String category, User owner) {
        String fileName = file.getOriginalFilename();
        // 1. 解析文本
        String text = parsingService.extractText(file);
        // 2. 保存原始文件
        String storagePath = saveFile(file);
        // 3. 落库文档元数据
        KnowledgeDoc doc = new KnowledgeDoc();
        doc.setFileName(fileName);
        doc.setCategory(category);
        doc.setFileType(parsingService.resolveFileType(fileName));
        doc.setFileSize(file.getSize());
        doc.setStoragePath(storagePath);
        doc.setRawText(text);
        doc.setStatus("PROCESSING");
        doc.setOwner(owner);
        doc = docRepository.save(doc);

        try {
            // 4. 切片
            List<String> chunks = splitText(text);
            // 5. 批量向量化
            List<float[]> vectors = embeddingService.embed(chunks);
            List<DocChunk> entities = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                String vectorId = "doc_" + doc.getId() + "_chunk_" + i;
                // 6. 写入向量库
                Map<String, Object> meta = new HashMap<>();
                meta.put("documentId", doc.getId());
                meta.put("fileName", fileName);
                meta.put("category", category == null ? "" : category);
                meta.put("chunkIndex", i);
                vectorStoreService.upsert(vectorId, vectors.get(i), chunks.get(i), meta);

                DocChunk chunk = new DocChunk();
                chunk.setDocument(doc);
                chunk.setChunkIndex(i);
                chunk.setContent(chunks.get(i));
                chunk.setVectorId(vectorId);
                entities.add(chunk);
            }
            chunkRepository.saveAll(entities);
            doc.setChunkCount(chunks.size());
            doc.setStatus("DONE");
        } catch (Exception e) {
            log.error("文档向量化失败: {}", e.getMessage(), e);
            doc.setStatus("FAILED");
        }
        doc = docRepository.save(doc);
        return toView(doc);
    }

    @Transactional(readOnly = true)
    public List<DocumentView> listAll() {
        return docRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public List<String> listCategories() {
        return docRepository.findAll().stream()
                .map(KnowledgeDoc::getCategory)
                .filter(c -> c != null && !c.isBlank())
                .distinct().toList();
    }

    @Transactional
    public void delete(Long id) {
        KnowledgeDoc doc = docRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在: " + id));
        // 删除向量
        for (DocChunk c : chunkRepository.findByDocumentId(id)) {
            if (c.getVectorId() != null) {
                vectorStoreService.delete(c.getVectorId());
            }
        }
        chunkRepository.deleteByDocumentId(id);
        docRepository.delete(doc);
    }

    /** 按字符窗口切片,带重叠 */
    private List<String> splitText(String text) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return result;
        }
        int max = kbProperties.getChunk().getMaxChars();
        int overlap = kbProperties.getChunk().getOverlap();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + max, text.length());
            result.add(text.substring(start, end).trim());
            if (end >= text.length()) break;
            start = Math.max(end - overlap, start + 1);
        }
        return result;
    }

    private String saveFile(MultipartFile file) {
        try {
            Path dir = Paths.get(appProperties.getUploadDir());
            Files.createDirectories(dir);
            String name = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path target = dir.resolve(name);
            file.transferTo(target.toFile());
            return target.toString();
        } catch (IOException e) {
            log.warn("原始文件保存失败: {}", e.getMessage());
            return null;
        }
    }

    private DocumentView toView(KnowledgeDoc doc) {
        DocumentView v = new DocumentView();
        v.setId(doc.getId());
        v.setFileName(doc.getFileName());
        v.setCategory(doc.getCategory());
        v.setFileType(doc.getFileType());
        v.setFileSize(doc.getFileSize());
        v.setChunkCount(doc.getChunkCount());
        v.setStatus(doc.getStatus());
        v.setOwner(doc.getOwner() != null ? doc.getOwner().getUsername() : null);
        v.setCreatedAt(doc.getCreatedAt());
        return v;
    }
}
