package cn.edu.sdua.jiankang.service;

import cn.edu.sdua.jiankang.config.LlmProperties;
import cn.edu.sdua.jiankang.dto.ChatDto;
import cn.edu.sdua.jiankang.entity.ChatMessage;
import cn.edu.sdua.jiankang.repository.ChatMessageRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 问答服务:向量检索知识库 + 大模型生成(可选),并持久化对话历史。
 */
@Service
public class QaService {

    private static final Logger log = LoggerFactory.getLogger(QaService.class);

    private final RetrievalService retrievalService;
    private final ChatMessageRepository chatRepository;
    private final LlmProperties llmProperties;
    private final RestClient restClient = RestClient.builder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public QaService(RetrievalService retrievalService,
                     ChatMessageRepository chatRepository,
                     LlmProperties llmProperties) {
        this.retrievalService = retrievalService;
        this.chatRepository = chatRepository;
        this.llmProperties = llmProperties;
    }

    public ChatDto.ChatResponse ask(Long userId, ChatDto.ChatRequest req) {
        String sessionId = req.getSessionId() == null || req.getSessionId().isBlank()
                ? "default" : req.getSessionId();

        // 1. 检索相关知识
        List<ChatDto.RetrievedChunk> refs =
                retrievalService.retrieve(req.getQuestion(), req.getCategory(), req.getTopK());

        // 2. 生成答案:优先大模型,失败/未配置则返回检索拼接
        String answer = generateAnswer(req.getQuestion(), refs);

        // 3. 保存对话历史
        saveMessage(userId, sessionId, "user", req.getQuestion());
        saveMessage(userId, sessionId, "assistant", answer);

        ChatDto.ChatResponse resp = new ChatDto.ChatResponse();
        resp.setSessionId(sessionId);
        resp.setAnswer(answer);
        resp.setReferences(refs);
        return resp;
    }

    public List<ChatMessage> history(Long userId, String sessionId) {
        return chatRepository.findByUserIdAndSessionIdOrderByCreatedAtAsc(
                userId, sessionId == null ? "default" : sessionId);
    }

    private String generateAnswer(String question, List<ChatDto.RetrievedChunk> refs) {
        if (refs.isEmpty()) {
            return "知识库中暂未找到与「" + question + "」相关的内容,请先上传相关宠物健康文档。";
        }
        if (llmProperties.getUrl() != null && !llmProperties.getUrl().isBlank()) {
            try {
                return callLlm(question, refs);
            } catch (Exception e) {
                log.warn("大模型生成失败,降级为检索拼接: {}", e.getMessage());
            }
        }
        return composeFromChunks(question, refs);
    }

    /** 检索增强:将 top 片段拼接为带出处的回答 */
    private String composeFromChunks(String question, List<ChatDto.RetrievedChunk> refs) {
        StringBuilder sb = new StringBuilder();
        sb.append("根据知识库中与「").append(question).append("」最相关的内容:\n\n");
        int limit = Math.min(3, refs.size());
        for (int i = 0; i < limit; i++) {
            ChatDto.RetrievedChunk c = refs.get(i);
            sb.append(i + 1).append(". ").append(c.getContent()).append("\n");
            if (c.getFileName() != null) {
                sb.append("   —— 来源:《").append(c.getFileName()).append("》\n");
            }
        }
        return sb.toString().trim();
    }

    /** 调用 OpenAI 兼容的 chat/completions 接口 */
    private String callLlm(String question, List<ChatDto.RetrievedChunk> refs) throws Exception {
        StringBuilder context = new StringBuilder();
        for (ChatDto.RetrievedChunk c : refs) {
            context.append(c.getContent()).append("\n---\n");
        }
        String systemPrompt = "你是宠物智能健康管家,请依据以下知识库资料回答用户问题,不要编造。资料:\n"
                + context;
        Map<String, Object> body = new HashMap<>();
        body.put("model", llmProperties.getModel());
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", question)));
        String resp = restClient.post()
                .uri(llmProperties.getUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + llmProperties.getKey())
                .body(body)
                .retrieve()
                .body(String.class);
        JsonNode node = objectMapper.readTree(resp);
        return node.path("choices").path(0).path("message").path("content").asText(
                composeFromChunks(question, refs));
    }

    private void saveMessage(Long userId, String sessionId, String role, String content) {
        ChatMessage m = new ChatMessage();
        m.setUserId(userId);
        m.setSessionId(sessionId);
        m.setRole(role);
        m.setContent(content);
        chatRepository.save(m);
    }
}
