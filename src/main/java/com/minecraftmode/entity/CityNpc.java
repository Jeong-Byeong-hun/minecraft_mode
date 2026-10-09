package com.minecraftmode.entity;

import com.minecraftmode.bounty.Bounties;
import com.minecraftmode.city.StarterKit;
import com.minecraftmode.enhance.EnhanceMenu;
import com.minecraftmode.loot.UpgradeMenu;
import com.minecraftmode.market.AuctionService;
import com.minecraftmode.network.OpenBountyPayload;
import com.minecraftmode.network.OpenDungeonPayload;
import com.minecraftmode.network.OpenGuidePayload;
import com.minecraftmode.network.OpenRaidPayload;
import com.minecraftmode.progress.ResetCycle;
import com.minecraftmode.raid.RaidAffix;
import com.minecraftmode.raid.RaidRecordsData;
import com.minecraftmode.registry.ModItems;
import com.minecraftmode.story.Story;
import java.util.List;
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
 * City service NPCs: the blacksmith (gear evolution with Evolution Ether), the raid marshal (party
 * raids), the guild clerk (bounties and the merit shop), the broker (the market) and the enhancer
 * (gear enhancement). Like the class trainers they stand still, cannot be hurt and are kept at their posts by
 * {@code CityServices}.
 */
public class CityNpc extends PathfinderMob {
	public enum Role {
		BLACKSMITH("blacksmith", 0xE07B26, "Master Smith Volund", "명장 볼룬드",
			"Bring me gear and enough Evolution Ether, and I will forge it into something greater.",
			"장비와 진화의 에테르를 충분히 가져오게. 더 강한 물건으로 벼려 주지."),
		RAID_MARSHAL("raid_marshal", 0xC0263A, "Raid Marshal Aldric", "토벌 사령관 알드릭",
			"Six great monsters threaten the realm. Gather a party, and I will send you to face them.",
			"여섯 마리의 거대한 마물이 왕국을 위협하고 있다. 파티를 꾸려 오면 그들과 싸울 곳으로 보내 주지."),
		BOUNTY_CLERK("bounty_clerk", 0x3A6ED8, "Guild Clerk Lina", "길드 접수원 리나",
			"Welcome to the Adventurers' Guild! New bounties go up every day, and a big one every three days.",
			"모험가 길드에 어서 오세요! 의뢰는 매일, 큰 의뢰는 사흘마다 새로 붙어요."),
		BROKER("broker", 0xB04AA0, "Broker Morgan", "중개인 모건",
			"Buy what others found and sell what you do not need. One percent to list, five percent when it sells.",
			"남이 찾은 물건은 사고, 필요 없는 물건은 파시오. 등록 수수료 1%, 팔리면 5%요."),
		ENHANCER("enhancer", 0x4AC8E0, "Artisan Brokk", "강화 장인 브로크",
			"Stones, essence and nerve. Past +10 a failure costs a level, unless you bring a protection scroll.",
			"강화석, 정수, 그리고 배짱. +10을 넘기면 실패할 때 단계가 깎이네. 보호 주문서가 있으면 막을 수 있지."),
		DUNGEON_WARDEN("dungeon_warden", 0x5AA87A, "Dungeon Warden Kael", "던전 관리인 카엘",
			"Two to four of you, and a keystone if you are brave. Beat the clock and the key grows stronger.",
			"둘에서 넷이면 충분하네. 용감하다면 쐐기돌을 가져오게. 시간 안에 돌파하면 쐐기돌이 더 강해지지."),
		HERALD("herald", 0xD84050, "Royal Herald Elric", "왕실 전령 엘릭",
			"Hear ye! The realm has need of heroes. Come, I have news of the war against the dark.",
			"들으시오! 왕국에 영웅이 필요하오. 이리 오시오, 어둠과의 전쟁 소식을 전하겠소."),
		GUIDE("guide", 0x4AA8E8, "Guide Nella", "안내원 넬라",
			"New to Stormhold? Ask me anything: pick a topic and I will explain.",
			"스톰홀드는 처음이세요? 궁금한 게 있으면 뭐든 물어보세요. 주제를 고르면 알려 드릴게요."),
		QUARTERMASTER("quartermaster", 0xA88A5A, "Quartermaster Bram", "보급관 브람",
			"Every adventurer gets one set of iron gear from me. Do not lose it out there!",
			"모험가라면 누구나 철 장비 한 벌은 내게서 받아 가지. 밖에서 잃어버리지 말게!");

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
		this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(switch (role) {
			case BLACKSMITH -> Items.IRON_AXE;
			case RAID_MARSHAL -> Items.NETHERITE_SWORD;
			case BOUNTY_CLERK -> Items.WRITABLE_BOOK;
			case BROKER -> ModItems.GOLD_COIN;
			case ENHANCER -> Items.MACE;
			case DUNGEON_WARDEN -> Items.TRIAL_KEY;
			case HERALD -> Items.GOAT_HORN;
			case GUIDE -> Items.BOOK;
			case QUARTERMASTER -> Items.IRON_SWORD;
		}));
	}

	/** The raid screen data: this cycle, its modifiers and the fastest clears. */
	public static OpenRaidPayload raidScreen(final ServerPlayer player, final int entityId) {
		long cycle = ResetCycle.cycle(player.level());
		List<String> affixes = RaidAffix.forCycle(cycle).stream().map(RaidAffix::id).toList();
		return new OpenRaidPayload(entityId, cycle, ResetCycle.ticksToNextCycle(player.level()), affixes,
			RaidRecordsData.get(player.level().getServer()).snapshot(5));
	}

	public static OpenBountyPayload bountyBoard(final ServerPlayer player, final int entityId) {
		Bounties.ensure(player);
		return new OpenBountyPayload(entityId, ResetCycle.ticksToNextDay(player.level()), ResetCycle.ticksToNextCycle(player.level()));
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
						ServerPlayNetworking.send(serverPlayer, raidScreen(serverPlayer, this.getId()));
					}
				}
				case BOUNTY_CLERK -> {
					if (ServerPlayNetworking.canSend(serverPlayer, OpenBountyPayload.TYPE)) {
						ServerPlayNetworking.send(serverPlayer, bountyBoard(serverPlayer, this.getId()));
					}
				}
				case BROKER -> AuctionService.send(serverPlayer, this.getId());
				case ENHANCER -> serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, p) -> new EnhanceMenu(id, inventory, this),
					Component.translatable("container.minecraft_mode.enhance")));
				case DUNGEON_WARDEN -> {
					if (ServerPlayNetworking.canSend(serverPlayer, OpenDungeonPayload.TYPE)) {
						ServerPlayNetworking.send(serverPlayer, new OpenDungeonPayload(this.getId(), ResetCycle.cycle(serverPlayer.level())));
					}
				}
				case HERALD -> Story.talk(serverPlayer, this);
				case GUIDE -> {
					if (ServerPlayNetworking.canSend(serverPlayer, OpenGuidePayload.TYPE)) {
						ServerPlayNetworking.send(serverPlayer, new OpenGuidePayload(this.getId()));
					}
				}
				case QUARTERMASTER -> StarterKit.give(serverPlayer);
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
