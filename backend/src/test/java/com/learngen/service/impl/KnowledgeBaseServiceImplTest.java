package com.learngen.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learngen.cache.RedisCacheSupport;
import com.learngen.exception.BusinessException;
import com.learngen.mapper.KnowledgePointMapper;
import com.learngen.model.KnowledgePoint;
import com.learngen.nlp.ChineseTokenizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * KnowledgeBaseService 缓存相关测试。
 */
class KnowledgeBaseServiceImplTest {

    private KnowledgePointMapper mapper;
    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;
    private ObjectMapper objectMapper;
    private RedisCacheSupport cache;
    private KnowledgeBaseServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        mapper = mock(KnowledgePointMapper.class);
        redis = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        objectMapper = new ObjectMapper();
        cache = new RedisCacheSupport(redis, objectMapper);
        ChineseTokenizer tokenizer = new ChineseTokenizer();
        tokenizer.load();
        service = new KnowledgeBaseServiceImpl(mapper, cache, tokenizer);
    }

    private KnowledgePoint point(Long id, String name, Integer module) {
        KnowledgePoint p = new KnowledgePoint();
        p.setId(id);
        p.setName(name);
        p.setModule(module);
        return p;
    }

    @Test
    void findById_cacheHit_skipsMapper() throws Exception {
        KnowledgePoint cached = point(3L, "线性回归", 1);
        when(valueOps.get("learngen:knowledge:byId:3")).thenReturn(objectMapper.writeValueAsString(cached));

        Optional<KnowledgePoint> result = service.findById(3L);

        assertTrue(result.isPresent());
        assertEquals("线性回归", result.get().getName());
        verify(mapper, never()).selectById(any());
    }

    @Test
    void findById_cacheMiss_queriesMapperAndPopulatesCache() {
        when(valueOps.get("learngen:knowledge:byId:3")).thenReturn(null);
        when(mapper.selectById(3L)).thenReturn(point(3L, "线性回归", 1));

        Optional<KnowledgePoint> result = service.findById(3L);

        assertTrue(result.isPresent());
        verify(valueOps).set(eq("learngen:knowledge:byId:3"), anyString(), any());
    }

    @Test
    void listByModule_cacheMiss_populatesCache() {
        when(valueOps.get("learngen:knowledge:byModule:2")).thenReturn(null);
        when(mapper.selectList(any())).thenReturn(List.of(point(1L, "A", 2), point(2L, "B", 2)));

        List<KnowledgePoint> result = service.listByModule(2);

        assertEquals(2, result.size());
        verify(valueOps).set(eq("learngen:knowledge:byModule:2"), anyString(), any());
    }

    @Test
    void searchByName_truncatesLongKeywords() {
        // 100 字符 keyword，应该被截断到 64 字符再 URL 编码
        String longKw = "啊".repeat(100);
        when(valueOps.get(anyString())).thenReturn(null);
        when(mapper.selectList(any())).thenReturn(List.of());

        service.searchByName(longKw);

        // 验证 key 里包含的 keyword 片段不超过 64 个字符（URL 编码后字节更多，所以用 char 数校验）
        org.mockito.ArgumentCaptor<String> keyCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(valueOps).get(keyCaptor.capture());
        String usedKey = keyCaptor.getValue();
        // 截断部分 = "learngen:knowledge:byName:" 前缀
        String prefix = "learngen:knowledge:byName:";
        assertTrue(usedKey.startsWith(prefix), "key 前缀应为 " + prefix + "，实际 " + usedKey);
        // 100 字符 keyword，截断后 64 字符 → URL 编码 UTF-8 每汉字 9 字符 = 576 + 前缀 26 = 602
        assertTrue(usedKey.length() <= 602, "key 不应过长，实际 " + usedKey.length());
    }

    @Test
    void update_invalidatesByIdAndModuleAndNameCaches() {
        KnowledgePoint existing = point(3L, "线性回归", 1);
        when(mapper.selectById(3L)).thenReturn(existing);

        KnowledgePoint patch = new KnowledgePoint();
        patch.setName("线性回归（修订）");
        KnowledgePoint updated = service.update(3L, patch);

        assertNotNull(updated);
        verify(mapper).updateById(existing);
        verify(redis).delete("learngen:knowledge:byId:3");
        // deleteByPattern 走 redis.execute(RedisCallback) 路径，
        // mock 时只要 redis.execute 被调过即可证明失效逻辑走到
        verify(redis, org.mockito.Mockito.atLeast(2)).execute(any(org.springframework.data.redis.core.RedisCallback.class));
    }

    @Test
    void delete_invalidatesByIdAndModuleAndNameCaches() {
        when(mapper.deleteById(3L)).thenReturn(1);

        service.delete(3L);

        verify(redis).delete("learngen:knowledge:byId:3");
        verify(redis, org.mockito.Mockito.atLeast(2)).execute(any(org.springframework.data.redis.core.RedisCallback.class));
    }

    @Test
    void delete_missing_throwsBusinessException() {
        when(mapper.deleteById(999L)).thenReturn(0);
        assertThrows(BusinessException.class, () -> service.delete(999L));
    }
}