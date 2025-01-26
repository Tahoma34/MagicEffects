// MagicEffects.java
package org.tahoma.magiceffects;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.ChatColor;

public final class MagicEffects extends JavaPlugin {

    private static MagicEffects instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Регистрируем команды
        getCommand("magiceffects").setExecutor(new CommandManager());

        // Регистрируем слушатель для GUI
        getServer().getPluginManager().registerEvents(new MenuListener(), this);

        // Лог успешного включения
        getLogger().info(ChatColor.GREEN + "MagicEffects плагин включен и готов к использованию!");
    }

    @Override
    public void onDisable() {
        getLogger().info(ChatColor.RED + "MagicEffects плагин выключен.");
    }

    public static MagicEffects getInstance() {
        return instance;
    }
}