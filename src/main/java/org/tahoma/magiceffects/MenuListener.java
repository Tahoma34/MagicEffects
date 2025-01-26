package org.tahoma.magiceffects;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class MenuListener implements Listener {

    public enum MechanicType {
        CIRCLE_FEET("Круг у ног"),
        WAVE_UP("Волна вверх"),
        CONE_UP("Конус вверх"),
        ROTATING_BEAM("Вращающийся луч"),
        TRAIL_BEHIND("Шлейф позади"),
        ARC("Дуга"),
        NIMB("Нимб");

        private final String displayName;

        MechanicType(String name) {
            this.displayName = name;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private static final Map<UUID, ParticleEffectType> selectedEffects = new HashMap<>();
    private static final Map<UUID, MechanicType> selectedMechanics = new HashMap<>();

    public static void openEffectsMenu(Player player) {
        // Титул меню
        String rawTitle = MagicEffects.getInstance().getConfig().getString("messages.menuTitle", "&5Меню эффектов");
        String menuTitle = ChatColor.translateAlternateColorCodes('&', rawTitle);
        Inventory menu = Bukkit.createInventory(null, 54, menuTitle);

        // Базовые строки из конфига, переводим & в игровые цвета
        String commonRarity = ChatColor.translateAlternateColorCodes(
                '&',
                MagicEffects.getInstance().getConfig().getString("messages.commonRarity", "Обычная")
        );
        String accessibleDefault = ChatColor.translateAlternateColorCodes(
                '&',
                MagicEffects.getInstance().getConfig().getString("messages.accessibleDefault", "Доступно")
        );
        String notAccessibleDefault = ChatColor.translateAlternateColorCodes(
                '&',
                MagicEffects.getInstance().getConfig().getString("messages.notAccessibleDefault", "Недоступно")
        );

        // Получаем все эффекты, для которых есть конфигурация
        ParticleEffectType[] effects = ParticleEffectType.values();
        int fallbackSlot = 10;

        for (ParticleEffectType effect : effects) {
            ConfigurationSection effectSection = MagicEffects.getInstance()
                    .getConfig()
                    .getConfigurationSection("effects." + effect.getConfigKey());

            if (effectSection == null) {
                // Если у эффекта нет секции в config.yml, пропускаем
                continue;
            }

            // Берём материал из конфига, при ошибке - используем дефолт
            String configuredMaterial = effectSection.getString("material");
            Material finalMaterial;
            try {
                finalMaterial = (configuredMaterial != null)
                        ? Material.valueOf(configuredMaterial)
                        : effect.getMaterial();
            } catch (IllegalArgumentException e) {
                finalMaterial = effect.getMaterial();
            }

            // Слот по умолчанию, если не указано корректное значение
            int slot = effectSection.getInt("slot", fallbackSlot);
            if (slot < 0 || slot >= 54) {
                slot = fallbackSlot;
            }

            // Создаём предмет-иконку эффекта
            ItemStack item = new ItemStack(finalMaterial);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                // Редкость
                String rarityRaw = effectSection.getString("rarity", commonRarity);
                String rarityColored = ChatColor.translateAlternateColorCodes('&', rarityRaw);

                // Доступность
                boolean hasPerm = player.hasPermission(effect.getPermissionNode());
                String availabilityRaw = hasPerm ? accessibleDefault : notAccessibleDefault;
                String availabilityColored = ChatColor.translateAlternateColorCodes('&', availabilityRaw);

                // Название эффекта (может включать цвета)
                String effectNameColored = ChatColor.translateAlternateColorCodes('&', effect.getDisplayName());

                // Устанавливаем имя и описание
                meta.setDisplayName(effectNameColored);
                meta.setLore(Arrays.asList(
                        ChatColor.GRAY + "Редкость: " + rarityColored,
                        ChatColor.GRAY + "Доступность: " + availabilityColored
                ));
                item.setItemMeta(meta);
            }

            menu.setItem(slot, item);
            fallbackSlot++;
        }

        // Выпадающий список механик
        MechanicType[] mechanics = MechanicType.values();
        int[] mechanicSlots = {37, 38, 39, 40, 41, 42, 43};
        for (int i = 0; i < mechanicSlots.length && i < mechanics.length; i++) {
            MechanicType mech = mechanics[i];
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                // Имя механики
                String mechanicName = ChatColor.translateAlternateColorCodes('&', mech.getDisplayName());

                meta.setDisplayName(ChatColor.AQUA + mechanicName);
                meta.setLore(Collections.singletonList(
                        ChatColor.GRAY + "Запуск механики: " + mechanicName
                ));
                item.setItemMeta(meta);
            }
            menu.setItem(mechanicSlots[i], item);
        }

        player.openInventory(menu);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Сравниваем заголовки
        String rawTitle = MagicEffects.getInstance().getConfig().getString("messages.menuTitle", "&5Меню эффектов");
        String configTitleStripped = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', rawTitle));
        String currentTitleStripped = ChatColor.stripColor(event.getView().getTitle());

        if (!currentTitleStripped.equals(configTitleStripped)) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || !clicked.hasItemMeta()) {
            return;
        }

        String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

        // Проверка на выбранный эффект
        for (ParticleEffectType effect : ParticleEffectType.values()) {
            if (name.equals(ChatColor.stripColor(effect.getDisplayName()))) {
                String effectSelectedRaw = MagicEffects.getInstance().getConfig().getString(
                        "messages.effectSelected",
                        "&aВы выбрали эффект: "
                );
                String effectSelected = ChatColor.translateAlternateColorCodes('&', effectSelectedRaw);

                player.sendMessage(effectSelected + ChatColor.translateAlternateColorCodes('&', effect.getDisplayName()));
                selectedEffects.put(player.getUniqueId(), effect);

                // Если механика уже выбрана, запускаем сразу
                MechanicType savedMechanic = selectedMechanics.get(player.getUniqueId());
                if (savedMechanic != null) {
                    ParticleManager.startEffectIndefinitely(player, effect, savedMechanic.getDisplayName());
                }
                return;
            }
        }

        // Проверка на выбранную механику
        for (MechanicType mechanic : MechanicType.values()) {
            if (name.equals(ChatColor.stripColor(mechanic.getDisplayName()))) {
                selectedMechanics.put(player.getUniqueId(), mechanic);

                ParticleEffectType chosenEffect = selectedEffects.get(player.getUniqueId());
                if (chosenEffect == null) {
                    String noEffectSelectedRaw = MagicEffects.getInstance().getConfig().getString(
                            "messages.noEffectSelected",
                            "&cСначала выберите эффект!"
                    );
                    String noEffectSelected = ChatColor.translateAlternateColorCodes('&', noEffectSelectedRaw);
                    player.sendMessage(noEffectSelected);
                } else {
                    String startEffectMessageRaw = MagicEffects.getInstance().getConfig().getString(
                            "messages.startEffectMessage",
                            "&aЗапускаем %effect% с механикой: %mechanic%"
                    );
                    String startEffectMessage = ChatColor.translateAlternateColorCodes('&', startEffectMessageRaw)
                            .replace("%effect%", ChatColor.translateAlternateColorCodes('&', chosenEffect.getDisplayName()))
                            .replace("%mechanic%", ChatColor.translateAlternateColorCodes('&', mechanic.getDisplayName()));

                    player.sendMessage(startEffectMessage);
                    ParticleManager.startEffectIndefinitely(player, chosenEffect, mechanic.getDisplayName());
                }
                return;
            }
        }

        // Если пункт не распознан
        String unrecognizedItemRaw = MagicEffects.getInstance().getConfig().getString(
                "messages.unrecognizedItem",
                "&cНевозможно распознать выбранный предмет!"
        );
        String unrecognizedItem = ChatColor.translateAlternateColorCodes('&', unrecognizedItemRaw);
        player.sendMessage(unrecognizedItem);
    }
}