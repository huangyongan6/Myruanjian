package com.learngen.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.learngen.agent.AgentMessage;
import com.learngen.agent.Orchestrator;
import com.learngen.cache.RedisCacheSupport;
import com.learngen.exception.BusinessException;
import com.learngen.mapper.LearningResourceMapper;
import com.learngen.model.LearningResource;
import com.learngen.service.KnowledgeBaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ResourceService 缓存相关测试（CLAUDE.md §11.3）。
 */
class ResourceServiceImplTest {

    private Orchestrator orchestrator;
    private LearningResourceMapper resourceMapper;
    private KnowledgeBaseService knowledgeBaseService;
    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;
    private ObjectMapper objectMapper;
    private RedisCacheSupport cache;
    private ResourceServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        orchestrator = mock(Orchestrator.class);
        resourceMapper = mock(LearningResourceMapper.class);
        knowledgeBaseService = mock(KnowledgeBaseService.class);
        redis = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        cache = new RedisCacheSupport(redis, objectMapper);
        service = new ResourceServiceImpl(orchestrator, resourceMapper, knowledgeBaseService, cache);
    }

    @Test
    void getById_cacheHit_returnsCachedAndSkipsDb() throws Exception {
        LearningResource cached = new LearningResource();
        cached.setId(42L);
        cached.setStudentId(7L);
        cached.setType("doc");
        cached.setTitle("缓存里的标题");
        when(valueOps.get("learngen:resource:byId:42")).thenReturn(objectMapper.writeValueAsString(cached));

        LearningResource result = service.getById(42L);

        assertEquals(42L, result.getId());
        assertEquals("缓存里的标题", result.getTitle());
        verify(resourceMapper, never()).selectById(any());
        verify(valueOps, never()).set(anyString(), anyString(), any());
    }

    @Test
    void getById_cacheMiss_queriesDbAndPopulatesCache() {
        when(valueOps.get("learngen:resource:byId:42")).thenReturn(null);
        LearningResource db = new LearningResource();
        db.setId(42L);
        db.setStudentId(7L);
        db.setType("doc");
        db.setTitle("DB 里的标题");
        when(resourceMapper.selectById(42L)).thenReturn(db);

        LearningResource result = service.getById(42L);

        assertEquals("DB 里的标题", result.getTitle());
        verify(resourceMapper).selectById(42L);
        verify(valueOps).set(eq("learngen:resource:byId:42"), anyString(), any());
    }

    @Test
    void getById_dbMiss_throwsBusinessException_noCacheWrite() {
        when(valueOps.get("learngen:resource:byId:999")).thenReturn(null);
        when(resourceMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getById(999L));
        assertEquals(404, ex.getCode());
        verify(valueOps, never()).set(anyString(), anyString(), any());
    }

    @Test
    void generate_invalidatesStudentListCache() {
        when(knowledgeBaseService.loadMarkdownByName(any())).thenReturn(Optional.empty());
        when(orchestrator.dispatch(any(), any())).thenReturn(
                AgentMessage.builder().payload("# 生成的资源").build());
        // mock MyBatis-Plus insert 不回填 id，手动给一个 id
        org.mockito.Mockito.doAnswer(invocation -> {
            LearningResource r = invocation.getArgument(0);
            r.setId(100L);
            return 1;
        }).when(resourceMapper).insert(any(LearningResource.class));

        LearningResource result = service.generate(7L, "doc", "线性回归");

        assertNotNull(result.getId());
        assertEquals(100L, result.getId());
        verify(resourceMapper).insert(result);
        verify(redis).delete("learngen:resource:byStudent:7");
    }

    @Test
    void listByStudent_cacheMiss_queriesDbAndPopulatesCache() {
        when(valueOps.get("learngen:resource:byStudent:7")).thenReturn(null);
        LearningResource r1 = new LearningResource();
        r1.setId(1L);
        r1.setStudentId(7L);
        r1.setType("doc");
        LearningResource r2 = new LearningResource();
        r2.setId(2L);
        r2.setStudentId(7L);
        r2.setType("quiz");
        when(resourceMapper.selectList(any())).thenReturn(List.of(r1, r2));

        List<LearningResource> result = service.listByStudent(7L, null);

        assertEquals(2, result.size());
        verify(resourceMapper).selectList(any());
        verify(valueOps).set(eq("learngen:resource:byStudent:7"), anyString(), any());
    }

    @Test
    void listByStudent_cacheHit_filtersByTypeInMemory() throws Exception {
        LearningResource docR = new LearningResource();
        docR.setId(1L);
        docR.setStudentId(7L);
        docR.setType("doc");
        LearningResource quizR = new LearningResource();
        quizR.setId(2L);
        quizR.setStudentId(7L);
        quizR.setType("quiz");
        when(valueOps.get("learngen:resource:byStudent:7"))
                .thenReturn(objectMapper.writeValueAsString(List.of(docR, quizR)));

        List<LearningResource> docOnly = service.listByStudent(7L, "doc");
        assertEquals(1, docOnly.size());
        assertEquals("doc", docOnly.get(0).getType());
        verify(resourceMapper, never()).selectList(any());
    }
}