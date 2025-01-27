package org.tahoma.magiceffects;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.Particle.DustOptions;
import org.bukkit.ChatColor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ParticleManager {

    private static final Map<UUID, BukkitTask> activeTasks = new HashMap<>();
    private static final Map<UUID, Double> arcAngles = new HashMap<>();
    private static final double ANGLE_INCREMENT = Math.toRadians(60);
    private static final FileConfiguration CONFIG = MagicEffects.getInstance().getConfig();
    private static final long UPDATE_INTERVAL = CONFIG.getLong("generalSettings.updateIntervalTicks", 20L);

    private static final String MSG_EFFECT_NOT_CONFIGURED = ChatColor.translateAlternateColorCodes(
            '&',
            CONFIG.getString("messages.effectNotConfigured", "&cЭтот эффект не настроен в config.yml.")
    );
    private static final String MSG_EFFECT_NOT_FOUND_LOG = CONFIG.getString(
            "messages.effectNotFoundLog",
            "[MagicEffects] Эффект %effect-type% не найден в config.yml!"
    );
    private static final String MSG_UNKNOWN_MECHANIC_LOG = ChatColor.translateAlternateColorCodes(
            '&',
            CONFIG.getString(
                    "messages.unknownMechanicLog",
                    "[MagicEffects] Неизвестная механика: %mechanic%"
            )
    );

    public static void startEffectIndefinitely(Player player, ParticleEffectType effectType, MenuListener.MechanicType mechanic) {
        if (activeTasks.containsKey(player.getUniqueId())) {
            activeTasks.get(player.getUniqueId()).cancel();
        }

        ConfigurationSection effectConfig = CONFIG.getConfigurationSection("effects." + effectType.getConfigKey());
        if (effectConfig == null) {
            player.sendMessage(MSG_EFFECT_NOT_CONFIGURED);
            MagicEffects.getInstance().getLogger().warning(
                    MSG_EFFECT_NOT_FOUND_LOG.replace("%effect-type%", effectType.getConfigKey())
            );
            return;
        }
        double radius = effectConfig.getDouble("radius", 0.5);
        int count = effectConfig.getInt("count", 5);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                switch (mechanic) {
                    case CIRCLE_FEET:
                        createCircleAtFeet(player, effectType, radius, count);
                        break;
                    case WAVE_UP:
                        createWaveUp(player, effectType, radius, count);
                        break;
                    case CONE_UP:
                        createConeUp(player, effectType, radius, count);
                        break;
                    case ROTATING_BEAM:
                        createRotatingBeam(player, effectType, radius, count);
                        break;
                    case TRAIL_BEHIND:
                        createTrailBehind(player, effectType, radius, count);
                        break;
                    case NIMB:
                        createNimb(player, effectType, radius, count);
                        break;
                    case ARC:
                        createArc(player, effectType, radius, count);
                        break;
                    default:
                        MagicEffects.getInstance().getLogger().warning(
                                MSG_UNKNOWN_MECHANIC_LOG.replace("%mechanic%", mechanic.getDisplayName())
                        );
                }
            }
        }.runTaskTimer(MagicEffects.getInstance(), 0L, UPDATE_INTERVAL);

        activeTasks.put(player.getUniqueId(), task);
    }

    private static final Map<UUID, Double> pulseRadius = new HashMap<>();

    private static void createCircleAtFeet(Player player, ParticleEffectType effectType, double baseRadius, int count) {
        Location center = player.getLocation().clone().add(0, 0.1, 0);
        int points = 30;
        double offset = 0.2;

        double currentRadius = pulseRadius.getOrDefault(player.getUniqueId(), baseRadius);
        double angleIncrement = 2 * Math.PI / points;

        for (int i = 0; i < points; i++) {
            double angle = angleIncrement * i;
            double dx = Math.cos(angle) * currentRadius;
            double dz = Math.sin(angle) * currentRadius;
            Location particleLocation = center.clone().add(dx, offset, dz);
            spawnParticles(effectType, particleLocation, count);
        }

        double newRadius = currentRadius + 0.3;
        if (newRadius > baseRadius * 4) {
            newRadius = baseRadius;
        }
        pulseRadius.put(player.getUniqueId(), newRadius);
    }

    private static void spawnParticlesWithDirection(ParticleEffectType effectType, Location location, int count, double xOffset, double yOffset, double zOffset, double speedMultiplier) {
        if (location.getWorld() == null) return;

        Particle[] particles = effectType.getParticles();
        DustOptions[] dusts = effectType.getDustOptions();

        for (int i = 0; i < particles.length; i++) {
            Particle particle = particles[i];
            if (particle == Particle.REDSTONE && dusts != null && dusts.length > i && dusts[i] != null) {
                location.getWorld().spawnParticle(
                        Particle.REDSTONE,
                        location,
                        count,
                        xOffset, yOffset, zOffset,
                        speedMultiplier,
                        dusts[i]
                );
            } else {
                location.getWorld().spawnParticle(
                        particle,
                        location,
                        count,
                        xOffset, yOffset, zOffset,
                        speedMultiplier
                );
            }
        }
    }

    private static final Map<UUID, Integer> spiralStepMap = new HashMap<>();

    private static void createWaveUp(Player player, ParticleEffectType effectType, double radius, int count) {
        Location center = player.getLocation().clone().add(0, 1, 0);
        int stepX = spiralStepMap.getOrDefault(player.getUniqueId(), 0);

        int particles = 25;
        int particlesPerRotation = 100;
        double spiralRadius = radius;

        for (double stepY = -60; stepY < 60; stepY += 120D / particles) {
            double dx = -(Math.cos(((stepX + stepY) / (double) particlesPerRotation) * Math.PI * 2)) * spiralRadius;
            double dy = stepY / particlesPerRotation / 2D;
            double dz = -(Math.sin(((stepX + stepY) / (double) particlesPerRotation) * Math.PI * 2)) * spiralRadius;
            Location particleLocation = center.clone().add(dx, dy, dz);
            spawnParticles(effectType, particleLocation, count);
        }

        stepX += 7;
        spiralStepMap.put(player.getUniqueId(), stepX);
    }

    private static void createConeUp(Player player, ParticleEffectType effectType, double radius, int count) {
        Location base = player.getLocation();
        for (double h = 0; h <= 2; h += 0.2) {
            double currentRadius = radius + h / 2;
            int points = 15;
            for (int i = 0; i < points; i++) {
                double angle = 2 * Math.PI * i / points;
                double x = currentRadius * Math.cos(angle);
                double z = currentRadius * Math.sin(angle);
                Location particleLocation = base.clone().add(x, h, z);
                spawnParticles(effectType, particleLocation, count);
            }
        }
    }

    public static void resetEffects() {
        for (BukkitTask task : activeTasks.values()) {
            task.cancel();
        }
        activeTasks.clear();
        arcAngles.clear();
    }

    private static final Map<UUID, Integer> stepXMap = new HashMap<>();
    private static final Map<UUID, Integer> stepYMap = new HashMap<>();
    private static final Map<UUID, Boolean> reverseMap = new HashMap<>();

    private static void createRotatingBeam(Player player, ParticleEffectType effectType, double radius, int count) {
        Location center = player.getLocation().clone().add(0, 1, 0);

        int stepX = stepXMap.getOrDefault(player.getUniqueId(), 0);
        int stepY = stepYMap.getOrDefault(player.getUniqueId(), 0);
        boolean reverse = reverseMap.getOrDefault(player.getUniqueId(), false);

        int orbs = 4;
        int maxStepX = 80;
        int maxStepY = 60;

        for (int i = 0; i < orbs; i++) {
            double dx = -(Math.cos((stepX / (double) maxStepX) * (Math.PI * 2) + (((Math.PI * 2) / orbs) * i))) * ((maxStepY - Math.abs(stepY)) / (double) maxStepY) * radius;
            double dy = (stepY / (double) maxStepY) * 1.5;
            double dz = -(Math.sin((stepX / (double) maxStepX) * (Math.PI * 2) + (((Math.PI * 2) / orbs) * i))) * ((maxStepY - Math.abs(stepY)) / (double) maxStepY) * radius;
            Location particleLocation = center.clone().add(dx, dy, dz);
            spawnParticles(effectType, particleLocation, count);
        }

        stepX += 9;
        if (stepX > maxStepX) {
            stepX = 0;
        }
        if (reverse) {
            stepY += 9;
            if (stepY > maxStepY)
                reverse = false;
        } else {
            stepY -= 9;
            if (stepY < -maxStepY)
                reverse = true;
        }

        stepXMap.put(player.getUniqueId(), stepX);
        stepYMap.put(player.getUniqueId(), stepY);
        reverseMap.put(player.getUniqueId(), reverse);
    }

    private static void createTrailBehind(Player player, ParticleEffectType effectType, double radius, int count) {
        Location behind = player.getLocation().clone().add(
                -player.getLocation().getDirection().getX() * radius,
                0.1,
                -player.getLocation().getDirection().getZ() * radius
        );
        spawnParticles(effectType, behind, count);
    }

    private static void createNimb(Player player, ParticleEffectType effectType, double radius, int count) {
        Location base = player.getLocation().clone().add(0, 2.0, 0);
        int points = 20;
        for (int i = 0; i < points; i++) {
            double angle = 2.0 * Math.PI * i / points;
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);
            Location particleLocation = base.clone().add(x, 0, z);
            spawnParticles(effectType, particleLocation, count);
        }
    }

    private static void createArc(Player player, ParticleEffectType effectType, double radius, int count) {
        double baseAngle = arcAngles.getOrDefault(player.getUniqueId(), 0.0);
        Location center = player.getLocation().clone().add(0, 0.8, 0);

        double arcRange = Math.toRadians(60);
        int arcPoints = 16;

        double startAngle1 = baseAngle - arcRange / 2;
        double endAngle1 = baseAngle + arcRange / 2;
        double step1 = (endAngle1 - startAngle1) / arcPoints;
        for (int i = 0; i <= arcPoints; i++) {
            double angle = startAngle1 + step1 * i;
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);
            Location loc = center.clone().add(x, 0, z);
            spawnParticles(effectType, loc, count);
        }

        double startAngle2 = baseAngle + Math.PI - arcRange / 2;
        double endAngle2 = baseAngle + Math.PI + arcRange / 2;
        double step2 = (endAngle2 - startAngle2) / arcPoints;
        for (int i = 0; i <= arcPoints; i++) {
            double angle = startAngle2 + step2 * i;
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);
            Location loc = center.clone().add(x, 0, z);
            spawnParticles(effectType, loc, count);
        }

        baseAngle += ANGLE_INCREMENT;
        if (baseAngle >= 2 * Math.PI) {
            baseAngle = 0.0;
        }
        arcAngles.put(player.getUniqueId(), baseAngle);
    }

    private static void spawnParticles(ParticleEffectType effectType, Location location, int count) {
        if (location.getWorld() == null) return;

        Particle[] particles = effectType.getParticles();
        DustOptions[] dusts = effectType.getDustOptions();

        for (int i = 0; i < particles.length; i++) {
            Particle particle = particles[i];
            if (particle == Particle.REDSTONE && dusts != null && dusts.length > i && dusts[i] != null) {
                location.getWorld().spawnParticle(
                        Particle.REDSTONE,
                        location,
                        count,
                        0, 0, 0, 0,
                        dusts[i]
                );
            } else {
                location.getWorld().spawnParticle(
                        particle,
                        location,
                        count,
                        0, 0, 0,
                        0
                );
            }
        }
    }
}