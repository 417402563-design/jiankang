package cn.edu.sdua.jiankang.repository;

import cn.edu.sdua.jiankang.entity.DocChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocChunkRepository extends JpaRepository<DocChunk, Long> {

    List<DocChunk> findByDocumentId(Long documentId);

    void deleteByDocumentId(Long documentId);
}
