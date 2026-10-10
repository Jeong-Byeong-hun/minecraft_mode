package com.minecraftmode.entity;

import com.minecraftmode.city.DummyMeter;
import com.minecraftmode.job.skill.CombatHooks;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;

/**
 * A straw training dummy in the warrior arena to measure damage on: every skill, shot and summon treats it as an enemy, it never
 * dies or moves, gives nothing, and reports each attacker's damage per second ({@link DummyMeter}). The boss kind counts as a boss
 * ({@link EliteMob}), so boss-damage bonuses apply on it. An armor stand body (pumpkin head, dyed tunic) keeps it light.
 */
public class TrainingDummy extends ArmorStand implements Enemy {
	public TrainingDummy(final EntityType<? extends TrainingDummy> type, final Level level) {
		super(type, level);
		this.setShowArms(true);
		this.setNoBasePlate(false);
		this.setNoGravity(false);
		this.dress();
	}

	public boolean isBossKind() {
		return false;
	}

	/** Pumpkin head and a straw-coloured tunic (the boss kind wears gold and red). */
	protected void dress() {
		this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CARVED_PUMPKIN));
		ItemStack tunic = new ItemStack(Items.LEATHER_CHESTPLATE);
		tunic.set(DataComponents.DYED_COLOR, new DyedItemColor(0xC9A35A));
		this.setItemSlot(EquipmentSlot.CHEST, tunic);
		this.setCustomName(Component.translatable("entity.minecraft_mode.training_dummy").withStyle(ChatFormatting.YELLOW));
		this.setCustomNameVisible(true);
	}

	@Override
	public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
		if (this.isRemoved()) {
			return false;
		}
		// /kill and the void still remove it
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			this.discard();
			return false;
		}
		Entity attacker = source.getEntity();
		ServerPlayer player = attacker instanceof ServerPlayer p ? p : CombatHooks.summonOwner(level, attacker);
		if (player == null) {
			return false;
		}
		// the same adjustments a monster's hit gets (critical hits, skill and boss bonuses, marks), then nothing is taken off
		float dealt = CombatHooks.modifyIncoming(this, source, damage);
		if (dealt <= 0.0F) {
			return false;
		}
		boolean crit = CombatHooks.lastHitWasCrit();
		ServerLivingEntityEvents.AFTER_DAMAGE.invoker().afterDamage(this, source, damage, dealt, false);
		DummyMeter.record(player, dealt, crit, level.getGameTime());
		level.broadcastEntityEvent(this, (byte)32);
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ARMOR_STAND_HIT, SoundSource.NEUTRAL, 0.4F, 1.0F);
		return true;
	}

	@Override
	public boolean skipAttackInteraction(final Entity source) {
		return false;
	}

	@Override
	public void readAdditionalSaveData(final ValueInput input) {
		super.readAdditionalSaveData(input);
		this.dress();
	}

	/** The boss kind: boss-damage bonuses (gear, talents, collections) count on it. */
	public static class Boss extends TrainingDummy implements EliteMob {
		public Boss(final EntityType<? extends Boss> type, final Level level) {
			super(type, level);
		}

		@Override
		public boolean isBossKind() {
			return true;
		}

		@Override
		protected void dress() {
			this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
			ItemStack tunic = new ItemStack(Items.LEATHER_CHESTPLATE);
			tunic.set(DataComponents.DYED_COLOR, new DyedItemColor(0xA02020));
			this.setItemSlot(EquipmentSlot.CHEST, tunic);
			this.setCustomName(Component.translatable("entity.minecraft_mode.training_dummy_boss").withStyle(ChatFormatting.GOLD));
			this.setCustomNameVisible(true);
		}
	}
}
