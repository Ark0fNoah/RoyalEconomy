package com.ArkOfNoah.RoyalEconomy.core;

import com.ArkOfNoah.RoyalEconomy.RoyalEconomy;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TransactionLogger {

    private final RoyalEconomy plugin;
    private final boolean enabled;
    private final boolean logToFile;
    private final boolean logToConsole;
    private final String format;
    private final File logFolder;
    private final SimpleDateFormat dateFileFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    public TransactionLogger(RoyalEconomy plugin) {
        this.plugin = plugin;
        var cfg = plugin.getConfig();
        this.enabled = cfg.getBoolean("logging.enabled", true);
        this.logToFile = cfg.getBoolean("logging.log-to-file", true);
        this.logToConsole = cfg.getBoolean("logging.log-to-console", false);
        this.format = cfg.getString("logging.format",
                "[%time%] %type% | %player% -> %target% | %amount% | %reason%");

        this.logFolder = new File(plugin.getDataFolder(), "logs");
        if (!logFolder.exists() && !logFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create logs folder: " + logFolder.getPath());
        }
    }

    public void log(String player, String target, double amount, String reason, boolean success) {
        if (!enabled) return;

        Date now = new Date();
        String time = timeFormat.format(now);
        String type = success ? "SUCCESS" : "FAIL";

        if (player == null) player = "-";
        if (target == null) target = "-";
        if (reason == null) reason = "OTHER";

        String line = format
                .replace("%time%", time)
                .replace("%type%", type)
                .replace("%player%", player)
                .replace("%target%", target)
                .replace("%amount%", String.valueOf(amount))
                .replace("%reason%", reason);

        if (logToConsole) {
            plugin.getLogger().info(line);
        }

        if (logToFile) {
            String fileName = dateFileFormat.format(now) + ".log";
            File file = new File(logFolder, fileName);
            try (FileWriter writer = new FileWriter(file, true)) {
                writer.write(line);
                writer.write(System.lineSeparator());
            } catch (IOException e) {
                plugin.getLogger().severe("Could not write transaction log: " + e.getMessage());
            }
        }
    }
}
