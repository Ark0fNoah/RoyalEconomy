package com.ArkOfNoah.RoyalEconomy.core;

import com.ArkOfNoah.RoyalEconomy.RoyalEconomy;
import com.ArkOfNoah.RoyalEconomy.api.Economy;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Handles:
 * - Bank model (inner class Bank)
 * - Bank storage (banks.yml)
 * - Bank operations (create, delete, deposit, withdraw, membership)
 */
public class BankManager {

    // ─────────────────────────────────────────
    //  Inner Bank class (was Bank.java)
    // ─────────────────────────────────────────
    public static class Bank {

        private final UUID id;
        private final String name;
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

    // ─────────────────────────────────────────
    //  BankManager implementation
    // ─────────────────────────────────────────

    private final RoyalEconomy plugin;
    private final Economy economy;
    private final Map<String, Bank> banksByName = new HashMap<>();

    private final int maxBanksPerPlayer;
    private final int defaultMaxMembers;
    private final double startingBankBalance;
    private final boolean allowNegative;

    private final File file;
    private FileConfiguration config;

    public BankManager(RoyalEconomy plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;

        FileConfiguration cfg = plugin.getConfig();
        this.maxBanksPerPlayer = cfg.getInt("banks.max-banks-per-player", 3);
        this.defaultMaxMembers = cfg.getInt("banks.default-max-members", 10);
        this.startingBankBalance = cfg.getDouble("banks.starting-bank-balance", 0.0);
        this.allowNegative = cfg.getBoolean("banks.allow-negative", false);

        this.file = new File(plugin.getDataFolder(), "banks.yml");
    }

    // ─────────── Storage (was BankStorage) ───────────

    public void load() {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create banks.yml");
                e.printStackTrace();
            }
        }

        config = YamlConfiguration.loadConfiguration(file);
        banksByName.clear();

        ConfigurationSection banksSec = config.getConfigurationSection("banks");
        if (banksSec == null) return;

        for (String idStr : banksSec.getKeys(false)) {
            try {
                UUID id = UUID.fromString(idStr);
                String path = "banks." + idStr + ".";
                String name = config.getString(path + "name");
                String ownerStr = config.getString(path + "owner");
                if (name == null || ownerStr == null) continue;

                UUID owner = UUID.fromString(ownerStr);
                double balance = config.getDouble(path + "balance");
                int maxMembers = config.getInt(path + "maxMembers");

                List<String> memberStr = config.getStringList(path + "members");
                Set<UUID> members = new HashSet<>();
                for (String s : memberStr) {
                    try {
                        members.add(UUID.fromString(s));
                    } catch (IllegalArgumentException ignored) {}
                }

                Bank bank = new Bank(id, name, owner, balance, maxMembers, members);
                banksByName.put(name.toLowerCase(), bank);

            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in banks.yml: " + idStr);
            }
        }
    }

    public void save() {
        if (config == null) {
            config = new YamlConfiguration();
        }
        config.set("banks", null);

        for (Bank bank : banksByName.values()) {
            String idStr = bank.getId().toString();
            String path = "banks." + idStr + ".";
            config.set(path + "name", bank.getName());
            config.set(path + "owner", bank.getOwner().toString());
            config.set(path + "balance", bank.getBalance());
            config.set(path + "maxMembers", bank.getMaxMembers());

            List<String> members = new ArrayList<>();
            for (UUID u : bank.getMembers()) {
                members.add(u.toString());
            }
            config.set(path + "members", members);
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save banks.yml");
            e.printStackTrace();
        }
    }

    // ─────────── Public API ───────────

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
        if (getBank(name) != null) return null;
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
            bank.setBalance(bank.getBalance() + amount);
            return false;
        }
        return true;
    }
}
