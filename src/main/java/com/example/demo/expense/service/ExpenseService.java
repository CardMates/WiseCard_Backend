package com.example.demo.expense.service;

import com.example.demo.auth.util.AuthUtils;
import com.example.demo.expense.dto.PushNotificationRequest;
import com.example.demo.expense.entity.Expense;
import com.example.demo.expense.repository.ExpenseRepository;
import com.example.demo.user.service.AutomaticBenefitCalculationService;
import com.example.demo.lock.RedisLockUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseService {
    
    private final ExpenseRepository expenseRepository;
    private final ExpenseParsingService parsingService;
    private final AutomaticBenefitCalculationService automaticBenefitCalculationService;
    private final RedisLockUtil redisLockUtil;
    
    /**
     * 푸시 알림 데이터를 파싱하여 소비내역 저장
     * 
     * 분산락 적용 이유:
     * 1. 동일한 푸시 알림 중복 처리 방지
     * 2. 실적 중복 누적 방지
     * 3. 혜택 중복 적용 방지
     */
    @Transactional
    public Expense saveExpenseFromPushNotification(PushNotificationRequest request) {
        String lockKey = String.format("expense:%s:%d", request.text(), request.postedAt());
        
        return redisLockUtil.acquireAndRunLock(
            lockKey,
            () -> processExpenseFromPushNotification(request),
            3, // 최대 재시도 3회
            100, // 재시도 간격 100ms
            30 // TTL 30초
        );
    }
    
    /**
     * 실제 소비내역 처리 로직 (분산락 내부에서 실행)
     */
    private Expense processExpenseFromPushNotification(PushNotificationRequest request) {
        // 텍스트 파싱
        ExpenseParsingService.ParsedExpenseData parsedData = 
            parsingService.parseExpenseText(request.text());
        
        if (parsedData == null) {
            throw new IllegalArgumentException("푸시 알림 파싱 실패");
        }
        
        // 소비내역 엔티티 생성
        Expense expense = Expense.builder()
                .userId(AuthUtils.getMemberId()) // 고정 사용자 ID
                .place(parsedData.getPlace())
                .amount(parsedData.getAmount())
                .originalText(parsedData.getOriginalText())
                .postedAt(LocalDateTime.now())
                .build();
        
        // DB 저장
        Expense savedExpense = expenseRepository.save(expense);
        log.info("소비내역 저장 완료: {} - {}원", parsedData.getPlace(), parsedData.getAmount());
        
        // 자동 혜택 계산 및 적용
        try {
            automaticBenefitCalculationService.processExpenseAndCalculateBenefits(savedExpense);
            log.info("자동 혜택 계산 완료: {} - {}원", parsedData.getPlace(), parsedData.getAmount());
        } catch (Exception e) {
            log.error("자동 혜택 계산 실패: {} - {}원", parsedData.getPlace(), parsedData.getAmount(), e);
            // 혜택 계산 실패해도 소비내역은 저장됨
        }
        
        return savedExpense;
    }
    
}
