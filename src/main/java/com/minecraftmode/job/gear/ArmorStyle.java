package com.minecraftmode.job.gear;

import com.minecraftmode.job.JobClass;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/**
 * How a class dresses: piece names, how much armor and toughness its sets give, the equip sound,
 * and the drawing style used by the armor artist.
 */
public enum ArmorStyle {
	PLATE(1.25F, 1.0F, SoundEvents.ARMOR_EQUIP_IRON,
		new String[] {"Helm", "Plate", "Greaves", "Sabatons"}, new String[] {"투구", "흉갑", "각반", "철장화"}),
	LEATHER(0.80F, 0.4F, SoundEvents.ARMOR_EQUIP_LEATHER,
		new String[] {"Hood", "Vest", "Leggings", "Boots"}, new String[] {"두건", "조끼", "바지", "장화"}),
	ROBE(0.65F, 0.3F, SoundEvents.ARMOR_EQUIP_LEATHER,
		new String[] {"Hat", "Robe", "Trousers", "Shoes"}, new String[] {"모자", "로브", "하의", "신발"}),
	RANGER(0.85F, 0.4F, SoundEvents.ARMOR_EQUIP_CHAIN,
		new String[] {"Cap", "Jerkin", "Breeches", "Boots"}, new String[] {"모자", "가죽 상의", "각반", "장화"}),
	COAT(1.0F, 0.6F, SoundEvents.ARMOR_EQUIP_CHAIN,
		new String[] {"Tricorn", "Coat", "Breeches", "Boots"}, new String[] {"삼각모", "코트", "바지", "장화"});

	private final float armorFactor;
	private final float toughnessFactor;
	private final Holder<SoundEvent> sound;
	private final String[] pieceEn;
	private final String[] pieceKo;

	ArmorStyle(final float armorFactor, final float toughnessFactor, final Holder<SoundEvent> sound, final String[] pieceEn, final String[] pieceKo) {
		this.armorFactor = armorFactor;
		this.toughnessFactor = toughnessFactor;
		this.sound = sound;
		this.pieceEn = pieceEn;
		this.pieceKo = pieceKo;
	}

	public static ArmorStyle of(final JobClass job) {
		return switch (job) {
			case WARRIOR -> PLATE;
			case ROGUE -> LEATHER;
			case MAGE -> ROBE;
			case ARCHER, HUNTER -> RANGER;
			case SHINIGAMI -> LEATHER;
			default -> COAT;
		};
	}

	public float armorFactor() {
		return this.armorFactor;
	}

	public float toughnessFactor() {
		return this.toughnessFactor;
	}

	public Holder<SoundEvent> sound() {
		return this.sound;
	}

	public String pieceEn(final GearSlot slot) {
		return this.pieceEn[slot.armorIndex()];
	}

	public String pieceKo(final GearSlot slot) {
		return this.pieceKo[slot.armorIndex()];
	}
}
