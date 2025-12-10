package com.ArkOfNoah.RoyalEconomy.commands;

import com.ArkOfNoah.RoyalEconomy.RoyalEconomy;
import com.ArkOfNoah.RoyalEconomy.core.Bank;
import com.ArkOfNoah.RoyalEconomy.core.BankManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class BankCommand implements CommandExecutor {

    private final RoyalEconomy plugin;
    private final BankManager bankManager;
    private final FileConfiguration config;

    public BankCommand(RoyalEconomy plugin, BankManager bankManager) {
        this.plugin = plugin;
        this.bankManager = bankManager;
        this.config = plugin.getConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!config.getBoolean("banks.enabled", false)) {
            sender.sendMessage(color(applyPrefix("&cBanks are disabled.")));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(color(applyPrefix(config.getString("messages.player-only",
                    "&cOnly players can use this command."))));
            return true;
        }

        if (!sender.hasPermission("royaleconomy.bank.use")) {
            sender.sendMessage(color(applyPrefix(config.getString("messages.no-permission",
                    "&cNo permission."))));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "help" -> sendHelp(player);
            case "create" -> handleCreate(player, args);
            case "delete" -> handleDelete(player, args);
            case "list" -> handleList(player);
            case "info" -> handleInfo(player, args);
            case "deposit" -> handleDeposit(player, args);
            case "withdraw" -> handleWithdraw(player, args);
            case "invite" -> handleInvite(player, args);
            case "remove" -> handleRemove(player, args);
            default -> sendHelp(player);
        }

        return true;
    }

    private void sendHelp(Player player) {
        String header = config.getString("banks.messages.help-header",
                "%prefix% &6Bank commands:");
        player.sendMessage(color(applyPrefix(header)));
        player.sendMessage(color("&7/bank create <name>"));
        player.sendMessage(color("&7/bank delete <name>"));
        player.sendMessage(color("&7/bank list"));
        player.sendMessage(color("&7/bank info <name>"));
        player.sendMessage(color("&7/bank deposit <name> <amount>"));
        player.sendMessage(color("&7/bank withdraw <name> <amount>"));
        player.sendMessage(color("&7/bank invite <player> <name>"));
        player.sendMessage(color("&7/bank remove <player> <name>"));
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(color(applyPrefix("&cUsage: /bank create <name>")));
            return;
        }
        String name = args[1];

        Bank bank = bankManager.createBank(player.getUniqueId(), name);
        if (bank == null) {
            String msg = config.getString("banks.messages.max-banks-reached",
                    "%prefix% &cYou cannot create more banks or that name is taken.");
            player.sendMessage(color(applyPrefix(msg)));
            return;
        }

        String msg = config.getString("banks.messages.created",
                "%prefix% &aBank '&e%bank_name%&a' created.");
        msg = msg.replace("%bank_name%", bank.getName());
        player.sendMessage(color(applyPrefix(msg)));

        plugin.getTransactionLogger().log(
                player.getName(),
                "BANK:" + bank.getName(),
                0.0,
                "BANK_CREATE",
                true
        );
    }

    private void handleDelete(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(color(applyPrefix("&cUsage: /bank delete <name>")));
            return;
        }
        String name = args[1];
        Bank bank = bankManager.getBank(name);
        if (bank == null) {
            sendBankNotFound(player);
            return;
        }

        boolean isOwner = bank.getOwner().equals(player.getUniqueId());
        boolean isAdmin = player.hasPermission("royaleconomy.bank.admin");
        if (!isOwner && !isAdmin) {
            player.sendMessage(color(applyPrefix(config.getString("banks.messages.no-access",
                    "%prefix% &cYou don't have access to that bank."))));
            return;
        }

        boolean success = bankManager.deleteBank(name);
        if (!success) {
            player.sendMessage(color(applyPrefix(config.getString("messages.internal-error",
                    "%prefix% &cAn error occurred."))));
            return;
        }

        String msg = config.getString("banks.messages.deleted",
                "%prefix% &cBank '&e%bank_name%&c' deleted.");
        msg = msg.replace("%bank_name%", name);
        player.sendMessage(color(applyPrefix(msg)));

        plugin.getTransactionLogger().log(
                player.getName(),
                "BANK:" + name,
                0.0,
                "BANK_DELETE",
                true
        );
    }

    private void handleList(Player player) {
        List<Bank> banks = bankManager.getBanksForPlayer(player.getUniqueId());
        String header = config.getString("banks.messages.list-header",
                "%prefix% &6Your banks:");
        player.sendMessage(color(applyPrefix(header)));

        if (banks.isEmpty()) {
            player.sendMessage(color("&7- &cYou have no banks."));
            return;
        }

        String lineFmt = config.getString("banks.messages.list-entry",
                "&7- &e%bank_name% &7(&a%balance_formatted%&7)");
        for (Bank bank : banks) {
            String line = lineFmt
                    .replace("%bank_name%", bank.getName())
                    .replace("%balance_formatted%",
                            plugin.getEconomy().format(bank.getBalance()));
            player.sendMessage(color(applyPrefix(line)));
        }
    }

    private void handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(color(applyPrefix("&cUsage: /bank info <name>")));
            return;
        }
        String name = args[1];
        Bank bank = bankManager.getBank(name);
        if (bank == null) {
            sendBankNotFound(player);
            return;
        }

        String header = config.getString("banks.messages.info-header",
                "%prefix% &6Bank '&e%bank_name%&6' info:");
        header = header.replace("%bank_name%", bank.getName());
        player.sendMessage(color(applyPrefix(header)));

        String ownerName = Bukkit.getOfflinePlayer(bank.getOwner()).getName();
        if (ownerName == null) ownerName = bank.getOwner().toString();

        String ownerLine = config.getString("banks.messages.info-line-owner",
                "&7Owner: &e%owner%");
        ownerLine = ownerLine.replace("%owner%", ownerName);
        player.sendMessage(color(ownerLine));

        String balanceLine = config.getString("banks.messages.info-line-balance",
                "&7Balance: &a%balance_formatted%");
        balanceLine = balanceLine.replace("%balance_formatted%",
                plugin.getEconomy().format(bank.getBalance()));
        player.sendMessage(color(balanceLine));

        int memberCount = bank.getMembers().size();
        int maxMembers = bank.getMaxMembers();

        String membersLine = config.getString("banks.messages.info-line-members",
                "&7Members (&e%member_count%&7/&e%max_members%&7): &f%members%");
        String membersStr;
        if (bank.getMembers().isEmpty()) {
            membersStr = "-";
        } else {
            membersStr = bank.getMembers().stream()
                    .map(uuid -> {
                        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                        return op.getName() != null ? op.getName() : uuid.toString();
                    })
                    .collect(Collectors.joining(", "));
        }
        membersLine = membersLine
                .replace("%member_count%", String.valueOf(memberCount))
                .replace("%max_members%", String.valueOf(maxMembers))
                .replace("%members%", membersStr);
        player.sendMessage(color(membersLine));
    }

    private void handleDeposit(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(color(applyPrefix("&cUsage: /bank deposit <name> <amount>")));
            return;
        }
        String name = args[1];
        Bank bank = bankManager.getBank(name);
        if (bank == null) {
            sendBankNotFound(player);
            return;
        }
        if (!bank.isMember(player.getUniqueId())) {
            player.sendMessage(color(applyPrefix(config.getString("banks.messages.no-access",
                    "%prefix% &cYou don't have access to that bank."))));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(color(applyPrefix(config.getString("messages.invalid-amount",
                    "%prefix% &cInvalid amount."))));
            return;
        }
        if (amount <= 0) {
            player.sendMessage(color(applyPrefix(config.getString("messages.negative-amount",
                    "%prefix% &cAmount must be positive."))));
            return;
        }

        boolean success = bankManager.depositToBank(bank, player.getUniqueId(), amount);
        if (!success) {
            player.sendMessage(color(applyPrefix(config.getString("core.core-messages.insufficient-funds",
                    "%prefix% &cYou don't have enough money."))));
            return;
        }

        String msg = config.getString("banks.messages.deposit",
                "%prefix% &aDeposited %amount_formatted% &ainto '&e%bank_name%&a'.");
        msg = msg
                .replace("%bank_name%", bank.getName())
                .replace("%amount_formatted%", plugin.getEconomy().format(amount));
        player.sendMessage(color(applyPrefix(msg)));

        plugin.getTransactionLogger().log(
                player.getName(),
                "BANK:" + bank.getName(),
                amount,
                "BANK_DEPOSIT",
                true
        );
    }

    private void handleWithdraw(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(color(applyPrefix("&cUsage: /bank withdraw <name> <amount>")));
            return;
        }
        String name = args[1];
        Bank bank = bankManager.getBank(name);
        if (bank == null) {
            sendBankNotFound(player);
            return;
        }
        if (!bank.isMember(player.getUniqueId())) {
            player.sendMessage(color(applyPrefix(config.getString("banks.messages.no-access",
                    "%prefix% &cYou don't have access to that bank."))));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(color(applyPrefix(config.getString("messages.invalid-amount",
                    "%prefix% &cInvalid amount."))));
            return;
        }
        if (amount <= 0) {
            player.sendMessage(color(applyPrefix(config.getString("messages.negative-amount",
                    "%prefix% &cAmount must be positive."))));
            return;
        }

        boolean success = bankManager.withdrawFromBank(bank, player.getUniqueId(), amount);
        if (!success) {
            player.sendMessage(color(applyPrefix(config.getString("banks.messages.insufficient-funds",
                    "%prefix% &cThe bank does not have enough money."))));
            return;
        }

        String msg = config.getString("banks.messages.withdraw",
                "%prefix% &aWithdrew %amount_formatted% &afrom '&e%bank_name%&a'.");
        msg = msg
                .replace("%bank_name%", bank.getName())
                .replace("%amount_formatted%", plugin.getEconomy().format(amount));
        player.sendMessage(color(applyPrefix(msg)));

        plugin.getTransactionLogger().log(
                "BANK:" + bank.getName(),
                player.getName(),
                amount,
                "BANK_WITHDRAW",
                true
        );
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(color(applyPrefix("&cUsage: /bank invite <player> <name>")));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        String name = args[2];

        Bank bank = bankManager.getBank(name);
        if (bank == null) {
            sendBankNotFound(player);
            return;
        }

        if (!bank.getOwner().equals(player.getUniqueId())
                && !player.hasPermission("royaleconomy.bank.admin")) {
            player.sendMessage(color(applyPrefix(config.getString("banks.messages.no-access",
                    "%prefix% &cYou don't have access to that bank."))));
            return;
        }

        UUID targetId = target.getUniqueId();
        if (bank.getMembers().contains(targetId)) {
            player.sendMessage(color(applyPrefix(config.getString("banks.messages.already-member",
                    "%prefix% &cThat player is already a member."))));
            return;
        }

        if (!bank.addMember(targetId)) {
            player.sendMessage(color(applyPrefix(config.getString("banks.messages.max-banks-reached",
                    "%prefix% &cBank has reached maximum members."))));
            return;
        }

        String msg = config.getString("banks.messages.member-added",
                "%prefix% &a%target% added to bank '&e%bank_name%&a'.");
        msg = msg
                .replace("%target%", target.getName() != null ? target.getName() : targetId.toString())
                .replace("%bank_name%", bank.getName());
        player.sendMessage(color(applyPrefix(msg)));
    }

    private void handleRemove(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(color(applyPrefix("&cUsage: /bank remove <player> <name>")));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        String name = args[2];

        Bank bank = bankManager.getBank(name);
        if (bank == null) {
            sendBankNotFound(player);
            return;
        }

        if (!bank.getOwner().equals(player.getUniqueId())
                && !player.hasPermission("royaleconomy.bank.admin")) {
            player.sendMessage(color(applyPrefix(config.getString("banks.messages.no-access",
                    "%prefix% &cYou don't have access to that bank."))));
            return;
        }

        UUID targetId = target.getUniqueId();
        if (!bank.getMembers().contains(targetId)) {
            player.sendMessage(color(applyPrefix(config.getString("banks.messages.not-member",
                    "%prefix% &cThat player is not a member of that bank."))));
            return;
        }

        bank.removeMember(targetId);

        String msg = config.getString("banks.messages.member-removed",
                "%prefix% &c%target% removed from bank '&e%bank_name%&c'.");
        msg = msg
                .replace("%target%", target.getName() != null ? target.getName() : targetId.toString())
                .replace("%bank_name%", bank.getName());
        player.sendMessage(color(applyPrefix(msg)));
    }

    private void sendBankNotFound(Player player) {
        String msg = config.getString("banks.messages.bank-not-found",
                "%prefix% &cThat bank doesn't exist.");
        player.sendMessage(color(applyPrefix(msg)));
    }

    private String getPrefix() {
        if (!config.getBoolean("messages.use-prefix", true)) return "";
        return config.getString("messages.prefix", "&8[&6RoyalEconomy&8]&r ");
    }

    private String applyPrefix(String msg) {
        return msg == null ? "" : msg.replace("%prefix%", getPrefix());
    }

    private String color(String msg) {
        return msg == null ? "" : msg.replace("&", "§");
    }
}
