package org.example.sportplan.service;

import lombok.Getter;

@Getter
public enum RewardTier {

    DRINK("即时畅饮券", 5, 30),
    DINNER("欢乐聚餐券", 10, 120),
    ENTERTAINMENT("休闲娱乐券", 20, 200),
    LUXURY("豪华大餐券", 30, 300);

    private final String name;
    private final int pointsCost;
    private final int maxAmount;

    RewardTier(String name, int pointsCost, int maxAmount) {
        this.name = name;
        this.pointsCost = pointsCost;
        this.maxAmount = maxAmount;
    }
}
