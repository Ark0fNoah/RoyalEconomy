package com.ArkOfNoah.RoyalEconomy.utils;

public enum EcoRank {
    PLAYER("royaleconomy.PLAYER"),
    ADMIN("royaleconomy.admin");

    private final String permission;

    EcoRank(String permission) {
        this.permission = permission;
    }

    public String getPermission() {
        return permission;
    }
}