package com.ArkOfNoah.RoyalEconomy;

import com.ArkOfNoah.RoyalEconomy.api.Economy;
import com.ArkOfNoah.RoyalEconomy.commands.*;
import com.ArkOfNoah.RoyalEconomy.core.*;
import com.ArkOfNoah.RoyalEconomy.listeners.PlayerJoinListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.util.logging.Level;

public class RoyalEconomy extends JavaPlugin {

    private static RoyalEconomy instance;

    // Managers
    private MessageManager messageManager; // NEW
    private EconomyManager economyManager;
    private StorageHandler storageHandler;
    private TransactionLogger transactionLogger;
    private LeaderboardManager leaderboardManager;
    private BankManager bankManager;
    private BoostManager boostManager;
    private TaxManager taxManager;

    // Tasks
    private InterestTask interestTask;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        initializeManagers();
        startInterestTask();

        registerService();
        setupVault2Bridge();
        setupPlaceholderAPI();

        registerCommands();
        registerListeners();

        getLogger().info("RoyalEconomy has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (storageHandler != null && economyManager != null) {
            storageHandler.save(economyManager.getAllAccounts());
        }

        if (bankManager != null && isBankEnabled()) bankManager.save();
        if (interestTask != null) interestTask.cancel();

        getLogger().info("RoyalEconomy disabled.");
    }

    private void initializeManagers() {
        // 1. Load Messages FIRST
        messageManager = new MessageManager(this);

        storageHandler = new StorageHandler(this);
        storageHandler.load();

        economyManager = new EconomyManager(this, storageHandler);
        transactionLogger = new TransactionLogger(this);
        boostManager = new BoostManager(this);
        taxManager = new TaxManager(this, economyManager);

        int cacheSeconds = getConfig().getInt("baltop.cache-seconds", 30);
        leaderboardManager = new LeaderboardManager(economyManager, cacheSeconds);

        bankManager = new BankManager(this, economyManager);
        if (isBankEnabled()) {
            bankManager.load();
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        messageManager.load(); // Reload messages

        transactionLogger = new TransactionLogger(this);

        int cacheSeconds = getConfig().getInt("baltop.cache-seconds", 30);
        leaderboardManager = new LeaderboardManager(economyManager, cacheSeconds);

        if (isBankEnabled()) bankManager.load();
        startInterestTask();
    }

    private void registerCommands() {
        RoyalEconomyTabCompleter tab = new RoyalEconomyTabCompleter(bankManager);

        registerCommand("royaleconomy", new RoyalEconomyCommand(this), tab);
        registerCommand("balance", new BalanceCommand(this), null);
        registerCommand("pay", new PayCommand(this), tab);
        registerCommand("eco", new EcoAdminCommand(this), tab);

        if (getConfig().getBoolean("baltop.enabled", true)) {
            registerCommand("baltop", new BaltopCommand(this), tab);
        }

        if (isBankEnabled()) {
            registerCommand("bank", new BankCommand(this), tab);
        }
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor, org.bukkit.command.TabCompleter tab) {
        if (getCommand(name) != null) {
            getCommand(name).setExecutor(executor);
            if (tab != null) getCommand(name).setTabCompleter(tab);
        }
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(economyManager, getConfig()), this);
    }

    private void startInterestTask() {
        if (interestTask != null) interestTask.cancel();
        if (getConfig().getBoolean("interest.enabled", false)) {
            int minutes = getConfig().getInt("interest.interval-minutes", 60);
            long ticks = Math.max(minutes * 60L * 20L, 1200L);
            interestTask = new InterestTask(this, economyManager, bankManager, boostManager);
            interestTask.runTaskTimer(this, ticks, ticks);
        }
    }

    private boolean isBankEnabled() {
        return getConfig().getBoolean("banks.enabled", false);
    }

    private void registerService() {
        Bukkit.getServicesManager().register(Economy.class, economyManager, this, ServicePriority.Normal);
    }

    private void setupPlaceholderAPI() {
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new com.ArkOfNoah.RoyalEconomy.placeholder.RoyalEconomyExpansion(this).register();
        }
    }

    private void setupVault2Bridge() {
        if (!getConfig().getBoolean("hooks.vault2.enabled", true)) return;
        if (getServer().getPluginManager().getPlugin("Vault") == null) return;

        try {
            Class<?> bridgeClass = Class.forName("com.ArkOfNoah.RoyalEconomy.vault.RoyalEconomyVaultBridge");
            // Standard Vault Package
            Class<?> vaultEconomyClass = Class.forName("net.milkbowl.vault.economy.Economy");

            Constructor<?> ctor = bridgeClass.getConstructor(RoyalEconomy.class, Economy.class);
            Object bridge = ctor.newInstance(this, economyManager);

            registerServiceUnsafe(vaultEconomyClass, bridge);
            getLogger().info("Vault bridge active.");
        } catch (Exception e) {
            getLogger().info("Vault API not found or error: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void registerServiceUnsafe(Class<?> serviceClass, Object provider) {
        Bukkit.getServicesManager().register((Class<T>) serviceClass, (T) provider, this, ServicePriority.Highest);
    }

    // --- Getters ---
    public static RoyalEconomy getInstance() { return instance; }
    public Economy getEconomy() { return economyManager; }
    public MessageManager getMessageManager() { return messageManager; }
    public BankManager getBankManager() { return bankManager; }
    public LeaderboardManager getLeaderboardManager() { return leaderboardManager; }
    public TransactionLogger getTransactionLogger() { return transactionLogger; }
}