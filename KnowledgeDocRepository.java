package cn.edu.sdua.jiankang.repository;

import cn.edu.sdua.jiankang.entity.KnowledgeDoc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KnowledgeDocRepository extends JpaRepository<KnowledgeDoc, Long> {

    List<KnowledgeDoc> findByCategory(String category);

    List<KnowledgeDoc> findAllByOrderByCreatedAtDesc();

    List<KnowledgeDoc> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}
