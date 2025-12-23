package com.ArkOfNoah.RoyalEconomy.listeners;

import com.ArkOfNoah.RoyalEconomy.RoyalEconomy;
import com.ArkOfNoah.RoyalEconomy.core.BankManager;
import com.ArkOfNoah.RoyalEconomy.core.EconomyManager;
import com.ArkOfNoah.RoyalEconomy.gui.BankGUI;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GUIListener implements Listener {

    private final RoyalEconomy plugin;
    private final BankGUI bankGUI;

    public GUIListener(RoyalEconomy plugin, BankGUI bankGUI) {
        this.plugin = plugin;
        this.bankGUI = bankGUI;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        String configTitle = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("gui.bank.title", "&8Bank Vault"));

        // Check if it's our GUI
        if (!title.equals(configTitle)) return;

        event.setCancelled(true); // Prevent taking items

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        int slot = event.getSlot();
        BankManager bm = plugin.getBankManager();
        EconomyManager em = (EconomyManager) plugin.getEconomy();

        // DEPOSIT LOGIC (Slot 11)
        if (slot == 11) {
            if (event.isLeftClick()) {
                // Deposit 100
                performTransaction(player, "deposit", 100.0);
            } else if (event.isRightClick()) {
                // Deposit All
                double wallet = em.getBalance(player.getUniqueId());
                performTransaction(player, "deposit", wallet);
            }
        }

        // WITHDRAW LOGIC (Slot 15)
        else if (slot == 15) {
            if (event.isLeftClick()) {
                // Withdraw 100
                performTransaction(player, "withdraw", 100.0);
            } else if (event.isRightClick()) {
                // Withdraw All
                double bankBal = bm.getBankBalance(player.getUniqueId());
                performTransaction(player, "withdraw", bankBal);
            }
        }
    }

    private void performTransaction(Player player, String type, double amount) {
        if (amount <= 0) return;

        // Use the command logic or direct manager calls
        // Direct calls are cleaner here:
        if (type.equals("deposit")) {
            boolean success = plugin.getBankManager().deposit(player.getUniqueId(), amount);
            if (success) {
                player.sendMessage(ChatColor.GREEN + "Deposited " + plugin.getEconomy().format(amount));
            } else {
                player.sendMessage(ChatColor.RED + "Cannot deposit (Insufficient funds or bank full).");
            }
        } else {
            boolean success = plugin.getBankManager().withdraw(player.getUniqueId(), amount);
            if (success) {
                player.sendMessage(ChatColor.GREEN + "Withdrew " + plugin.getEconomy().format(amount));
            } else {
                player.sendMessage(ChatColor.RED + "Cannot withdraw (Insufficient bank funds).");
            }
        }

        // Refresh GUI
        bankGUI.openBank(player);
    }
}