package com.ArkOfNoah.RoyalEconomy;

import com.ArkOfNoah.RoyalEconomy.api.Economy;
import com.ArkOfNoah.RoyalEconomy.core.EconomyManager;
import com.ArkOfNoah.RoyalEconomy.core.StorageHandler;
import com.ArkOfNoah.RoyalEconomy.commands.BalanceCommand;
import com.ArkOfNoah.RoyalEconomy.commands.PayCommand;
import com.ArkOfNoah.RoyalEconomy.commands.EcoAdminCommand;
import com.ArkOfNoah.RoyalEconomy.listeners.PlayerJoinListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public class RoyalEconomy extends JavaPlugin {

    private static RoyalEconomy instance;
    private EconomyManager economyManager;
    private StorageHandler storageHandler;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        this.storageHandler = new StorageHandler(this);
        this.storageHandler.load(); // load balances from file

        this.economyManager = new EconomyManager(this, storageHandler);

        registerCommands();
        registerListeners();
        registerService();

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
            storageHandler.save(); // save balances to file
        }
        getLogger().info("RoyalEconomy disabled!");
    }

    private void registerCommands() {
        getCommand("balance").setExecutor(new BalanceCommand(economyManager));
        getCommand("pay").setExecutor(new PayCommand(economyManager));
        getCommand("eco").setExecutor(new EcoAdminCommand(economyManager));
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(
                new PlayerJoinListener(economyManager, getConfig()),
                this
        );
    }

    private void registerService() {
        // Expose as a service (like Vault does) so RoyalCore + Shop can fetch it
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
}
