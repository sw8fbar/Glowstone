package net.glowstone.entity.projectile;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import net.glowstone.EventFactory;
import net.glowstone.net.message.play.entity.SpawnObjectMessage;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.SplashPotion;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;

public class GlowSplashPotion extends GlowProjectile implements SplashPotion {
    private static final double MAX_VERTICAL_DISTANCE = 2.125;
    private static final double MAX_DISTANCE_SQUARED = 16.0;
    @Getter
    @Setter
    private ItemStack item;

    public GlowSplashPotion(Location location) {
        super(location);
    }

    @Override
    public void collide(Block block) {
        applyEffects();
    }

    @Override
    public void collide(LivingEntity entity) {
        applyEffects();
    }

    private void applyEffects() {
        Collection<PotionEffect> effects = getEffects();
        if (effects.isEmpty()) {
            return;
        }
        double y = location.getY();
        Map<LivingEntity, Double> affectedIntensities = new HashMap<>();
        world.getLivingEntities().stream().forEach(entity -> {
            Location entityLoc = entity.getLocation();
            double verticalOffset = entityLoc.getY() - y;
            if (verticalOffset > MAX_VERTICAL_DISTANCE
                    || verticalOffset < -MAX_VERTICAL_DISTANCE) {
                return;
            }
            double distFractionSquared = entityLoc.distanceSquared(location) / MAX_DISTANCE_SQUARED;
            if (distFractionSquared < 1) {
                // intensity is 1 - (distance / max distance)
                affectedIntensities.put(entity, 1 - Math.sqrt(distFractionSquared));
            }
        });
        PotionSplashEvent event = EventFactory.getInstance().callEvent(
                new PotionSplashEvent(this, affectedIntensities));
        if (!event.isCancelled()) {
            for (LivingEntity entity : event.getAffectedEntities()) {
                double intensity = event.getIntensity(entity);
                for (PotionEffect effect : getEffects()) {
                    // TODO: Apply intensity to Healing and Harming
                    entity.addPotionEffect(intensity >= 1.0 ? effect : new PotionEffect(
                            // FIXME: PotionEffect needs a builder class for situations like this
                            effect.getType(),
                            (int) (effect.getDuration() * intensity),
                            effect.getAmplifier(),
                            effect.isAmbient(),
                            effect.hasParticles(),
                            effect.getColor()));
                }
            }
        }
        remove();
    }

    @Override
    protected int getObjectId() {
        return SpawnObjectMessage.SPLASH_POTION;
    }

    @Override
    public Collection<PotionEffect> getEffects() {
        if (item == null) {
            return Collections.emptyList();
        }
        ItemMeta meta = item.getItemMeta();
        return meta instanceof PotionMeta ? ((PotionMeta) meta).getCustomEffects()
                : Collections.emptyList();
    }
}
