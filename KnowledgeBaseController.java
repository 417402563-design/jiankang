package cn.edu.sdua.jiankang.controller;

import cn.edu.sdua.jiankang.dto.ApiResponse;
import cn.edu.sdua.jiankang.dto.DocumentView;
import cn.edu.sdua.jiankang.entity.User;
import cn.edu.sdua.jiankang.repository.UserRepository;
import cn.edu.sdua.jiankang.service.KnowledgeBaseService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识库管理接口(需 ADMIN/EDITOR 上传/删除,读接口登录即可)
 */
@RestController
@RequestMapping("/api/kb")
public class KnowledgeBaseController {

    private final KnowledgeBaseService kbService;
    private final UserRepository userRepository;

    public KnowledgeBaseController(KnowledgeBaseService kbService, UserRepository userRepository) {
        this.kbService = kbService;
        this.userRepository = userRepository;
    }

    @PostMapping("/upload")
    public ApiResponse<DocumentView> upload(@RequestParam("file") MultipartFile file,
                                            @RequestParam(value = "category", required = false) String category) {
        User owner = currentUser();
        return ApiResponse.ok("上传成功", kbService.ingest(file, category, owner));
    }

    @GetMapping("/documents")
    public ApiResponse<List<DocumentView>> documents() {
        return ApiResponse.ok(kbService.listAll());
    }

    @GetMapping("/categories")
    public ApiResponse<List<String>> categories() {
        return ApiResponse.ok(kbService.listCategories());
    }

    @DeleteMapping("/documents/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        kbService.delete(id);
        return ApiResponse.ok("已删除", null);
    }

    private User currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElse(null);
    }
}
