package com.ArkOfNoah.RoyalEconomy.api;

import java.util.UUID;

public interface Economy {

    double getBalance(UUID uuid);

    void setBalance(UUID uuid, double amount);

    boolean deposit(UUID uuid, double amount);

    /**
     * @return true if withdrawal successful, false if not enough funds or invalid amount.
     */
    boolean withdraw(UUID uuid, double amount);

    boolean has(UUID uuid, double amount);

    /**
     * Transfer amount from one player to another.
     * @return true if both withdraw & deposit succeeded.
     */
    boolean transfer(UUID from, UUID to, double amount);

    /**
     * Simple currency formatting for display.
     */
    String format(double amount);
}
