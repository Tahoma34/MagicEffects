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

    // Хранение активных задач (чтобы отменять старые при повторном запуске)
    private static final Map<UUID, BukkitTask> activeTasks = new HashMap<>();

    // Карта для хранения "угла" вращения дуги для каждого игрока
    private static final Map<UUID, Double> arcAngles = new HashMap<>();

    // Увеличение угла для дуги
    private static final double ANGLE_INCREMENT = Math.toRadians(60);

    // Получаем конфигурацию плагина
    private static final FileConfiguration CONFIG = MagicEffects.getInstance().getConfig();

    // Загружаем интервал обновления из конфига (в тиках)
    private static final long UPDATE_INTERVAL = CONFIG.getLong("generalSettings.updateIntervalTicks", 20L);

    private static final String MSG_EFFECT_NOT_CONFIGURED = ChatColor.translateAlternateColorCodes(
            '&',
            CONFIG.getString("messages.effectNotConfigured", "&cЭтот эффект не настроен в config.yml.")
    );
    private static final String MSG_EFFECT_NOT_FOUND_LOG = CONFIG.getString(
            "messages.effectNotFoundLog",
            "[MagicEffects] Эффект %effect-type% не найден в config.yml!"
    );
    private static final String MSG_UNKNOWN_MECHANIC_LOG = CONFIG.getString(
            "messages.unknownMechanicLog",
            "[MagicEffects] Неизвестная механика: %mechanic%"
    );

    /**
     * Запуск эффекта на неограниченное время, пока не будет вызвана отмена.
     */
    public static void startEffectIndefinitely(Player player, ParticleEffectType effectType, String mechanic) {
        if (activeTasks.containsKey(player.getUniqueId())) {
            activeTasks.get(player.getUniqueId()).cancel();
        }

        // Ищем данные для эффекта в config.yml
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

        // Используем переменную UPDATE_INTERVAL для плавности анимации
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                switch (mechanic) {
                    case "Круг у ног":
                        createCircleAtFeet(player, effectType, radius, count);
                        break;
                    case "Волна вверх":
                        createWaveUp(player, effectType, radius, count);
                        break;
                    case "Конус вверх":
                        createConeUp(player, effectType, radius, count);
                        break;
                    case "Вращающийся луч":
                        createRotatingBeam(player, effectType, radius, count);
                        break;
                    case "Шлейф позади":
                        createTrailBehind(player, effectType, radius, count);
                        break;
                    case "Нимб":
                        createNimb(player, effectType, radius, count);
                        break;
                    case "Дуга":
                        createArc(player, effectType, radius, count);
                        break;
                    default:
                        MagicEffects.getInstance().getLogger().warning(
                                MSG_UNKNOWN_MECHANIC_LOG.replace("%mechanic%", mechanic)
                        );
                }
            }
        }.runTaskTimer(MagicEffects.getInstance(), 0L, UPDATE_INTERVAL);

        activeTasks.put(player.getUniqueId(), task);
    }

    /*
     * Круг у ног игрока
     */
    private static void createCircleAtFeet(Player player, ParticleEffectType effectType, double radius, int count) {
        Location center = player.getLocation().clone().add(0, 0.1, 0);
        int points = 20;
        for (int i = 0; i < points; i++) {
            double angle = 2.0 * Math.PI * i / points;
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);
            Location particleLocation = center.clone().add(x, 0, z);
            spawnParticles(effectType, particleLocation, count);
        }
    }

    /*
     * Волна частиц, идущая вверх
     */
    private static void createWaveUp(Player player, ParticleEffectType effectType, double radius, int count) {
        Location base = player.getLocation();
        for (double y = 0; y <= 2; y += 0.2) {
            Location waveLocation = base.clone().add(0, y, 0);
            spawnParticles(effectType, waveLocation, count);
        }
    }

    /*
     * Конус частиц, уходящий вверх
     */
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

    /*
     * Сброс всех активных эффектов.
     */
    public static void resetEffects() {
        // Отменяем все активные задачи
        for (BukkitTask task : activeTasks.values()) {
            task.cancel();
        }
        activeTasks.clear();

        // Сбрасываем сохранённые дуги и любые другие параметры
        arcAngles.clear();
    }

    /*
     * Вращающийся "луч" из частиц
     */
    private static void createRotatingBeam(Player player, ParticleEffectType effectType, double radius, int count) {
        Location center = player.getLocation().clone().add(0, 1, 0);
        int points = 30;
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);
            Location particleLocation = center.clone().add(x, 0, z);
            spawnParticles(effectType, particleLocation, count);
        }
    }

    /*
     * Шлейф позади игрока
     */
    private static void createTrailBehind(Player player, ParticleEffectType effectType, double radius, int count) {
        Location behind = player.getLocation().clone().add(
                -player.getLocation().getDirection().getX() * radius,
                0.1,
                -player.getLocation().getDirection().getZ() * radius
        );
        spawnParticles(effectType, behind, count);
    }

    /*
     * Нимб над головой
     */
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

    /**
     * Создание двух вращающихся дуг вокруг игрока.
     */
    private static void createArc(Player player, ParticleEffectType effectType, double radius, int count) {
        double baseAngle = arcAngles.getOrDefault(player.getUniqueId(), 0.0);
        Location center = player.getLocation().clone().add(0, 0.8, 0);

        double arcRange = Math.toRadians(60);
        int arcPoints = 16;

        // Первая дуга
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

        // Вторая дуга (со сдвигом на π)
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

        // Увеличиваем угол на каждое обновление
        baseAngle += ANGLE_INCREMENT;
        if (baseAngle >= 2 * Math.PI) {
            baseAngle = 0.0;
        }
        arcAngles.put(player.getUniqueId(), baseAngle);
    }

    /**
     * Универсальный метод спавна частиц для конкретного типа эффекта.
     */
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