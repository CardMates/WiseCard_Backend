package com.example.demo.benefit.entity;

import com.example.demo.benefit.application.dto.ChannelType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@Getter
public class CashbackBenefit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private double rate;
    private double amount;
    private long minimumAmount;
    private Long benefitLimit;
    
    @Enumerated(EnumType.STRING)
    private ChannelType channel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "benefit_id")
    private Benefit benefit;

    private Integer minimumSpending;

    @Builder
    public CashbackBenefit(double rate, double amount, long minimumAmount, Long benefitLimit, ChannelType channel, Benefit benefit, Integer minimumSpending) {
        this.rate = rate;
        this.amount = amount;
        this.minimumAmount = minimumAmount;
        this.benefitLimit = benefitLimit;
        this.channel = channel;
        this.benefit = benefit;
        this.minimumSpending = minimumSpending;
    }

}
