package com.learngen.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;

/**
 * Redis 缓存工具（手动封装，对应 26 软件杯 MVP 缓存层）。
 *
 * <p>关键约定：
 * <ul>
 *   <li>键命名空间：{@code learngen:{module}:{subtype}:{id}}，通过 {@link #key(String, String, Object)} 统一拼装</li>
 *   <li>值序列化：JSON，复用全局 {@link ObjectMapper}（{@code application.yml} 已配置 {@code LocalDateTime} 格式与 Asia/Shanghai 时区）</li>
 *   <li>容错契约：所有 Redis 异常降级为日志 + null / no-op，调用方必须自行回源 DB，绝不向上抛</li>
 *   <li>批量删除：使用 {@code SCAN} 命令，禁止用 {@code KEYS}（{@code KEYS} 会阻塞 Redis）</li>
 * </ul>
 *
 * <p>TTL 常量：见 {@link #TTL_RESOURCE_BY_ID} 等静态字段。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisCacheSupport {

    /** 顶层 namespace，与 {@code spring.application.name=learngen-backend} 对齐。 */
    public static final String NAMESPACE = "learngen";

    /** 单条资源（含 Markdown 正文）缓存时长。 */
    public static final Duration TTL_RESOURCE_BY_ID = Duration.ofMinutes(30);
    /** 资源列表缓存时长（失效兜底用）。 */
    public static final Duration TTL_RESOURCE_LIST = Duration.ofMinutes(5);
    /** 学生画像缓存时长。 */
    public static final Duration TTL_PROFILE_BY_ID = Duration.ofMinutes(15);
    /** 知识点详情缓存时长（变更稀少，给长一点）。 */
    public static final Duration TTL_KNOWLEDGE_BY_ID = Duration.ofHours(2);
    /** 知识点列表缓存时长。 */
    public static final Duration TTL_KNOWLEDGE_LIST = Duration.ofHours(1);
    /** 知识点关键字搜索缓存时长。 */
    public static final Duration TTL_KNOWLEDGE_SEARCH = Duration.ofMinutes(15);

    /** {@link #searchByName(String)} 类缓存键里关键字最大长度，超出截断避免 key 过长。 */
    private static final int MAX_KEYWORD_LEN = 64;

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    /**
     * 组装缓存键。例：{@code key("resource","byId",42)} → {@code learngen:resource:byId:42}。
     */
    public String key(String module, String subtype, Object id) {
        return NAMESPACE + ":" + module + ":" + subtype + ":" + id;
    }

    /**
     * 读取缓存值。未命中或任何异常时返回 {@code null}，调用方回源 DB。
     */
    public <T> T get(String key, Class<T> clazz) {
        if (key == null) return null;
        try {
            String json = redis.opsForValue().get(key);
            if (json == null) return null;
            return objectMapper.readValue(json, clazz);
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis 连接失败，降级到 DB: key={} cause={}", key, e.getMessage());
            return null;
        } catch (JsonProcessingException e) {
            log.warn("Redis 反序列化失败，降级到 DB: key={} cause={}", key, e.getMessage());
            return null;
        } catch (RuntimeException e) {
            log.warn("Redis get 异常，降级到 DB: key={} cause={}", key, e.getMessage());
            return null;
        }
    }

    /**
     * 读取缓存值（带泛型列表类型，避免 {@code List<T>} 被反序列化成 {@code List<LinkedHashMap>}）。
     */
    public <T> T get(String key, TypeReference<T> typeRef) {
        if (key == null || typeRef == null) return null;
        try {
            String json = redis.opsForValue().get(key);
            if (json == null) return null;
            return objectMapper.readValue(json, typeRef);
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis 连接失败，降级到 DB: key={} cause={}", key, e.getMessage());
            return null;
        } catch (JsonProcessingException e) {
            log.warn("Redis 反序列化失败，降级到 DB: key={} cause={}", key, e.getMessage());
            return null;
        } catch (RuntimeException e) {
            log.warn("Redis get 异常，降级到 DB: key={} cause={}", key, e.getMessage());
            return null;
        }
    }

    /**
     * 读取缓存值（基于 key 组装）。
     */
    public <T> T get(String module, String subtype, Object id, Class<T> clazz) {
        return get(key(module, subtype, id), clazz);
    }

    /**
     * 写入缓存值。失败仅日志，不抛。
     */
    public void set(String key, Object value, Duration ttl) {
        if (key == null || value == null || ttl == null) return;
        try {
            String json = objectMapper.writeValueAsString(value);
            redis.opsForValue().set(key, json, ttl);
        } catch (JsonProcessingException e) {
            log.warn("Redis 序列化失败，跳过写入: key={} cause={}", key, e.getMessage());
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis 连接失败，跳过写入: key={} cause={}", key, e.getMessage());
        } catch (RuntimeException e) {
            log.warn("Redis set 异常，跳过写入: key={} cause={}", key, e.getMessage());
        }
    }

    /**
     * 写入缓存值（基于 key 组装）。
     */
    public void set(String module, String subtype, Object id, Object value, Duration ttl) {
        set(key(module, subtype, id), value, ttl);
    }

    /**
     * 删除单条缓存。失败仅日志。
     */
    public void delete(String key) {
        if (key == null) return;
        try {
            redis.delete(key);
        } catch (RuntimeException e) {
            log.warn("Redis delete 异常，跳过: key={} cause={}", key, e.getMessage());
        }
    }

    /**
     * 批量删除缓存（pipelined DEL）。失败仅日志。
     */
    public void delete(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) return;
        try {
            redis.delete(keys);
        } catch (RuntimeException e) {
            log.warn("Redis 批量 delete 异常，跳过: size={} cause={}", keys.size(), e.getMessage());
        }
    }

    /**
     * 按 pattern 批量删除（{@code SCAN}，非 {@code KEYS}）。返回实际删除的 key 数。
     * <p>调用方自行决定是否需要该开销；本方法失败时返回 {@code 0}。
     */
    public long deleteByPattern(String pattern) {
        if (pattern == null || pattern.isBlank()) return 0L;
        try {
            ScanOptions options = ScanOptions.scanOptions().match(pattern).count(256).build();
            Long removed = redis.execute((RedisCallback<Long>) connection -> {
                long count = 0L;
                try (Cursor<byte[]> cursor = connection.scan(options)) {
                    while (cursor.hasNext()) {
                        byte[] keyBytes = cursor.next();
                        Long deleted = connection.del(keyBytes);
                        if (deleted != null && deleted > 0L) count++;
                    }
                }
                return count;
            });
            return removed == null ? 0L : removed;
        } catch (RuntimeException e) {
            log.warn("Redis deleteByPattern 异常，跳过: pattern={} cause={}", pattern, e.getMessage());
            return 0L;
        }
    }

    /**
     * 把任意字符串关键字规整为缓存键片段：截断到 {@link #MAX_KEYWORD_LEN}，
     * 并对非 ASCII 做 URL 编码，避免 Redis 里出现奇怪字节。
     * <p>仅用于 {@link com.learngen.service.KnowledgeBaseService#searchByName(String)} 等
     * 把用户输入直接当作 key 片段的场景。
     */
    public String normalizeKeySegment(String raw) {
        if (raw == null) return "";
        String trimmed = raw.trim();
        if (trimmed.length() > MAX_KEYWORD_LEN) {
            trimmed = trimmed.substring(0, MAX_KEYWORD_LEN);
        }
        // 用 java.net.URLEncoder 处理非 ASCII 与特殊字符，charset 显式 UTF-8
        return java.net.URLEncoder.encode(trimmed, StandardCharsets.UTF_8);
    }
}
