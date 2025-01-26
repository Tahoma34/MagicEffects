// CommandManager.java
package org.tahoma.magiceffects;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandManager implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Команду может использовать только игрок
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Эту команду может использовать только игрок.");
            return true;
        }
        Player player = (Player) sender;

        // Если передали хотя бы один аргумент
        if (args.length == 1) {

            // Подкоманда: /magiceffects menu
            if (args[0].equalsIgnoreCase("menu")) {
                MenuListener.openEffectsMenu(player);
                return true;
            }

            // Подкоманда: /magiceffects reload
            if (args[0].equalsIgnoreCase("reload")) {
                if (!player.hasPermission("magiceffects.reload")) {
                    player.sendMessage(ChatColor.RED + "У вас недостаточно прав для использования этой команды.");
                    return true;
                }
                MagicEffects.getInstance().reloadConfig();
                player.sendMessage(ChatColor.GREEN + "Конфигурация плагина перезагружена.");
                return true;
            }

            // Подкоманда: /magiceffects reset
            if (args[0].equalsIgnoreCase("reset")) {
                if (!player.hasPermission("magiceffects.reset")) {
                    player.sendMessage(ChatColor.RED + "У вас недостаточно прав для использования этой команды.");
                    return true;
                }
                ParticleManager.resetEffects();
                player.sendMessage(ChatColor.GREEN + "Все эффекты и механики сброшены.");
                return true;
            }

            // Неизвестная подкоманда
            player.sendMessage(ChatColor.YELLOW + "Неизвестная команда. Используйте: /magiceffects <menu|reload|reset>");
            return true;
        }

        // Если нет аргументов, подсказываем синтаксис
        player.sendMessage(ChatColor.YELLOW + "Использование: /magiceffects <menu|reload|reset>");
        return true;
    }
}