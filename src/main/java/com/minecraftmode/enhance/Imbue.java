package com.minecraftmode.enhance;

import com.minecraftmode.accessory.AccessoryDef;
import com.minecraftmode.accessory.Accessories;
import com.minecraftmode.job.gear.ArmorPieceDef;
import com.minecraftmode.job.gear.ClassArmor;
import com.minecraftmode.job.gear.StatLine;
import com.minecraftmode.registry.ModDataComponents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * An imbued line (0.5.49): the one fixed option an imbue scroll put on a class armor piece or a raid accessory for good, the stat of its
 * {@link ImbueSlot} at the scroll's grade. A piece takes one; a greater scroll may replace a normal one, nothing else replaces a line.
 * Only the slot and grade are stored, so the numbers follow {@link ImbueSlot}. Evolution ({@code GearUpgrades.evolve}) and accessory
 * inheritance ({@code Accessories.inherit}) carry it over.
 */
public record Imbue(ImbueSlot slot, int grade) {
	public static final int NORMAL = 1;
	public static final int GREATER = 2;

	private static final Codec<ImbueSlot> SLOT_CODEC = Codec.STRING.comapFlatMap(id -> {
		ImbueSlot slot = ImbueSlot.byId(id);
		return slot == null ? DataResult.error(() -> "Unknown imbue slot " + id) : DataResult.success(slot);
	}, ImbueSlot::id);

	public static final Codec<Imbue> CODEC = RecordCodecBuilder.create(i -> i.group(
		SLOT_CODEC.fieldOf("slot").forGetter(Imbue::slot),
		Codec.INT.fieldOf("grade").forGetter(Imbue::grade)
	).apply(i, Imbue::new));
	public static final StreamCodec<ByteBuf, Imbue> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

	public StatLine line() {
		return StatLine.of(this.slot.stat(), this.slot.value(this.grade));
	}

	public static @Nullable Imbue of(final ItemStack stack) {
		return stack.get(ModDataComponents.IMBUE);
	}

	/** The imbued line of {@code stack}, or none. */
	public static List<StatLine> lines(final ItemStack stack) {
		Imbue imbue = of(stack);
		return imbue == null ? List.of() : List.of(imbue.line());
	}

	/** The slot {@code stack} counts as: a class armor piece's slot or a raid accessory's; null for anything else. */
	public static @Nullable ImbueSlot slotOf(final ItemStack stack) {
		ArmorPieceDef piece = ClassArmor.def(stack);
		if (piece != null) {
			return ImbueSlot.of(piece.slot());
		}
		AccessoryDef accessory = Accessories.def(stack);
		return accessory == null ? null : ImbueSlot.of(accessory.slot());
	}

	/** Whether a scroll for {@code slot} of {@code grade} may go on {@code stack}: the right piece, nothing imbued yet, or greater over normal. */
	public static boolean canApply(final ItemStack stack, final ImbueSlot slot, final int grade) {
		if (slotOf(stack) != slot) {
			return false;
		}
		Imbue current = of(stack);
		return current == null || grade == GREATER && current.grade() < GREATER;
	}
}
