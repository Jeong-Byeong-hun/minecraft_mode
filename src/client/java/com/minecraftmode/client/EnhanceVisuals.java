package com.minecraftmode.client;

import com.minecraftmode.enhance.Enhancement;
import com.minecraftmode.job.gear.ClassArmor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.ExtractItemDecorationsCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * How enhanced gear shows ({@link Enhancement#glow}): from +10 the item glints (the items themselves) and gets a coloured frame in
 * every slot (blue +10, purple +13, gold +15, red-gold and a ✦ count once awakened); a held weapon gives off particles by the same
 * steps, and a full set of class armor at +10 or more circles its wearer's feet with the set's lowest step. Particles follow
 * {@link ClientSettings#enhanceEffects} ({@code /enhanceeffects}); frames always show.
 */
public final class EnhanceVisuals {
	/** Frame colours by glow step 1..4. */
	private static final int[] FRAME = {0xB04A8CFF, 0xC0B060FF, 0xD0FFC93C, 0xE0FF6A3C};
	private static final int[] DUST = {0x4A8CFF, 0xB060FF, 0xFFC93C, 0xFF6A3C};
	private static final double RANGE = 32.0;
	private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

	public static void init() {
		ExtractItemDecorationsCallback.EVENT.register((g, font, stack, x, y) -> {
			int glow = Enhancement.glow(stack);
			if (glow <= 0) {
				return;
			}
			g.outline(x - 1, y - 1, 18, 18, FRAME[glow - 1]);
			int awaken = Enhancement.of(stack).awaken();
			if (awaken > 0) {
				g.text(font, "✦" + awaken, x, y, 0xFFFFB347, true);
			}
		});
		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
			if (!ClientSettings.enhanceEffects || minecraft.level == null || minecraft.player == null || minecraft.isPaused()) {
				return;
			}
			for (Player player : minecraft.level.players()) {
				if (player.isInvisible() || player.isSpectator() || player.distanceToSqr(minecraft.player) > RANGE * RANGE) {
					continue;
				}
				boolean firstPerson = player == minecraft.player && minecraft.options.getCameraType().isFirstPerson();
				weapon(minecraft.level, player, Enhancement.glow(player.getMainHandItem()), firstPerson);
				armor(minecraft.level, player, armorGlow(player));
			}
		});
	}

	/** The lowest glow of the worn class armor, or 0 unless all four pieces are class armor at +10 or more. */
	static int armorGlow(final Player player) {
		int lowest = Integer.MAX_VALUE;
		for (EquipmentSlot slot : ARMOR) {
			ItemStack stack = player.getItemBySlot(slot);
			int glow = ClassArmor.def(stack) == null ? 0 : Enhancement.glow(stack);
			lowest = Math.min(lowest, glow);
		}
		return lowest == Integer.MAX_VALUE ? 0 : lowest;
	}

	/** Particles around the weapon hand: runes at +10, sparks at +13, golden motes at +15, embers once awakened. */
	private static void weapon(final ClientLevel level, final Player player, final int glow, final boolean firstPerson) {
		if (glow <= 0) {
			return;
		}
		int every = switch (glow) {
			case 1 -> 8;
			case 2 -> 5;
			default -> 3;
		};
		if ((player.tickCount + player.getId()) % every != 0) {
			return;
		}
		RandomSource random = level.getRandom();
		// beside the right hand, a little ahead of the body; in first person further out so it does not cover the view
		float yaw = player.yBodyRot * Mth.DEG_TO_RAD;
		double side = firstPerson ? 0.9 : 0.45;
		double ahead = firstPerson ? 0.9 : 0.3;
		Vec3 hand = player.position().add(-Mth.sin(yaw) * ahead - Mth.cos(yaw) * side, firstPerson ? 1.0 : 0.85, Mth.cos(yaw) * ahead - Mth.sin(yaw) * side);
		double x = hand.x + (random.nextDouble() - 0.5) * 0.4;
		double y = hand.y + random.nextDouble() * 0.6;
		double z = hand.z + (random.nextDouble() - 0.5) * 0.4;
		ParticleOptions particle = switch (glow) {
			case 1 -> ParticleTypes.ENCHANT;
			case 2 -> ParticleTypes.ELECTRIC_SPARK;
			case 3 -> new DustParticleOptions(DUST[2], 0.8F);
			default -> ParticleTypes.SMALL_FLAME;
		};
		level.addParticle(particle, x, y, z, 0.0, 0.02, 0.0);
		if (glow >= 3) {
			level.addParticle(glow == 3 ? ParticleTypes.END_ROD : new DustParticleOptions(DUST[3], 0.9F), x, y, z, 0.0, 0.03, 0.0);
		}
	}

	/** A turning ring of the set's colour around the feet. */
	private static void armor(final ClientLevel level, final Player player, final int glow) {
		if (glow <= 0 || (player.tickCount + player.getId()) % 4 != 0) {
			return;
		}
		int points = 1 + glow;
		double turn = player.tickCount * 0.12;
		for (int i = 0; i < points; i++) {
			double angle = turn + i * Mth.TWO_PI / points;
			double x = player.getX() + Math.cos(angle) * 0.6;
			double z = player.getZ() + Math.sin(angle) * 0.6;
			level.addParticle(new DustParticleOptions(DUST[glow - 1], 0.7F), x, player.getY() + 0.1, z, 0.0, 0.01, 0.0);
		}
		if (glow == 4 && level.getRandom().nextInt(3) == 0) {
			level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 0.1, player.getZ(), 0.0, 0.02, 0.0);
		}
	}

	private EnhanceVisuals() {
	}
}
