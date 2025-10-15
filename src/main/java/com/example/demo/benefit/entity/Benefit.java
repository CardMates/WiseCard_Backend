package com.example.demo.benefit.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.example.demo.card.entity.Card;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@NoArgsConstructor
@Getter
public class Benefit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id")
    private Card card;

    @ElementCollection
    @CollectionTable(name = "benefit_applicable_categories", joinColumns = @JoinColumn(name = "benefit_id"))
    @Column(name = "applicable_category")
    private List<String> applicableCategory = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "benefit_applicable_targets", joinColumns = @JoinColumn(name = "benefit_id"))
    @Column(name = "applicable_targets")
    private List<String> applicableTargets = new ArrayList<>();

    @OneToMany(mappedBy = "benefit", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DiscountBenefit> discountBenefits = new ArrayList<>();

    @OneToMany(mappedBy = "benefit", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PointBenefit> pointBenefits = new ArrayList<>();

    @OneToMany(mappedBy = "benefit", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CashbackBenefit> cashbackBenefits = new ArrayList<>();

    private String summary;

    @Builder
    public Benefit(List<String> applicableCategory, List<String> applicableTargets, String summary, Card card) {
        this.applicableCategory = applicableCategory;
        this.applicableTargets = applicableTargets;
        this.summary = summary;
        this.card = card;
    }

}

