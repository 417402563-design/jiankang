package cn.edu.sdua.jiankang.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * 问答聊天记录
 */
@Entity
@Table(name = "chat_message")
@Getter
@Setter
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    /** 会话 ID,用于区分多轮对话 */
    @Column(length = 64)
    private String sessionId;

    /** 角色:user / assistant */
    @Column(nullable = false, length = 20)
    private String role;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
