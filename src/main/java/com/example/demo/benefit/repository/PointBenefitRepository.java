package com.example.demo.benefit.repository;

import com.example.demo.benefit.entity.DiscountBenefit;
import com.example.demo.benefit.entity.PointBenefit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PointBenefitRepository extends JpaRepository<PointBenefit, Long> {
}