package com.minecraftmode.craft;

import com.minecraftmode.economy.Essence;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.registry.ModMenus;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * A profession station: the recipes of one profession made straight from the inventory. Button id = recipe index × 2, plus one
 * to make five at once. Every craft teaches profession experience and at higher levels sometimes makes twice the output.
 */
public class CraftMenu extends AbstractContainerMenu {
	public static final int WIDTH = 256;
	public static final int HEIGHT = 228;
	public static final int INVENTORY_X = 48;
	public static final int INVENTORY_Y = 146;

	private final Profession profession;
	private final @Nullable BlockPos station;

	public CraftMenu(final MenuType<CraftMenu> type, final int containerId, final Inventory inventory, final Profession profession, final @Nullable BlockPos station) {
		super(type, containerId);
		this.profession = profession;
		this.station = station;
		this.addStandardInventorySlots(inventory, INVENTORY_X, INVENTORY_Y);
	}

	public static MenuType<CraftMenu> type(final Profession profession) {
		return switch (profession) {
			case COOKING -> ModMenus.CRAFT_COOKING;
			case ALCHEMY -> ModMenus.CRAFT_ALCHEMY;
			case SMITHING -> ModMenus.CRAFT_SMITHING;
		};
	}

	public Profession profession() {
		return this.profession;
	}

	public List<CraftRecipes.Recipe> recipes() {
		return CraftRecipes.of(this.profession);
	}

	/** How many times {@code player} could make {@code recipe} with what they carry. */
	public static int makeable(final Player player, final CraftRecipes.Recipe recipe) {
		int times = Integer.MAX_VALUE;
		for (CraftRecipes.Ingredient in : recipe.ingredients()) {
			times = Math.min(times, held(player, recipe, in) / in.count());
		}
		return times;
	}

	/**
	 * How much of {@code in} {@code player} can put in: essence ingredients take either kind ({@link Essence}), except in
	 * recipes that make essence, where condensed essence would pay for the essence it is made from.
	 */
	public static int held(final Player player, final CraftRecipes.Recipe recipe, final CraftRecipes.Ingredient in) {
		return eitherEssence(player, recipe) ? Essence.held(player.getInventory(), in.item()) : JobProgression.count(player.getInventory(), in.item());
	}

	private static boolean eitherEssence(final Player player, final CraftRecipes.Recipe recipe) {
		return !Essence.is(recipe.preview(player).getItem());
	}

	@Override
	public boolean clickMenuButton(final Player player, final int buttonId) {
		List<CraftRecipes.Recipe> recipes = this.recipes();
		int index = buttonId / 2;
		if (buttonId < 0 || index >= recipes.size()) {
			return false;
		}
		CraftRecipes.Recipe recipe = recipes.get(index);
		int level = this.profession.level(player);
		if (level < recipe.level()) {
			return false;
		}
		int times = Math.min(buttonId % 2 == 0 ? 1 : 5, makeable(player, recipe));
		if (times <= 0) {
			return false;
		}
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return true;
		}
		int made = 0;
		int doubled = 0;
		boolean eitherEssence = eitherEssence(player, recipe);
		for (int i = 0; i < times; i++) {
			for (CraftRecipes.Ingredient in : recipe.ingredients()) {
				if (eitherEssence) {
					Essence.take(player.getInventory(), in.item(), in.count());
				} else {
					JobProgression.removeItems(player.getInventory(), in.item(), in.count());
				}
			}
			ItemStack out = recipe.output().apply(player);
			if (player.getRandom().nextInt(100) < this.profession.doubleChance(player)) {
				out.grow(out.getCount());
				doubled++;
			}
			made += out.getCount();
			player.getInventory().placeItemBackInInventory(out, Prediction.SERVER_ONLY);
			this.profession.addExp(serverPlayer, recipe.expFor(this.profession.level(player)));
		}
		serverPlayer.sendOverlayMessage(Component.translatable(doubled > 0 ? "message.minecraft_mode.profession.crafted_double" : "message.minecraft_mode.profession.crafted",
			recipe.output().apply(player).getHoverName(), made).withStyle(ChatFormatting.GREEN));
		serverPlayer.level().playSound(null, player.getX(), player.getY(), player.getZ(), switch (this.profession) {
			case COOKING -> SoundEvents.SMOKER_SMOKE;
			case ALCHEMY -> SoundEvents.BREWING_STAND_BREW;
			case SMITHING -> SoundEvents.ANVIL_USE;
		}, SoundSource.BLOCKS, 0.7F, 1.1F);
		this.broadcastChanges();
		return true;
	}

	@Override
	public ItemStack quickMoveStack(final Player player, final int slotIndex) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(final Player player) {
		return this.station == null || player.distanceToSqr(this.station.getX() + 0.5, this.station.getY() + 0.5, this.station.getZ() + 0.5) < 64.0;
	}
}
