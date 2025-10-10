package com.example.demo.controller;

import com.example.demo.expense.dto.PushNotificationRequest;
import com.example.demo.expense.entity.Expense;
import com.example.demo.expense.service.ExpenseService;
import com.example.demo.lock.RedisLockUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {
    
    private final ExpenseService expenseService;
    private final RedisLockUtil redisLockUtil;
    
    /**
     * 푸시 알림 데이터를 받아서 소비내역 저장
     * 
     * 컨트롤러 레벨에서 분산락 사용 예시:
     * - 동일한 푸시 알림 중복 처리 방지
     * - 여러 서버 인스턴스 간 동시 처리 방지
     */
    @PostMapping("/push-notification")
    public ResponseEntity<Expense> saveExpenseFromPushNotification(
            @RequestBody PushNotificationRequest request) {
        
        log.info("푸시 알림 데이터 수신: {}", request.text());
        
        try {
            // 컨트롤러에서 직접 분산락 사용
            String lockKey = String.format("expense:controller:%s:%d", request.text(), request.postedAt());
            
            Expense savedExpense = redisLockUtil.acquireAndRunLock(
                lockKey,
                () -> expenseService.saveExpenseFromPushNotification(request),
                3, // 최대 재시도 3회
                100, // 재시도 간격 100ms
                30 // TTL 30초
            );
            
            return ResponseEntity.ok(savedExpense);
            
        } catch (RedisLockUtil.DistributedLockException e) {
            log.warn("분산락 획득 실패: {}", e.getMessage());
            return ResponseEntity.status(429).build(); // Too Many Requests
        } catch (Exception e) {
            log.error("소비내역 저장 실패", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
}
