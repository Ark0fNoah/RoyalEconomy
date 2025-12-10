package com.ArkOfNoah.RoyalEconomy.listeners;

import com.ArkOfNoah.RoyalEconomy.core.EconomyManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final EconomyManager economy;
    private final FileConfiguration config;

    public PlayerJoinListener(EconomyManager economy, FileConfiguration config) {
        this.economy = economy;
        this.config = config;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!config.getBoolean("give-starting-balance-on-every-join", false)) {
            // Only apply if player doesn't have a stored balance yet
            // (depending on your logic; here we check if it's equal to default)
            double balance = economy.getBalance(player.getUniqueId());
            if (balance == economy.getDefaultBalance()) {
                return; // already has default
            }
        }
        // If you want a one-time starting bonus, you can track that separately in another file/section.
    }
}
