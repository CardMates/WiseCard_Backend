package com.example.demo.expense.entity;

import com.example.demo.auth.entity.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "expenses")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Expense {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private Long userId;
    
    @Column(nullable = false)
    private String place; // 장소명
    
    @Column(nullable = false)
    private Long amount; // 결제 금액
    
    @Column(nullable = false)
    private LocalDateTime postedAt; // 결제 시간
    
    @Column
    private String originalText; // 원본 푸시 알림 텍스트
}
