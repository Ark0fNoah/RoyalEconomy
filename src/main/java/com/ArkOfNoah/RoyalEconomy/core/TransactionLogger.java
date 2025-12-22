package com.ArkOfNoah.RoyalEconomy.core;

import com.ArkOfNoah.RoyalEconomy.RoyalEconomy;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

public class TransactionLogger {

    private final RoyalEconomy plugin;
    private final File logFile;

    public TransactionLogger(RoyalEconomy plugin) {
        this.plugin = plugin;
        this.logFile = new File(plugin.getDataFolder(), "transactions.log");
        createFile();
    }

    private void createFile() {
        if (!logFile.exists()) {
            try {
                logFile.getParentFile().mkdirs();
                logFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Could not create transactions.log", e);
            }
        }
    }

    /**
     * Standard logging method used by commands.
     */
    public void logTransaction(String sender, String target, double amount, String type) {
        log(sender, target, amount, type, false);
    }

    /**
     * flexible logging method used by InterestTask.
     */
    public void log(String sender, String target, double amount, String type, boolean consoleLog) {
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String entry = String.format("[%s] [%s] %s -> %s: %.2f", date, type, sender, target, amount);

        // Write to file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write(entry);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Optional Console Log
        if (consoleLog) {
            plugin.getLogger().info(entry);
        }
    }
}