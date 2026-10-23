package com.markethub.ai.service;

import com.markethub.ai.config.AiProperties;
import com.markethub.ai.dto.AskResponse;
import com.markethub.ai.entity.AiChatLog;
import com.markethub.ai.mapper.AiMapper;
import com.markethub.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/** RAG 问答主流程：检索知识库片段后交给模型作答，每次问答落一行 ai_chat_log。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final String SYSTEM_PROMPT = """
            你是「MarketHub 二手交易平台」的在线客服助手，服务对象是平台上的买家和卖家。
            你的职责是解答与平台使用有关的问题：注册登录、浏览与搜索商品、下单、支付、取消订单、
            确认收货、评价、发布与审核商品、开店申请、平台交易规则。

            回答要求：
            1. 只依据下面提供的平台资料作答，不要引入资料之外的常识或猜测。
            2. 资料里找不到答案时，直接说「平台资料里没有相关内容」，并建议联系人工客服，不要编造。
            3. 用简体中文回答，条理清楚，能分点时分成短点，总长度控制在 300 字以内。
            4. 不要编造订单号、价格、时间、规则条款等具体信息。
            """;

    private static final String NO_KNOWLEDGE_ANSWER =
            "平台资料还没有建立索引，暂时无法回答。请联系管理员执行一次索引重建。";

    private final AiMapper aiMapper;
    private final OllamaClient ollamaClient;
    private final VectorIndex vectorIndex;
    private final AiProperties properties;

    public AskResponse ask(Integer userId, String rawQuestion) {
        String question = rawQuestion == null ? "" : rawQuestion.trim();
        if (question.isEmpty()) {
            throw new BusinessException(400, "问题不能为空");
        }
        int maxLength = properties.getChat().getMaxQuestionLength();
        if (question.length() > maxLength) {
            throw new BusinessException(400, "问题太长了，请控制在 " + maxLength + " 字以内");
        }

        long startedAt = System.currentTimeMillis();
        List<VectorIndex.Scored> hits = vectorIndex.search(
                ollamaClient.embed(question),
                properties.getKnowledge().getTopK(),
                properties.getKnowledge().getHelpBoost());

        String answer = hits.isEmpty()
                ? NO_KNOWLEDGE_ANSWER
                : ollamaClient.chat(SYSTEM_PROMPT, buildUserPrompt(question, hits));

        long latency = System.currentTimeMillis() - startedAt;
        recordChatLog(userId, question, answer, hits, latency);
        return new AskResponse(answer, latency);
    }

    private String buildUserPrompt(String question, List<VectorIndex.Scored> hits) {
        StringBuilder prompt = new StringBuilder("以下是平台资料片段：\n\n");
        int no = 1;
        for (VectorIndex.Scored hit : hits) {
            prompt.append("【资料 ").append(no++).append("】出处：").append(hit.chunk().getSource());
            if (hit.chunk().getHeading() != null && !hit.chunk().getHeading().isEmpty()) {
                prompt.append(" › ").append(hit.chunk().getHeading());
            }
            prompt.append('\n').append(hit.chunk().getContent()).append("\n\n");
        }
        prompt.append("请只根据以上资料回答下面的问题。资料里没有的内容就说不知道。\n");
        prompt.append("用户问题：").append(question);
        return prompt.toString();
    }

    private void recordChatLog(Integer userId, String question, String answer,
                               List<VectorIndex.Scored> hits, long latency) {
        try {
            AiChatLog chatLog = new AiChatLog();
            chatLog.setUserId(userId);
            chatLog.setQuestion(question);
            chatLog.setAnswer(answer);
            chatLog.setSources(hits.stream()
                    .map(hit -> hit.chunk().getSource() + " › " + hit.chunk().getHeading()
                            + " (" + String.format("%.3f", hit.score()) + ")")
                    .collect(Collectors.joining(" | ")));
            chatLog.setLatencyMs((int) latency);
            aiMapper.insertChatLog(chatLog);
        } catch (Exception e) {
            log.warn("问答日志写入失败（不影响回答）：{}", e.getMessage());
        }
    }
}
