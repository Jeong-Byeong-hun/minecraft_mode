package com.minecraftmode.consumable;

import com.minecraftmode.city.CityZone;
import com.minecraftmode.job.skill.SkillScheduler;
import com.minecraftmode.raid.RaidDimension;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Return Scroll: after reading it for 5 seconds without moving, the player is taken to the capital's
 * plaza (or the world spawn without a city). Not in raids. Sold at the general store.
 */
public class ReturnScrollItem extends Item {
	private static final int CHANNEL = 100;

	public ReturnScrollItem(final Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
		if (!(player instanceof ServerPlayer sp) || !(level instanceof ServerLevel)) {
			return InteractionResult.SUCCESS;
		}
		if (RaidDimension.is(level)) {
			sp.sendOverlayMessage(Component.translatable("message.minecraft_mode.return_scroll.raid").withStyle(ChatFormatting.RED));
			return InteractionResult.FAIL;
		}
		ItemStack stack = player.getItemInHand(hand);
		Vec3 start = sp.position();
		sp.getCooldowns().addCooldown(stack, CHANNEL + 20);
		sp.sendOverlayMessage(Component.translatable("message.minecraft_mode.return_scroll.reading").withStyle(ChatFormatting.AQUA));
		boolean[] broken = {false};
		for (int t = 10; t < CHANNEL; t += 10) {
			SkillScheduler.schedule(t, () -> {
				if (broken[0] || !sp.isAlive() || sp.hasDisconnected()) {
					broken[0] = true;
					return;
				}
				if (sp.position().distanceToSqr(start) > 1.0) {
					broken[0] = true;
					sp.sendOverlayMessage(Component.translatable("message.minecraft_mode.return_scroll.moved").withStyle(ChatFormatting.RED));
					return;
				}
				sp.level().sendParticles(ParticleTypes.PORTAL, sp.getX(), sp.getY(0.5), sp.getZ(), 20, 0.4, 0.8, 0.4, 0.4);
			});
		}
		SkillScheduler.schedule(CHANNEL, () -> {
			if (broken[0] || !sp.isAlive() || sp.hasDisconnected() || sp.position().distanceToSqr(start) > 1.0) {
				return;
			}
			ItemStack held = sp.getItemInHand(hand);
			if (!held.is(this)) {
				return;
			}
			held.shrink(1);
			ServerLevel overworld = sp.level().getServer().overworld();
			BlockPos to = CityZone.isCityLevel(overworld) ? CityZone.spawn(CityZone.baseY(overworld)) : overworld.getRespawnData().pos();
			sp.teleportTo(overworld, to.getX() + 0.5, to.getY(), to.getZ() + 0.5, Set.of(), 180.0F, 0.0F, true);
			overworld.playSound(null, to, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.2F);
		});
		return InteractionResult.CONSUME;
	}
}
