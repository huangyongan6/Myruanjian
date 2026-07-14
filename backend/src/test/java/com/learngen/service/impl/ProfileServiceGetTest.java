package com.learngen.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.learngen.agent.Orchestrator;
import com.learngen.cache.RedisCacheSupport;
import com.learngen.exception.BusinessException;
import com.learngen.mapper.StudentProfileMapper;
import com.learngen.model.StudentProfile;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ProfileService 查询路径测试。
 */
class ProfileServiceGetTest {

    @SuppressWarnings("unchecked")
    private static ProfileServiceImpl newService(StudentProfileMapper mapper) {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        ObjectMapper om = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new ProfileServiceImpl(mapper, mock(Orchestrator.class), om, new RedisCacheSupport(redis, om));
    }

    @Test
    void getByStudentId_notFound_throwsBusinessException() {
        StudentProfileMapper mapper = mock(StudentProfileMapper.class);
        when(mapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(null);

        ProfileServiceImpl service = newService(mapper);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getByStudentId(999L));
        assertEquals(404, ex.getCode());
    }

    @Test
    void getByStudentId_found_returnsProfile() {
        StudentProfileMapper mapper = mock(StudentProfileMapper.class);
        StudentProfile profile = new StudentProfile();
        profile.setStudentId(1L);
        profile.setKnowledgeBase("{\"math_level\":\"中等\"}");
        when(mapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(profile);

        ProfileServiceImpl service = newService(mapper);
        StudentProfile found = service.getByStudentId(1L);
        assertEquals(1L, found.getStudentId());
    }
}