package com.minecraftmode.client.datagen;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.engrave.Engraving;
import com.minecraftmode.job.engrave.Engravings;
import com.minecraftmode.job.gear.GearSlot;
import com.minecraftmode.job.weapon.Archetype;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;

/**
 * Writes docs/ENGRAVINGS.md (Korean): every engraving of every class in one place, weapon lines with the weapon shapes they fit, armor
 * lines with the slots they fit, and an index by stat with the cap that bounds its total. Read from {@link Engraving}, so it cannot
 * drift from the code.
 */
public class EngravingDocProvider implements DataProvider {
	private final FabricPackOutput output;

	public EngravingDocProvider(final FabricPackOutput output) {
		this.output = output;
	}

	@Override
	public CompletableFuture<?> run(final CachedOutput cache) {
		List<Engraving> all = Arrays.asList(Engraving.values());
		long weapons = all.stream().filter(e -> !e.isArmor()).count();
		StringBuilder md = new StringBuilder();
		md.append("# 각인\n\n");
		md.append("> 이 문서는 `./gradlew runDatagen`이 코드 정의에서 생성합니다(`EngravingDocProvider`). 직접 고치지 마세요.\n\n");
		md.append("- 각인 ").append(all.size()).append("종: 무기 각인 ").append(weapons).append("종, 방어구 각인 ").append(all.size() - weapons)
			.append("종. 직업마다 따로 있고, 그 직업의 장비에만 붙습니다.\n");
		md.append("- 모험가 길드의 정수 각인대에서 직업 무기에 최대 ").append(Engravings.WEAPON_LINES).append("줄, 직업 방어구에 부위마다 최대 ")
			.append(Engravings.ARMOR_LINES).append("줄 붙입니다. 각인대는 그 장비에 붙을 수 있는 각인 중 서로 다른 후보 3개를 보여 주고, ")
			.append("각인을 붙이거나 후보를 새로 고치면 후보가 바뀝니다(줄을 지우면 그대로).\n");
		md.append("- 표의 수치는 1줄 기준입니다. 같은 각인을 여러 줄 붙이면 수치가 더해지고, 일부 능력치는 각인·옵션·세트 효과 등 모든 출처를 ")
			.append("합친 값에 상한이 있습니다(능력치별 찾아보기의 \"합계 상한\").\n");
		md.append("- 무기 각인은 직업·차수·레벨이 모두 맞아 무기가 제대로 작동할 때만 적용됩니다.\n");
		md.append("- 비용: 차수 × 4(방어구는 3) × (현재 줄 수 + 1) 정수와 장비 구간 길드 가격 × (현재 줄 수 + 1) ÷ 4 동전. ")
			.append("후보 새로 고침: 차수 × 2 정수와 구간 가격 ÷ 6, 줄 제거: 차수 × 2 정수와 구간 가격 ÷ 4. 응축된 정수는 정수 9개로 칩니다.\n\n");

		md.append("## 한눈에 보기\n\n");
		md.append("부위 칸은 그 부위에 붙을 수 있는 방어구 각인 수입니다.\n\n");
		md.append("| 직업 | 무기 각인 | 방어구 각인 | 투구 | 갑옷 | 각반 | 장화 |\n|---|---|---|---|---|---|---|\n");
		for (JobClass job : JobClass.PLAYABLE) {
			List<Engraving> own = all.stream().filter(e -> e.job() == job).toList();
			long weapon = own.stream().filter(e -> !e.isArmor()).count();
			md.append("| [").append(job.ko()).append("](#").append(job.ko()).append(") | ").append(weapon).append(" | ").append(own.size() - weapon).append(" |");
			for (GearSlot slot : GearSlot.ARMOR) {
				md.append(' ').append(Engraving.pool(job, slot).size()).append(" |");
			}
			md.append('\n');
		}
		md.append('\n');

		for (JobClass job : JobClass.PLAYABLE) {
			List<Engraving> own = all.stream().filter(e -> e.job() == job).toList();
			List<Engraving> weapon = own.stream().filter(e -> !e.isArmor()).toList();
			List<Engraving> armor = own.stream().filter(Engraving::isArmor).toList();
			md.append("## ").append(job.ko()).append("\n\n");
			md.append("### 무기 각인 (").append(weapon.size()).append(")\n\n");
			md.append("| 각인 | 효과 (1줄) | 장착 가능 무기 |\n|---|---|---|\n");
			for (Engraving e : weapon) {
				List<Archetype> fits = Arrays.stream(Archetype.values()).filter(a -> e.fits(job, a)).toList();
				String only = fits.size() == Archetype.values().length ? "전체" : fits.stream().map(Archetype::ko).collect(Collectors.joining(", "));
				md.append("| ").append(e.ko()).append(" | ").append(statText(e.stat(), e.value(), job)).append(" | ").append(only).append(" |\n");
			}
			md.append("\n### 방어구 각인 (").append(armor.size()).append(")\n\n");
			md.append("| 각인 | 효과 (1줄) | 투구 | 갑옷 | 각반 | 장화 |\n|---|---|---|---|---|---|\n");
			for (Engraving e : armor) {
				md.append("| ").append(e.ko()).append(" | ").append(statText(e.stat(), e.value(), job)).append(" |");
				for (GearSlot slot : GearSlot.ARMOR) {
					md.append(e.slots().contains(slot) ? " ✔ |" : " |");
				}
				md.append('\n');
			}
			md.append('\n');
		}

		md.append("## 능력치별 찾아보기\n\n");
		md.append("- 같은 능력치를 주는 각인을 모았습니다. 괄호 안은 직업, 숫자는 1줄 수치입니다(단위는 능력치 칸과 같음).\n");
		md.append("- 합계 상한은 각인만이 아니라 그 능력치를 주는 모든 출처를 합친 값의 상한입니다. 재사용 대기시간 감소는 퍼센트와 초 감소를 ")
			.append("합쳐 최대 60%이기도 합니다.\n\n");
		md.append("| 능력치 | 합계 상한 | 무기 각인 | 방어구 각인 |\n|---|---|---|---|\n");
		for (EngraveStat stat : EngraveStat.values()) {
			List<Engraving> lines = all.stream().filter(e -> e.stat() == stat).toList();
			if (lines.isEmpty()) {
				continue;
			}
			md.append("| ").append(String.format(Locale.ROOT, stat.ko(), "N").replace("%%", "%")).append(" | ")
				.append(stat.cap() > 0 ? ClassDocProvider.num(stat.cap()) : "-").append(" | ")
				.append(listed(lines.stream().filter(e -> !e.isArmor()).toList())).append(" | ")
				.append(listed(lines.stream().filter(Engraving::isArmor).toList())).append(" |\n");
		}

		try {
			Path root = this.output.getOutputFolder().getParent().getParent().getParent();
			Path file = root.resolve("docs/ENGRAVINGS.md");
			Files.createDirectories(file.getParent());
			Files.writeString(file, md.toString(), StandardCharsets.UTF_8);
		} catch (IOException | NullPointerException e) {
			MinecraftMode.LOGGER.warn("Could not write docs/ENGRAVINGS.md", e);
		}
		return CompletableFuture.completedFuture(null);
	}

	private static String listed(final List<Engraving> lines) {
		return lines.isEmpty()
			? "-"
			: lines.stream().map(e -> e.ko() + "(" + e.job().ko() + ") " + ClassDocProvider.num(e.value())).collect(Collectors.joining(", "));
	}

	private static String statText(final EngraveStat stat, final float value, final JobClass job) {
		return String.format(Locale.ROOT, stat.ko(job), ClassDocProvider.num(value)).replace("%%", "%");
	}

	@Override
	public String getName() {
		return "Engraving documentation";
	}
}
