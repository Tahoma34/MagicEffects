package org.tahoma.magiceffects;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.ChatColor;

public enum ParticleEffectType {

    STAR_TRAIL(
            "STAR_TRAIL",
            "Star Trail",
            new Particle[]{Particle.CRIT_MAGIC},
            "magiceffects.particle.startrail",
            Material.NETHER_STAR
    ),
    FLAME_RING(
            "FLAME_RING",
            "Flame Ring",
            new Particle[]{Particle.FLAME},
            "magiceffects.particle.flamering",
            Material.BLAZE_POWDER
    ),
    RAINBOW_CLOUD(
            "RAINBOW_CLOUD",
            "Rainbow Cloud",
            new Particle[]{Particle.SPELL_MOB_AMBIENT},
            "magiceffects.particle.rainbowcloud",
            Material.CYAN_DYE
    ),
    HEART_PATH(
            "HEART_PATH",
            "Heart Path",
            new Particle[]{Particle.HEART},
            "magiceffects.particle.heartpath",
            Material.RED_DYE
    ),
    MAGIC_SPIRAL(
            "MAGIC_SPIRAL",
            "Magic Spiral",
            new Particle[]{Particle.ENCHANTMENT_TABLE},
            "magiceffects.particle.magicspiral",
            Material.ENCHANTED_BOOK
    ),
    BUBBLE_BURST(
            "BUBBLE_BURST",
            "Bubble Burst",
            new Particle[]{Particle.WATER_BUBBLE},
            "magiceffects.particle.bubbleburst",
            Material.PRISMARINE_CRYSTALS
    ),
    GREEN_SPARK(
            "GREEN_SPARK",
            "Green Spark",
            new Particle[]{Particle.VILLAGER_HAPPY},
            "magiceffects.particle.greenspark",
            Material.EMERALD
    ),
    SNOWFLAKE_TRAIL(
            "SNOWFLAKE_TRAIL",
            "Snowflake Trail",
            new Particle[]{Particle.SNOWBALL},
            "magiceffects.particle.snowflaketrail",
            Material.SNOWBALL
    ),
    ELECTRIC_ZAP(
            "ELECTRIC_ZAP",
            "Electric Zap",
            new Particle[]{Particle.SPELL_WITCH},
            "magiceffects.particle.electriczap",
            Material.ENDER_PEARL
    ),
    ASH_AURA(
            "ASH_AURA",
            "Ash Aura",
            new Particle[]{Particle.ASH},
            "magiceffects.particle.ashaura",
            Material.GUNPOWDER
    ),
    SPIRAL_STEPS(
            "SPIRAL_STEPS",
            "Spiral Steps",
            new Particle[]{Particle.FLAME, Particle.SMOKE_NORMAL},
            "magiceffects.particle.spiralsteps",
            Material.MAGMA_CREAM
    ),
    EMBER_PULSE(
            "EMBER_PULSE",
            "Ember Pulse",
            new Particle[]{Particle.SOUL, Particle.REDSTONE},
            new DustOptions[]{
                    null,
                    new DustOptions(Color.ORANGE, 1.0F)
            },
            "magiceffects.particle.emberpulse",
            Material.COAL
    ),
    BLOOM_TRAIL(
            "BLOOM_TRAIL",
            "Bloom Trail",
            new Particle[]{Particle.VILLAGER_HAPPY, Particle.SPELL_MOB},
            "magiceffects.particle.bloomtrail",
            Material.POPPY
    ),
    PHOENIX_FLARE(
            "PHOENIX_FLARE",
            "Phoenix Flare",
            new Particle[]{Particle.FLAME, Particle.ASH},
            "magiceffects.particle.phoenixflare",
            Material.FIRE_CHARGE
    );

    private final String configKey;
    private final String fallbackName;
    private final Particle[] particles;
    private final DustOptions[] dustOptions;
    private final String permissionNode;
    private final Material material;

    ParticleEffectType(
            String configKey,
            String fallbackName,
            Particle[] particles,
            String permissionNode,
            Material material
    ) {
        this(configKey, fallbackName, particles, null, permissionNode, material);
    }

    ParticleEffectType(
            String configKey,
            String fallbackName,
            Particle[] particles,
            DustOptions[] dustOptions,
            String permissionNode,
            Material material
    ) {
        this.configKey = configKey;
        this.fallbackName = fallbackName;
        this.particles = particles;
        this.dustOptions = dustOptions;
        this.permissionNode = permissionNode;
        this.material = material;
    }

    public String getDisplayName() {
        String fromConfig = MagicEffects.getInstance().getConfig()
                .getString("names." + configKey, fallbackName);
        return ChatColor.translateAlternateColorCodes('&', fromConfig);
    }

    public Particle[] getParticles() {
        return particles;
    }

    public DustOptions[] getDustOptions() {
        return dustOptions;
    }

    public String getPermissionNode() {
        return permissionNode;
    }

    public Material getMaterial() {
        return material;
    }

    public String getConfigKey() {
        return configKey;
    }
}
