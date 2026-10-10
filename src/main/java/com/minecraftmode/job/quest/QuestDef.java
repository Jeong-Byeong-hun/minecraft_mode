package com.minecraftmode.job.quest;

import com.minecraftmode.job.JobClass;
import java.util.List;
import java.util.Set;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

/**
 * One advancement trial: defeat the listed enemies, collect the trial tokens they drop while the
 * quest is active, and bring the tokens plus the extra materials (essence, boss drops) back to the
 * class trainer. A trial without goals, tokens or materials ({@link #instant()}, the first choice of
 * class) is no trial at all: the trainer grants the tier on the spot. Ids are saved in player data,
 * so never rename them.
 */
public record QuestDef(
	String id,
	JobClass job,
	int tier,
	String en,
	String ko,
	String storyEn,
	String storyKo,
	List<KillGoal> kills,
	Item token,
	int tokenCount,
	List<TokenSource> sources,
	List<Material> materials
) {
	/** Defeat {@code count} of {@code types}; {@code ranged} = only kills by projectile count. */
	public record KillGoal(String en, String ko, Set<EntityType<?>> types, int count, boolean ranged) {
		public boolean matches(final EntityType<?> type, final boolean byProjectile) {
			return this.types.contains(type) && (!this.ranged || byProjectile);
		}
	}

	/** Defeating one of {@code types} gives {@code amount} tokens with {@code chance}. */
	public record TokenSource(Set<EntityType<?>> types, float chance, int amount) {
	}

	/** Extra item handed in at the end (essence, condensed essence, boss drops). */
	public record Material(Item item, int count) {
	}

	/** Nothing to do: accepting it advances right away. */
	public boolean instant() {
		return this.kills.isEmpty() && this.tokenCount == 0 && this.materials.isEmpty();
	}

	public String nameKey() {
		return "quest.minecraft_mode." + this.id;
	}

	public String storyKey() {
		return this.nameKey() + ".story";
	}

	public String goalKey(final int index) {
		return this.nameKey() + ".goal" + index;
	}
}
