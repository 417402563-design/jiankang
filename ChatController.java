package cn.edu.sdua.jiankang.controller;

import cn.edu.sdua.jiankang.dto.ApiResponse;
import cn.edu.sdua.jiankang.dto.ChatDto;
import cn.edu.sdua.jiankang.entity.ChatMessage;
import cn.edu.sdua.jiankang.entity.User;
import cn.edu.sdua.jiankang.repository.UserRepository;
import cn.edu.sdua.jiankang.service.QaService;
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI 问答聊天接口
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final QaService qaService;
    private final UserRepository userRepository;

    public ChatController(QaService qaService, UserRepository userRepository) {
        this.qaService = qaService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ApiResponse<ChatDto.ChatResponse> chat(@Valid @RequestBody ChatDto.ChatRequest req) {
        User me = currentUser();
        return ApiResponse.ok(qaService.ask(me.getId(), req));
    }

    @GetMapping("/history")
    public ApiResponse<List<ChatMessage>> history(@RequestParam(required = false) String sessionId) {
        User me = currentUser();
        return ApiResponse.ok(qaService.history(me.getId(), sessionId));
    }

    private User currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }
}
