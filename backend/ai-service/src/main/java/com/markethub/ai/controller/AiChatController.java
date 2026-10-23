package com.markethub.ai.controller;

import com.markethub.ai.dto.AskRequest;
import com.markethub.ai.dto.AskResponse;
import com.markethub.ai.service.ChatService;
import com.markethub.ai.service.KnowledgeBaseService;
import com.markethub.ai.service.OllamaClient;
import com.markethub.common.constant.PermissionConstants;
import com.markethub.common.exception.BusinessException;
import com.markethub.common.exception.R;
import com.markethub.common.security.CurrentUser;
import com.markethub.common.security.CurrentUserId;
import com.markethub.common.security.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI 助手", description = "基于本地 Ollama 与知识库的问答助手")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final ChatService chatService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final OllamaClient ollamaClient;

    @Operation(summary = "向助手提问")
    @PostMapping("/ask")
    @RequirePermission(PermissionConstants.AI_CHAT)
    public R<AskResponse> ask(@Valid @RequestBody AskRequest request, @CurrentUserId Integer userId) {
        return R.success(chatService.ask(userId, request.getQuestion()));
    }

    @Operation(summary = "查看知识库与模型状态（管理员）")
    @GetMapping("/admin/status")
    public R<KnowledgeBaseService.Status> status() {
        requireAdmin();
        return R.success(knowledgeBaseService.status());
    }

    @Operation(summary = "重建知识库索引（管理员，同步等待向量化跑完）")
    @PostMapping("/admin/reindex")
    public R<KnowledgeBaseService.ReindexResult> reindex() {
        requireAdmin();
        return R.success(knowledgeBaseService.reindex());
    }

    @Operation(summary = "预热对话模型（管理员）：把模型加载进显存，演示前先跑一次")
    @PostMapping("/admin/warmup")
    public R<Long> warmup() {
        requireAdmin();
        long startedAt = System.currentTimeMillis();
        ollamaClient.chat("你是一个连通性测试助手，只回复一个字：好。", "在吗");
        return R.success(System.currentTimeMillis() - startedAt);
    }

    private void requireAdmin() {
        if (!CurrentUser.isAdmin()) {
            throw new BusinessException(403, "仅管理员可操作");
        }
    }
}
