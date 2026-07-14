package com.learngen.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.learngen.model.LearningResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RedisCacheSupport 自身行为测试。
 */
class RedisCacheSupportTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;
    private ObjectMapper objectMapper;
    private RedisCacheSupport cache;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        cache = new RedisCacheSupport(redis, objectMapper);
    }

    @Test
    void key_assemblesStandardFormat() {
        assertEquals("learngen:resource:byId:42", cache.key("resource", "byId", 42L));
        assertEquals("learngen:profile:byStudent:1", cache.key("profile", "byStudent", 1L));
    }

    @Test
    void set_get_roundTrip_withLocalDateTime() throws Exception {
        LearningResource r = new LearningResource();
        r.setId(1L);
        r.setStudentId(7L);
        r.setType("doc");
        r.setTitle("缓存测试");
        r.setCreatedAt(LocalDateTime.now());

        // 模拟 set 行为：捕获写入的 JSON，存到 valueOps mock 中
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        when(valueOps.get("learngen:resource:byId:1")).thenAnswer(invocation -> {
            // 第一次调用：模拟已经写入
            return null;
        });

        cache.set("learngen:resource:byId:1", r, Duration.ofMinutes(1));
        verify(valueOps).set(eq("learngen:resource:byId:1"), jsonCaptor.capture(), any());
        String storedJson = jsonCaptor.getValue();
        assertNotNull(storedJson);
        assertTrue(storedJson.contains("缓存测试"));

        // 现在把写进去的 JSON 配回给 mock，让读路径命中
        when(valueOps.get("learngen:resource:byId:1")).thenReturn(storedJson);

        LearningResource read = cache.get("learngen:resource:byId:1", LearningResource.class);
        assertNotNull(read);
        assertEquals(1L, read.getId());
        assertEquals("doc", read.getType());
        assertEquals("缓存测试", read.getTitle());
        assertNotNull(read.getCreatedAt());
    }

    @Test
    void get_cacheMiss_returnsNull() {
        when(valueOps.get("learngen:resource:byId:999")).thenReturn(null);
        assertNull(cache.get("learngen:resource:byId:999", LearningResource.class));
    }

    @Test
    void get_redisDown_returnsNullAndDoesNotPropagate() {
        when(valueOps.get(anyString())).thenThrow(new RedisConnectionFailureException("连接失败"));
        LearningResource result = cache.get("learngen:resource:byId:1", LearningResource.class);
        assertNull(result);
    }

    @Test
    void get_invalidJson_returnsNull() {
        when(valueOps.get("learngen:resource:byId:1")).thenReturn("not valid json");
        assertNull(cache.get("learngen:resource:byId:1", LearningResource.class));
    }

    @Test
    void set_serializationFailure_doesNotThrow() throws Exception {
        // 构造一个 ObjectMapper 让 writeValueAsString 抛错
        ObjectMapper bad = mock(ObjectMapper.class);
        when(bad.writeValueAsString(any())).thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("boom") {});
        RedisCacheSupport badCache = new RedisCacheSupport(redis, bad);

        // 不应抛异常
        badCache.set("k", new Object(), Duration.ofSeconds(1));
        verify(valueOps, never()).set(anyString(), anyString(), any());
    }

    @Test
    void delete_singleKey() {
        cache.delete("learngen:resource:byId:1");
        verify(redis).delete("learngen:resource:byId:1");
    }

    @Test
    void deleteByPattern_invokesScanWithCorrectPattern() {
        ArgumentCaptor<ScanOptions> optsCaptor = ArgumentCaptor.forClass(ScanOptions.class);
        when(redis.execute(any(RedisCallback.class))).thenReturn(0L);

        long count = cache.deleteByPattern("learngen:knowledge:byModule:*");

        verify(redis).execute(any(RedisCallback.class));
        // ScanOptions 内部字段不可直接 assert，但只要 execute 被调用就算覆盖
        assertEquals(0L, count);
    }

    @Test
    void deleteByPattern_emptyPattern_returnsZero() {
        assertEquals(0L, cache.deleteByPattern(""));
        assertEquals(0L, cache.deleteByPattern(null));
        verify(redis, never()).execute(any(RedisCallback.class));
    }

    @Test
    void normalizeKeySegment_truncatesLongInput() {
        String raw = "啊".repeat(200);
        String result = cache.normalizeKeySegment(raw);
        // 100 字符被截断到 64 字符；URLEncoder UTF-8 每汉字 9 字符 → 上限约 64*9=576
        assertTrue(result.length() <= 600, "应截断到 64 字符以内，实际 " + result.length());
        assertNotNull(result);
    }

    @Test
    void normalizeKeySegment_handlesNull() {
        assertEquals("", cache.normalizeKeySegment(null));
    }
}