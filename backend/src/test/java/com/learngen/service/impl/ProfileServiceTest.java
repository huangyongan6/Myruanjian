package com.learngen.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.learngen.agent.AgentMessage;
import com.learngen.agent.Orchestrator;
import com.learngen.cache.RedisCacheSupport;
import com.learngen.mapper.StudentProfileMapper;
import com.learngen.model.StudentProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ProfileService 6 维深度合并测试（CLAUDE.md §11.3 关键能力）。
 */
class ProfileServiceTest {

    private StudentProfileMapper profileMapper;
    private Orchestrator orchestrator;
    private ObjectMapper objectMapper;
    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;
    private RedisCacheSupport cache;
    private ProfileServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        profileMapper = mock(StudentProfileMapper.class);
        orchestrator = mock(Orchestrator.class);
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        redis = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        cache = new RedisCacheSupport(redis, objectMapper);
        service = new ProfileServiceImpl(profileMapper, orchestrator, objectMapper, cache);
    }

    @Test
    void upsertFromAgent_noExisting_createsWithAllFields() {
        when(orchestrator.dispatch(any(), any())).thenReturn(AgentMessage.builder()
                .payload("{\"knowledge_base\":{\"math_level\":\"中等\"},\"cognitive_style\":{\"visual\":0.6},\"learning_goal\":{},\"weak_points\":{\"weak_topics\":[\"概率论\"]},\"learning_pace\":{},\"interest_area\":{\"areas\":[\"推荐系统\"]}}")
                .build());
        when(profileMapper.selectOne(any())).thenReturn(null);

        service.upsertFromAgent(1L, "对话内容");

        ArgumentCaptor<StudentProfile> captor = ArgumentCaptor.forClass(StudentProfile.class);
        verify(profileMapper).insert(captor.capture());
        StudentProfile saved = captor.getValue();
        assertEquals(1L, saved.getStudentId());
        assertTrue(saved.getKnowledgeBase().contains("中等"));
        assertTrue(saved.getWeakPoints().contains("概率论"));
        assertTrue(saved.getInterestArea().contains("推荐系统"));
    }

    @Test
    void upsertFromAgent_existing_mergesArrayFieldsAsUnion() throws Exception {
        StudentProfile existing = new StudentProfile();
        existing.setStudentId(1L);
        existing.setWeakPoints("{\"weak_topics\":[\"概率论\"]}");
        existing.setInterestArea("{\"areas\":[\"NLP\"]}");
        existing.setKnowledgeBase("{\"math_level\":\"中等\"}");
        when(profileMapper.selectOne(any())).thenReturn(existing);

        when(orchestrator.dispatch(any(), any())).thenReturn(AgentMessage.builder()
                .payload("{\"weak_points\":{\"weak_topics\":[\"矩阵运算\"]},\"interest_area\":{\"areas\":[\"推荐系统\"]}}")
                .build());

        service.upsertFromAgent(1L, "");

        ArgumentCaptor<StudentProfile> captor = ArgumentCaptor.forClass(StudentProfile.class);
        verify(profileMapper).updateById(captor.capture());
        StudentProfile saved = captor.getValue();

        var wp = objectMapper.readTree(saved.getWeakPoints()).get("weak_topics");
        assertNotNull(wp);
        assertEquals(2, wp.size());
        assertTrue(wp.toString().contains("概率论"));
        assertTrue(wp.toString().contains("矩阵运算"));

        var ia = objectMapper.readTree(saved.getInterestArea()).get("areas");
        assertEquals(2, ia.size());
        assertTrue(ia.toString().contains("NLP"));
        assertTrue(ia.toString().contains("推荐系统"));

        assertTrue(saved.getKnowledgeBase().contains("中等"));
    }

    @Test
    void upsertFromAgent_existing_mergesObjectFieldsByOverride() throws Exception {
        StudentProfile existing = new StudentProfile();
        existing.setStudentId(1L);
        existing.setKnowledgeBase("{\"math_level\":\"中等\",\"programming_level\":\"Python基础\"}");
        when(profileMapper.selectOne(any())).thenReturn(existing);

        when(orchestrator.dispatch(any(), any())).thenReturn(AgentMessage.builder()
                .payload("{\"knowledge_base\":{\"math_level\":\"较强\"}}")
                .build());

        service.upsertFromAgent(1L, "");

        ArgumentCaptor<StudentProfile> captor = ArgumentCaptor.forClass(StudentProfile.class);
        verify(profileMapper).updateById(captor.capture());
        StudentProfile saved = captor.getValue();

        var kb = objectMapper.readTree(saved.getKnowledgeBase());
        assertEquals("较强", kb.get("math_level").asText());
        assertEquals("Python基础", kb.get("programming_level").asText());
    }

    @Test
    void upsertFromAgent_invalidJsonPayload_doesNotThrow() {
        when(orchestrator.dispatch(any(), any())).thenReturn(AgentMessage.builder()
                .payload("not a json at all")
                .build());
        when(profileMapper.selectOne(any())).thenReturn(null);

        service.upsertFromAgent(1L, "hi");

        ArgumentCaptor<StudentProfile> captor = ArgumentCaptor.forClass(StudentProfile.class);
        verify(profileMapper).insert(captor.capture());
        assertEquals("{}", captor.getValue().getKnowledgeBase());
    }

    // ---------------- 缓存相关测试 ----------------

    @Test
    void getByStudentId_cacheHit_skipsMapper() throws Exception {
        StudentProfile cached = new StudentProfile();
        cached.setStudentId(1L);
        cached.setKnowledgeBase("{\"math_level\":\"中等\"}");
        when(valueOps.get("learngen:profile:byStudent:1")).thenReturn(objectMapper.writeValueAsString(cached));

        StudentProfile result = service.getByStudentId(1L);

        assertEquals(1L, result.getStudentId());
        assertTrue(result.getKnowledgeBase().contains("中等"));
        verify(profileMapper, never()).selectOne(any());
    }

    @Test
    void getByStudentId_cacheMiss_queriesMapperAndPopulatesCache() {
        when(valueOps.get("learngen:profile:byStudent:1")).thenReturn(null);
        StudentProfile dbProfile = new StudentProfile();
        dbProfile.setId(99L);
        dbProfile.setStudentId(1L);
        dbProfile.setKnowledgeBase("{\"math_level\":\"较强\"}");
        when(profileMapper.selectOne(any())).thenReturn(dbProfile);

        StudentProfile result = service.getByStudentId(1L);

        assertEquals(99L, result.getId());
        verify(profileMapper).selectOne(any());
        verify(valueOps).set(eq("learngen:profile:byStudent:1"), anyString(), any());
    }

    @Test
    void upsertFromAgent_invalidatesProfileCache() {
        when(profileMapper.selectOne(any())).thenReturn(null);
        when(orchestrator.dispatch(any(), any())).thenReturn(AgentMessage.builder()
                .payload("{}")
                .build());

        service.upsertFromAgent(1L, "对话");

        verify(redis).delete("learngen:profile:byStudent:1");
    }
}