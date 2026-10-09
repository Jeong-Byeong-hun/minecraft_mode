package com.minecraftmode.entity;

import com.minecraftmode.loot.UpgradeMenu;
import com.minecraftmode.network.OpenRaidPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * City service NPCs: the blacksmith (gear evolution with Evolution Ether) and the raid marshal (party
 * raids). Like the class trainers they stand still, cannot be hurt and are kept at their posts by
 * {@code CityServices}.
 */
public class CityNpc extends PathfinderMob {
	public enum Role {
		BLACKSMITH("blacksmith", 0xE07B26, "Master Smith Volund", "명장 볼룬드",
			"Bring me gear and enough Evolution Ether, and I will forge it into something greater.",
			"장비와 진화의 에테르를 충분히 가져오게. 더 강한 물건으로 벼려 주지."),
		RAID_MARSHAL("raid_marshal", 0xC0263A, "Raid Marshal Aldric", "토벌 사령관 알드릭",
			"Six great monsters threaten the realm. Gather a party, and I will send you to face them.",
			"여섯 마리의 거대한 마물이 왕국을 위협하고 있다. 파티를 꾸려 오면 그들과 싸울 곳으로 보내 주지.");

		private final String id;
		private final int color;
		public final String en;
		public final String ko;
		public final String greetingEn;
		public final String greetingKo;

		Role(final String id, final int color, final String en, final String ko, final String greetingEn, final String greetingKo) {
			this.id = id;
			this.color = color;
			this.en = en;
			this.ko = ko;
			this.greetingEn = greetingEn;
			this.greetingKo = greetingKo;
		}

		public String id() {
			return this.id;
		}

		public int color() {
			return this.color;
		}

		public String nameKey() {
			return "entity.minecraft_mode.city_npc." + this.id;
		}

		public String greetingKey() {
			return this.nameKey() + ".greeting";
		}

		public static Role byId(final String id) {
			for (Role role : values()) {
				if (role.id.equals(id)) {
					return role;
				}
			}
			return BLACKSMITH;
		}
	}

	private static final EntityDataAccessor<Integer> DATA_ROLE = SynchedEntityData.defineId(CityNpc.class, EntityDataSerializers.INT);

	public CityNpc(final EntityType<? extends CityNpc> type, final Level level) {
		super(type, level);
		this.setPermanentlyInvulnerable(true);
		this.setPersistenceRequired();
		this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
	}

	@Override
	protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
		super.defineSynchedData(entityData);
		entityData.define(DATA_ROLE, Role.BLACKSMITH.ordinal());
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 10.0F, 1.0F));
		this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
	}

	public Role role() {
		int index = this.entityData.get(DATA_ROLE);
		return index >= 0 && index < Role.values().length ? Role.values()[index] : Role.BLACKSMITH;
	}

	public void setRole(final Role role) {
		this.entityData.set(DATA_ROLE, role.ordinal());
		this.setCustomName(Component.translatable(role.nameKey()).withColor(role.color()));
		this.setCustomNameVisible(true);
		this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(role == Role.BLACKSMITH ? Items.IRON_AXE : Items.NETHERITE_SWORD));
	}

	@Override
	protected InteractionResult mobInteract(final Player player, final InteractionHand hand) {
		if (player instanceof ServerPlayer serverPlayer) {
			Role role = this.role();
			serverPlayer.sendSystemMessage(Component.translatable(role.nameKey()).withColor(role.color())
				.append(Component.literal(": ").withStyle(net.minecraft.ChatFormatting.DARK_GRAY))
				.append(Component.translatable(role.greetingKey()).withStyle(net.minecraft.ChatFormatting.GRAY)));
			switch (role) {
				case BLACKSMITH -> serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, p) -> new UpgradeMenu(id, inventory, this),
					Component.translatable("container.minecraft_mode.upgrade")));
				case RAID_MARSHAL -> {
					if (ServerPlayNetworking.canSend(serverPlayer, OpenRaidPayload.TYPE)) {
						ServerPlayNetworking.send(serverPlayer, new OpenRaidPayload(this.getId()));
					}
				}
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.level().isClientSide() && !this.hasCustomName()) {
			this.setRole(this.role());
		}
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(final double distSqr) {
		return false;
	}

	@Override
	public boolean canBeLeashed() {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(final ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putString("Role", this.role().id());
	}

	@Override
	protected void readAdditionalSaveData(final ValueInput input) {
		super.readAdditionalSaveData(input);
		this.setRole(Role.byId(input.getStringOr("Role", "blacksmith")));
	}
}
