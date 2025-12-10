package com.ArkOfNoah.RoyalEconomy.core;

import com.ArkOfNoah.RoyalEconomy.RoyalEconomy;
import com.ArkOfNoah.RoyalEconomy.api.Economy;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class BankManager {

    private final RoyalEconomy plugin;
    private final Economy economy;
    private final BankStorage storage;
    private final Map<String, Bank> banksByName = new HashMap<>();

    private final int maxBanksPerPlayer;
    private final int defaultMaxMembers;
    private final double startingBankBalance;
    private final boolean allowNegative;

    public BankManager(RoyalEconomy plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
        this.storage = new BankStorage(plugin);

        FileConfiguration cfg = plugin.getConfig();
        this.maxBanksPerPlayer = cfg.getInt("banks.max-banks-per-player", 3);
        this.defaultMaxMembers = cfg.getInt("banks.default-max-members", 10);
        this.startingBankBalance = cfg.getDouble("banks.starting-bank-balance", 0.0);
        this.allowNegative = cfg.getBoolean("banks.allow-negative", false);
    }

    public void load() {
        storage.load(banksByName);
    }

    public void save() {
        storage.save(banksByName);
    }

    public Collection<Bank> getAllBanks() {
        return banksByName.values();
    }

    public Bank getBank(String name) {
        if (name == null) return null;
        return banksByName.get(name.toLowerCase());
    }

    public List<Bank> getBanksForPlayer(UUID uuid) {
        List<Bank> result = new ArrayList<>();
        for (Bank bank : banksByName.values()) {
            if (bank.isMember(uuid)) {
                result.add(bank);
            }
        }
        return result;
    }

    public long getBankCountForOwner(UUID owner) {
        return banksByName.values().stream()
                .filter(b -> b.getOwner().equals(owner))
                .count();
    }

    public Bank createBank(UUID owner, String name) {
        if (getBank(name) != null) return null; // name taken
        if (getBankCountForOwner(owner) >= maxBanksPerPlayer) return null;

        UUID id = UUID.randomUUID();
        Bank bank = new Bank(id, name, owner, startingBankBalance, defaultMaxMembers, new HashSet<>());
        banksByName.put(name.toLowerCase(), bank);
        return bank;
    }

    public boolean deleteBank(String name) {
        Bank removed = banksByName.remove(name.toLowerCase());
        return removed != null;
    }

    public boolean depositToBank(Bank bank, UUID player, double amount) {
        if (amount <= 0) return false;

        double playerBal = economy.getBalance(player);
        if (playerBal < amount) return false;

        if (!economy.withdraw(player, amount)) return false;
        bank.setBalance(bank.getBalance() + amount);
        return true;
    }

    public boolean withdrawFromBank(Bank bank, UUID player, double amount) {
        if (amount <= 0) return false;
        double newBalance = bank.getBalance() - amount;
        if (!allowNegative && newBalance < 0) return false;

        bank.setBalance(newBalance);
        if (!economy.deposit(player, amount)) {
            // rollback bank if player deposit fails for some reason
            bank.setBalance(bank.getBalance() + amount);
            return false;
        }
        return true;
    }
}
