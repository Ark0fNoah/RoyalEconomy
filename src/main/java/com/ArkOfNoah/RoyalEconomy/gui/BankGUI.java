package com.ArkOfNoah.RoyalEconomy.gui;

import com.ArkOfNoah.RoyalEconomy.RoyalEconomy;
import com.ArkOfNoah.RoyalEconomy.core.BankManager;
import com.ArkOfNoah.RoyalEconomy.utils.OraxenHook;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class BankGUI implements Listener {

    private final RoyalEconomy plugin;
    private final Map<UUID, String> inputMode = new HashMap<>();

    public static class BankHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() { return null; }
    }

    public BankGUI(RoyalEconomy plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Generates the Title (Texture Only - No Text)
     */
    private String getMenuTitle() {
        String guiTexture = OraxenHook.getGlyph("bank_gui");
        String alignGUI = OraxenHook.getShift(-11);

        if (guiTexture.isEmpty()) {
            return ChatColor.DARK_BLUE + "Bank Account";
        }
        // Structure: [White Color] + [Align GUI] + [Texture]
        return ChatColor.WHITE + alignGUI + guiTexture;
    }

    public void openBank(Player player) {
        BankManager bm = plugin.getBankManager();

        if (!bm.hasBankAccount(player.getUniqueId())) {
            bm.createBankAccount(player.getUniqueId());
        }

        double walletBal = plugin.getEconomy().getBalance(player.getUniqueId());
        double bankBal = bm.getBankBalance(player.getUniqueId());

        // Create Inventory with Texture Title (No Balance Text in Title)
        Inventory inv = Bukkit.createInventory(new BankHolder(), 27, getMenuTitle());

        // --- 1. DEPOSIT Button (Slot 12) ---
        setButton(inv, "transparent", "&a&lDeposit",
                Arrays.asList("&7Click to deposit", "&7money from your wallet."),
                12);

        // --- 2. WITHDRAW Button (Slot 14) ---
        setButton(inv, "transparent", "&c&lWithdraw",
                Arrays.asList("&7Click to withdraw", "&7money to your wallet."),
                14);

        // --- 3. BANK BALANCE (Slot 13) ---
        setButton(inv, "transparent", "&6&lBank Balance",
                Arrays.asList("&f" + plugin.getEconomy().format(bankBal)),
                13);

        // --- 4. WALLET BALANCE (Slot 22) ---
        setButton(inv, "transparent", "&e&lWallet Balance",
                Arrays.asList("&f" + plugin.getEconomy().format(walletBal)),
                22);

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.0f);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof BankHolder)) return;

        e.setCancelled(true);
        if (e.getCurrentItem() == null) return;

        Player p = (Player) e.getWhoClicked();
        ItemStack clicked = e.getCurrentItem();
        String oraxenId = OraxenItems.getIdByItem(clicked);

        if (oraxenId != null && oraxenId.equals("transparent")) {
            String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

            if (name.contains("Deposit")) {
                p.closeInventory();
                inputMode.put(p.getUniqueId(), "DEPOSIT");
                p.sendMessage(ChatColor.GREEN + "Type amount to DEPOSIT in chat (or 'cancel'):");
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            }
            else if (name.contains("Withdraw")) {
                p.closeInventory();
                inputMode.put(p.getUniqueId(), "WITHDRAW");
                p.sendMessage(ChatColor.GOLD + "Type amount to WITHDRAW in chat (or 'cancel'):");
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        if (!inputMode.containsKey(p.getUniqueId())) return;

        e.setCancelled(true);
        String mode = inputMode.remove(p.getUniqueId());
        String msg = e.getMessage().toLowerCase();

        if (msg.equals("cancel") || msg.equals("exit")) {
            p.sendMessage(ChatColor.RED + "Cancelled.");
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
            Bukkit.getScheduler().runTask(plugin, () -> openBank(p));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(msg);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            p.sendMessage(ChatColor.RED + "Invalid number.");
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            BankManager bm = plugin.getBankManager();
            boolean success = false;

            if (mode.equals("DEPOSIT")) {
                if (bm.deposit(p.getUniqueId(), amount)) {
                    p.sendMessage(ChatColor.GREEN + "Deposited: " + plugin.getEconomy().format(amount));
                    success = true;
                } else {
                    p.sendMessage(ChatColor.RED + "Insufficient funds.");
                }
            } else {
                if (bm.withdraw(p.getUniqueId(), amount)) {
                    p.sendMessage(ChatColor.GREEN + "Withdrawn: " + plugin.getEconomy().format(amount));
                    success = true;
                } else {
                    p.sendMessage(ChatColor.RED + "Insufficient bank balance.");
                }
            }

            if (success) {
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.0f);
            } else {
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
            }

            openBank(p);
        });
    }

    private void setButton(Inventory inv, String oraxenId, String name, List<String> lore, int... slots) {
        ItemStack item = OraxenHook.getItem(oraxenId, Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            List<String> coloredLore = new ArrayList<>();
            for (String l : lore) coloredLore.add(ChatColor.translateAlternateColorCodes('&', l));
            meta.setLore(coloredLore);
            item.setItemMeta(meta);
        }

        for (int slot : slots) {
            if (slot < inv.getSize()) {
                inv.setItem(slot, item);
            }
        }
    }
}