package com.minecraftmode.job.gear;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.enhance.Enhancement;
import com.minecraftmode.job.weapon.JobWeaponItem;
import com.minecraftmode.registry.ModDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * A piece of class armor. Only its class (at the right tier and level) can wear it; options are
 * rolled the first time it sits in a player's inventory. Unbreakable and not enchantable: armor is
 * improved with engravings (4 lines) instead.
 */
public class GearArmorItem extends Item {
	private final ArmorPieceDef def;

	public GearArmorItem(final Properties properties, final ArmorPieceDef def) {
		super(properties);
		this.def = def;
	}

	public ArmorPieceDef def() {
		return this.def;
	}

	static Properties properties(final ArmorPieceDef def, final ResourceKey<EquipmentAsset> asset) {
		GearSlot slot = def.slot();
		EquipmentSlotGroup group = EquipmentSlotGroup.bySlot(slot.equipmentSlot());
		Identifier modifierId = MinecraftMode.id("gear." + slot.suffix());
		ItemAttributeModifiers.Builder attributes = ItemAttributeModifiers.builder()
			.add(Attributes.ARMOR, new AttributeModifier(modifierId, def.armor(), AttributeModifier.Operation.ADD_VALUE), group);
		if (def.toughness() > 0.0F) {
			attributes.add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(modifierId, def.toughness(), AttributeModifier.Operation.ADD_VALUE), group);
		}
		int tier = def.tier();
		return new Properties()
			.stacksTo(1)
			.rarity(tier >= 3 ? Rarity.EPIC : tier == 2 ? Rarity.RARE : Rarity.UNCOMMON)
			.attributes(attributes.build())
			.component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
			// armor and toughness are a line of the class tooltip (GearTooltips); vanilla's "When on ...:" block and unbreakable line only made it longer
			.component(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.ATTRIBUTE_MODIFIERS, true).withHidden(DataComponents.UNBREAKABLE, true))
			.component(DataComponents.EQUIPPABLE, Equippable.builder(slot.equipmentSlot()).setEquipSound(def.set().style().sound()).setAsset(asset).build());
	}

	@Override
	public Component getName(final ItemStack itemStack) {
		return Enhancement.decorate(Component.translatable(this.getDescriptionId()).withColor(JobWeaponItem.tierColor(this.def.tier())), itemStack);
	}

	/** Gear enhanced to +{@value Enhancement#GLOW_FROM} or more has the enchantment glint (the client adds slot frames and particles). */
	@Override
	public boolean isFoil(final ItemStack itemStack) {
		return super.isFoil(itemStack) || Enhancement.glow(itemStack) > 0;
	}

	@Override
	public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
		if (!GearRules.canUse(player, this.def.job(), this.def.level())) {
			if (player instanceof ServerPlayer serverPlayer) {
				serverPlayer.sendOverlayMessage(GearRules.refusal(this.def.job(), this.def.level()));
			}
			return InteractionResult.FAIL;
		}
		return super.use(level, player, hand);
	}

	@Override
	public void inventoryTick(final ItemStack itemStack, final ServerLevel level, final Entity owner, final @Nullable EquipmentSlot slot) {
		super.inventoryTick(itemStack, level, owner, slot);
		if (!itemStack.has(ModDataComponents.GEAR_ROLLS)) {
			itemStack.set(ModDataComponents.GEAR_ROLLS, ArmorOptions.roll(this.def, level.getRandom()));
		}
	}
}
