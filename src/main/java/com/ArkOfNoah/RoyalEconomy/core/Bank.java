package com.ArkOfNoah.RoyalEconomy.core;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Bank {

    private final UUID id;
    private String name;
    private final UUID owner;
    private final Set<UUID> members;
    private double balance;
    private int maxMembers;

    public Bank(UUID id, String name, UUID owner, double balance, int maxMembers, Set<UUID> members) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.balance = balance;
        this.maxMembers = maxMembers;
        this.members = members != null ? members : new HashSet<>();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public UUID getOwner() {
        return owner;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public int getMaxMembers() {
        return maxMembers;
    }

    public void setMaxMembers(int maxMembers) {
        this.maxMembers = maxMembers;
    }

    public boolean isMember(UUID uuid) {
        return owner.equals(uuid) || members.contains(uuid);
    }

    public boolean addMember(UUID uuid) {
        if (members.size() >= maxMembers) return false;
        return members.add(uuid);
    }

    public boolean removeMember(UUID uuid) {
        return members.remove(uuid);
    }
}
