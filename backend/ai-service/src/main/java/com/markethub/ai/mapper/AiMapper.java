package com.markethub.ai.mapper;

import com.markethub.ai.entity.AiChatLog;
import com.markethub.ai.entity.AiDocChunk;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AiMapper {

    @Select("SELECT COUNT(*) FROM ai_doc_chunk")
    int countChunks();

    @Select("SELECT * FROM ai_doc_chunk ORDER BY id")
    List<AiDocChunk> findAllChunks();

    @Delete("DELETE FROM ai_doc_chunk")
    int deleteAllChunks();

    @Insert("<script>INSERT INTO ai_doc_chunk (source, kind, heading, chunk_index, content, embedding, create_time) VALUES " +
            "<foreach collection='list' item='c' separator=','>(#{c.source}, #{c.kind}, #{c.heading}, #{c.chunkIndex}, #{c.content}, #{c.embedding}, NOW())</foreach></script>")
    int batchInsertChunks(@Param("list") List<AiDocChunk> chunks);

    @Insert("INSERT INTO ai_chat_log (user_id, question, answer, sources, latency_ms, create_time) " +
            "VALUES (#{userId}, #{question}, #{answer}, #{sources}, #{latencyMs}, NOW())")
    int insertChatLog(AiChatLog log);
}
