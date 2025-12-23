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
    private final Map<UUID, UUID> payTargets = new HashMap<>(); // Stores target player for transfers

    public static class BankHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() { return null; }
    }

    public BankGUI(RoyalEconomy plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private String getMenuTitle() {
        String guiTexture = OraxenHook.getGlyph("bank_gui");
        String alignGUI = OraxenHook.getShift(-11);

        if (guiTexture.isEmpty()) {
            return ChatColor.DARK_BLUE + "Bank Account";
        }
        return ChatColor.WHITE + alignGUI + guiTexture;
    }

    public void openBank(Player player) {
        BankManager bm = plugin.getBankManager();

        if (!bm.hasBankAccount(player.getUniqueId())) {
            bm.createBankAccount(player.getUniqueId());
        }

        double walletBal = plugin.getEconomy().getBalance(player.getUniqueId());
        double bankBal = bm.getBankBalance(player.getUniqueId());

        Inventory inv = Bukkit.createInventory(new BankHolder(), 27, getMenuTitle());

        // --- 1. DEPOSIT (Slot 12) ---
        setButton(inv, "transparent", "&a&lDeposit",
                Arrays.asList("&7Click to deposit", "&7money from your wallet."),
                12);

        // --- 2. BANK BALANCE (Slot 13 - Center) ---
        setButton(inv, "transparent", "&6&lBank Balance",
                Arrays.asList("&f" + plugin.getEconomy().format(bankBal)),
                13);

        // --- 3. WITHDRAW (Slot 14) ---
        setButton(inv, "transparent", "&c&lWithdraw",
                Arrays.asList("&7Click to withdraw", "&7money to your wallet."),
                14);

        // --- 4. PAY / TRANSFER (Slot 15 - New!) ---
        setButton(inv, "transparent", "&b&lPay Player",
                Arrays.asList("&7Transfer money from", "&7your bank to a player."),
                15);

        // --- 5. WALLET BALANCE (Slot 22) ---
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
            else if (name.contains("Pay Player")) {
                p.closeInventory();
                inputMode.put(p.getUniqueId(), "PAY_NAME");
                p.sendMessage(ChatColor.AQUA + "Type the NAME of the player to pay (or 'cancel'):");
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
        String msg = e.getMessage();

        // 1. Handle Cancel
        if (msg.equalsIgnoreCase("cancel") || msg.equalsIgnoreCase("exit")) {
            p.sendMessage(ChatColor.RED + "Cancelled.");
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
            payTargets.remove(p.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> openBank(p));
            return;
        }

        // 2. Handle Name Input (Text)
        if (mode.equals("PAY_NAME")) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player target = Bukkit.getPlayer(msg); // Must be online to pay
                if (target == null) {
                    p.sendMessage(ChatColor.RED + "Player '" + msg + "' not found or offline.");
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
                    openBank(p);
                } else if (target.getUniqueId().equals(p.getUniqueId())) {
                    p.sendMessage(ChatColor.RED + "You cannot pay yourself.");
                    openBank(p);
                } else {
                    // Valid Target -> Ask for Amount
                    payTargets.put(p.getUniqueId(), target.getUniqueId());
                    inputMode.put(p.getUniqueId(), "PAY_AMOUNT");
                    p.sendMessage(ChatColor.AQUA + "Paying " + target.getName() + ". Type amount:");
                    p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                }
            });
            return;
        }

        // 3. Handle Amount Input (Number)
        double amount;
        try {
            amount = Double.parseDouble(msg);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            p.sendMessage(ChatColor.RED + "Invalid number.");
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
            payTargets.remove(p.getUniqueId()); // Cleanup
            return;
        }

        // 4. Process Transaction Sync
        Bukkit.getScheduler().runTask(plugin, () -> {
            BankManager bm = plugin.getBankManager();
            boolean success = false;

            if (mode.equals("DEPOSIT")) {
                if (bm.deposit(p.getUniqueId(), amount)) {
                    p.sendMessage(ChatColor.GREEN + "Deposited: " + plugin.getEconomy().format(amount));
                    success = true;
                } else {
                    p.sendMessage(ChatColor.RED + "Insufficient wallet funds.");
                }
            }
            else if (mode.equals("WITHDRAW")) {
                if (bm.withdraw(p.getUniqueId(), amount)) {
                    p.sendMessage(ChatColor.GREEN + "Withdrawn: " + plugin.getEconomy().format(amount));
                    success = true;
                } else {
                    p.sendMessage(ChatColor.RED + "Insufficient bank balance.");
                }
            }
            else if (mode.equals("PAY_AMOUNT")) {
                UUID targetUUID = payTargets.remove(p.getUniqueId());
                if (targetUUID != null) {
                    // Transfer: Withdraw from Bank -> Deposit to Target Wallet
                    if (bm.getBankBalance(p.getUniqueId()) >= amount) {
                        bm.withdraw(p.getUniqueId(), amount); // Take from Bank
                        plugin.getEconomy().deposit(targetUUID, amount); // Give to Target Wallet

                        p.sendMessage(ChatColor.GREEN + "Transferred " + plugin.getEconomy().format(amount) + " from bank.");

                        Player target = Bukkit.getPlayer(targetUUID);
                        if (target != null) {
                            target.sendMessage(ChatColor.GREEN + "Received " + plugin.getEconomy().format(amount) + " from " + p.getName());
                            target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                        }
                        success = true;
                    } else {
                        p.sendMessage(ChatColor.RED + "Insufficient bank funds for transfer.");
                    }
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