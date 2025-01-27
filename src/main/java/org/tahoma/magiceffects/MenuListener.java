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
        CIRCLE_FEET("circle_feet"),
        WAVE_UP("wave_up"),
        CONE_UP("cone_up"),
        ROTATING_BEAM("rotating_beam"),
        TRAIL_BEHIND("trail_behind"),
        ARC("arc"),
        NIMB("nimb");

        private final String configKey;

        MechanicType(String configKey) {
            this.configKey = configKey;
        }

        public String getConfigKey() {
            return configKey;
        }

        public String getDisplayName() {
            String configName = MagicEffects.getInstance()
                    .getConfig()
                    .getString("mechanics." + configKey, configKey); // Получаем имя из конфигурации
            return ChatColor.translateAlternateColorCodes('&', configName); // Поддержка цвета
        }

        public static MechanicType fromString(String name) {
            for (MechanicType mechanic : MechanicType.values()) {
                if (mechanic.getDisplayName().equalsIgnoreCase(name)) {
                    return mechanic;
                }
            }
            return null;
        }
    }

    private static final Map<UUID, ParticleEffectType> selectedEffects = new HashMap<>();
    private static final Map<UUID, MechanicType> selectedMechanics = new HashMap<>();

    public static void openEffectsMenu(Player player) {
        String rawTitle = MagicEffects.getInstance().getConfig().getString("messages.menuTitle", "&5Меню эффектов");
        String menuTitle = ChatColor.translateAlternateColorCodes('&', rawTitle);
        Inventory menu = Bukkit.createInventory(null, 54, menuTitle);

        // Добавление эффектов в меню
        ParticleEffectType[] effects = ParticleEffectType.values();
        int fallbackSlot = 10;

        for (ParticleEffectType effect : effects) {
            ConfigurationSection effectSection = MagicEffects.getInstance()
                    .getConfig()
                    .getConfigurationSection("effects." + effect.getConfigKey());

            if (effectSection == null) {
                continue;
            }

            String configuredMaterial = effectSection.getString("material");
            Material finalMaterial;
            try {
                finalMaterial = (configuredMaterial != null)
                        ? Material.valueOf(configuredMaterial)
                        : effect.getMaterial();
            } catch (IllegalArgumentException e) {
                finalMaterial = effect.getMaterial();
            }

            int slot = effectSection.getInt("slot", fallbackSlot);
            if (slot < 0 || slot >= 54) {
                slot = fallbackSlot;
            }

            ItemStack item = new ItemStack(finalMaterial);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String rarityRaw = effectSection.getString("rarity", "Обычная");
                String rarityColored = ChatColor.translateAlternateColorCodes('&', rarityRaw);

                boolean hasPerm = player.hasPermission(effect.getPermissionNode());
                String availabilityRaw = hasPerm ? "&aДоступно" : "&cНедоступно";
                String availabilityColored = ChatColor.translateAlternateColorCodes('&', availabilityRaw);

                String effectNameColored = ChatColor.translateAlternateColorCodes('&', effect.getDisplayName());

                meta.setDisplayName(effectNameColored);
                meta.setLore(Arrays.asList(
                        ChatColor.GRAY + " ",
                        ChatColor.GRAY + "Редкость: " + rarityColored,
                        ChatColor.GRAY + "Права: " + availabilityColored,
                        ChatColor.GRAY + " "
                ));
                item.setItemMeta(meta);
            }

            menu.setItem(slot, item);
            fallbackSlot++;
        }

        // Добавление механик в меню
        MechanicType[] mechanics = MechanicType.values();
        int[] mechanicSlots = {37, 38, 39, 40, 41, 42, 43};
        for (int i = 0; i < mechanicSlots.length && i < mechanics.length; i++) {
            MechanicType mech = mechanics[i];
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                // Используем getDisplayName для получения названия из конфига
                String mechanicName = mech.getDisplayName();

                meta.setDisplayName(mechanicName);
                meta.setLore(Collections.singletonList(ChatColor.GRAY + " "));
                item.setItemMeta(meta);
            }
            menu.setItem(mechanicSlots[i], item);
        }

        player.openInventory(menu);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
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

        for (ParticleEffectType effect : ParticleEffectType.values()) {
            if (name.equals(ChatColor.stripColor(effect.getDisplayName()))) {
                String effectSelectedRaw = MagicEffects.getInstance().getConfig().getString(
                        "messages.effectSelected",
                        "&aВы выбрали эффект: "
                );
                String effectSelected = ChatColor.translateAlternateColorCodes('&', effectSelectedRaw);

                player.sendMessage(effectSelected + ChatColor.translateAlternateColorCodes('&', effect.getDisplayName()));
                selectedEffects.put(player.getUniqueId(), effect);

                MechanicType savedMechanic = selectedMechanics.get(player.getUniqueId());
                if (savedMechanic != null) {
                    ParticleManager.startEffectIndefinitely(player, effect, savedMechanic);
                }
                return;
            }
        }

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
                            .replace("%mechanic%", mechanic.getDisplayName()); // Используем getDisplayName для отображения имени из конфига

                    player.sendMessage(startEffectMessage);
                    ParticleManager.startEffectIndefinitely(player, chosenEffect, mechanic);
                }
                return;
            }
        }

        String unrecognizedItemRaw = MagicEffects.getInstance().getConfig().getString(
                "messages.unrecognizedItem",
                "&cНевозможно распознать выбранный предмет!"
        );
        String unrecognizedItem = ChatColor.translateAlternateColorCodes('&', unrecognizedItemRaw);
        player.sendMessage(unrecognizedItem);
    }
}