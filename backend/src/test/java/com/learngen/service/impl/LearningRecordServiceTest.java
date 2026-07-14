package com.learngen.service.impl;

import com.learngen.exception.BusinessException;
import com.learngen.mapper.LearningRecordMapper;
import com.learngen.model.LearningRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * LearningRecordService 评估逻辑测试。
 */
class LearningRecordServiceTest {

    private LearningRecordMapper mapper;
    private SimpMessagingTemplate messagingTemplate;
    private LearningRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(LearningRecordMapper.class);
        // 广播事件使用 mock 即可：测试不关注推送内容，只关注持久化行为。
        // 推送失败由 publishRecordEvent 内部 try/catch 兜底，mock 抛异常也会被吞掉。
        messagingTemplate = mock(SimpMessagingTemplate.class);
        service = new LearningRecordServiceImpl(mapper, messagingTemplate);
    }

    @Test
    void record_validAction_persists() {
        service.record(1L, 10L, "view", null, 60);
        ArgumentCaptor<LearningRecord> captor = ArgumentCaptor.forClass(LearningRecord.class);
        verify(mapper).insert(captor.capture());
        LearningRecord saved = captor.getValue();
        assertEquals(1L, saved.getStudentId());
        assertEquals(10L, saved.getResourceId());
        assertEquals("view", saved.getAction());
        assertEquals(60, saved.getDuration());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void record_invalidAction_throwsBusinessException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.record(1L, 10L, "delete", null, null));
        assertEquals(400, ex.getCode());
    }

    @Test
    void evaluate_emptyHistory_returnsZeros() {
        when(mapper.selectList(any())).thenReturn(List.of());
        Map<String, Object> report = service.evaluate(1L);
        assertEquals(0, report.get("viewCount"));
        assertEquals(0, report.get("completeCount"));
        assertEquals(0, report.get("quizCount"));
        assertEquals(0, report.get("totalDurationSeconds"));
        assertNull(report.get("averageScore"));
    }

    @Test
    void evaluate_aggregatesCorrectly() {
        when(mapper.selectList(any())).thenReturn(List.of(
                rec(1L, "view", null, 30),
                rec(1L, "view", null, 30),
                rec(1L, "complete", null, 120),
                rec(1L, "quiz", 80, 300),
                rec(1L, "quiz", 60, 240)
        ));
        Map<String, Object> report = service.evaluate(1L);
        assertEquals(2, report.get("viewCount"));
        assertEquals(1, report.get("completeCount"));
        assertEquals(2, report.get("quizCount"));
        assertEquals(720L, ((Number) report.get("totalDurationSeconds")).longValue());
        assertEquals(70.0, (Double) report.get("averageScore"), 0.01);
        assertEquals(1.0 / 3.0, (Double) report.get("completionRate"), 0.01);
    }

    private static LearningRecord rec(Long id, String action, Integer score, Integer duration) {
        LearningRecord r = new LearningRecord();
        r.setId(id);
        r.setStudentId(1L);
        r.setAction(action);
        r.setScore(score);
        r.setDuration(duration);
        return r;
    }
}