package com.ArkOfNoah.RoyalEconomy;

import com.ArkOfNoah.RoyalEconomy.api.Economy;
import com.ArkOfNoah.RoyalEconomy.core.EconomyManager;
import com.ArkOfNoah.RoyalEconomy.core.StorageHandler;
import com.ArkOfNoah.RoyalEconomy.core.TransactionLogger;
import com.ArkOfNoah.RoyalEconomy.core.LeaderboardManager;
import com.ArkOfNoah.RoyalEconomy.core.BankManager;
import com.ArkOfNoah.RoyalEconomy.core.BoostManager;
import com.ArkOfNoah.RoyalEconomy.core.InterestTask;
import com.ArkOfNoah.RoyalEconomy.core.TaxManager;
import com.ArkOfNoah.RoyalEconomy.commands.BalanceCommand;
import com.ArkOfNoah.RoyalEconomy.commands.RoyalEconomyCommand;
import com.ArkOfNoah.RoyalEconomy.commands.RoyalEconomyTabCompleter;
import com.ArkOfNoah.RoyalEconomy.commands.PayCommand;
import com.ArkOfNoah.RoyalEconomy.commands.EcoAdminCommand;
import com.ArkOfNoah.RoyalEconomy.commands.BaltopCommand;
import com.ArkOfNoah.RoyalEconomy.commands.BankCommand;
import com.ArkOfNoah.RoyalEconomy.listeners.PlayerJoinListener;
import com.ArkOfNoah.RoyalEconomy.vault.RoyalEconomyVaultBridge;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public class RoyalEconomy extends JavaPlugin {

    // Vault2 provider (VaultUnlocked)
    private net.milkbowl.vault2.economy.Economy vaultProvider;

    private static RoyalEconomy instance;

    private TaxManager taxManager;
    private BoostManager boostManager;
    private InterestTask interestTask;

    private EconomyManager economyManager;
    private StorageHandler storageHandler;

    private TransactionLogger transactionLogger;
    private LeaderboardManager leaderboardManager;
    private BankManager bankManager;

    // ─────────────────────────────────────
    // Vault2 bridge (ONLY Vault2)
    // ─────────────────────────────────────
    private void setupVaultBridge() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().info("VaultUnlocked/Vault not found. Skipping Vault2 bridge.");
            return;
        }

        // Our Vault2 Economy implementation
        vaultProvider = new RoyalEconomyVaultBridge(this, economyManager);

        // Register as Vault2 Economy provider
        Bukkit.getServicesManager().register(
                net.milkbowl.vault2.economy.Economy.class,
                vaultProvider,
                this,
                ServicePriority.Highest
        );

        getLogger().info("Registered RoyalEconomy as VaultUnlocked (Vault2) economy provider.");
    }

    public void reloadLogger() {
        this.transactionLogger = new TransactionLogger(this);
    }

    public void reloadLeaderboard() {
        int cacheSeconds = getConfig().getInt("baltop.cache-seconds", 30);
        leaderboardManager = new LeaderboardManager(economyManager, cacheSeconds);
    }

    public TaxManager getTaxManager() {
        return taxManager;
    }

    public BoostManager getBoostManager() {
        return boostManager;
    }

    @Override
    public void onEnable() {
        instance = this;

        // Config
        saveDefaultConfig();

        // Economy storage + manager
        storageHandler = new StorageHandler(this);
        storageHandler.load();
        economyManager = new EconomyManager(this, storageHandler);

        // Logging
        transactionLogger = new TransactionLogger(this);

        // Leaderboard
        int cacheSeconds = getConfig().getInt("baltop.cache-seconds", 30);
        leaderboardManager = new LeaderboardManager(economyManager, cacheSeconds);

        // Banks
        bankManager = new BankManager(this, economyManager);
        if (getConfig().getBoolean("banks.enabled", false)) {
            bankManager.load();
        }

        // Boosts
        boostManager = new BoostManager(this);

        // Taxes
        taxManager = new TaxManager(this, economyManager);

        // Interest
        if (getConfig().getBoolean("interest.enabled", false)) {
            int minutes = getConfig().getInt("interest.interval-minutes", 60);
            long ticks = minutes * 60L * 20L;
            if (ticks <= 0) ticks = 20L * 60L; // fallback 1 minute

            interestTask = new InterestTask(this, economyManager, bankManager, boostManager);
            interestTask.runTaskTimer(this, ticks, ticks);
            getLogger().info("InterestTask scheduled every " + minutes + " minute(s).");
        }

        registerService();
        registerCommands();
        registerListeners();

        // Vault2 bridge
        setupVaultBridge();

        // PlaceholderAPI expansion
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new com.ArkOfNoah.RoyalEconomy.placeholder.RoyalEconomyExpansion(this).register();
            getLogger().info("Registered RoyalEconomy PlaceholderAPI expansion.");
        } else {
            getLogger().info("PlaceholderAPI not found. Skipping PAPI expansion registration.");
        }

        getLogger().info("RoyalEconomy enabled!");
    }

    @Override
    public void onDisable() {
        if (storageHandler != null) {
            storageHandler.save();
        }
        if (bankManager != null && getConfig().getBoolean("banks.enabled", false)) {
            bankManager.save();
        }

        if (interestTask != null) {
            interestTask.cancel();
        }

        getLogger().info("RoyalEconomy disabled!");
    }

    private void registerCommands() {

        RoyalEconomyTabCompleter tab = new RoyalEconomyTabCompleter(bankManager);

        // /royaleconomy
        getCommand("royaleconomy").setExecutor(new RoyalEconomyCommand(this));
        getCommand("royaleconomy").setTabCompleter(tab);

        // /balance
        getCommand("balance").setExecutor(new BalanceCommand(economyManager));
        getCommand("balance").setTabCompleter(tab);

        // /pay
        getCommand("pay").setExecutor(new PayCommand(this, economyManager));
        getCommand("pay").setTabCompleter(tab);

        // /eco
        getCommand("eco").setExecutor(new EcoAdminCommand(this, economyManager));
        getCommand("eco").setTabCompleter(tab);

        // /baltop
        if (getConfig().getBoolean("baltop.enabled", true)) {
            getCommand("baltop").setExecutor(new BaltopCommand(this, leaderboardManager));
            getCommand("baltop").setTabCompleter(tab);
        }

        // /bank
        if (getConfig().getBoolean("banks.enabled", false)) {
            getCommand("bank").setExecutor(new BankCommand(this, bankManager));
            getCommand("bank").setTabCompleter(tab);
        }
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(
                new PlayerJoinListener(economyManager, getConfig()),
                this
        );
    }

    private void registerService() {
        // Register your own Economy API as a Bukkit service
        Bukkit.getServicesManager().register(
                Economy.class,
                economyManager,
                this,
                ServicePriority.Normal
        );
    }

    public static RoyalEconomy getInstance() {
        return instance;
    }

    public Economy getEconomy() {
        return economyManager;
    }

    public TransactionLogger getTransactionLogger() {
        return transactionLogger;
    }

    public BankManager getBankManager() {
        return bankManager;
    }

    public LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }
}
