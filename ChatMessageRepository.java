package cn.edu.sdua.jiankang.repository;

import cn.edu.sdua.jiankang.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByUserIdAndSessionIdOrderByCreatedAtAsc(Long userId, String sessionId);
}
