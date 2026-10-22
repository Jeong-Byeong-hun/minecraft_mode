package com.minecraftmode.client.datagen;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.client.vfx.LookEdges;
import com.minecraftmode.client.vfx.LookRanges;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.skill.HitArea;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;

/**
 * Writes docs/SKILL-LOOKS.md: for every tier 4 skill, the area it really hits (read from its actions, {@link HitArea}) next to where its
 * drawn looks put their danger edge ({@link LookEdges} x the content's look sizes), and whether they match ({@link LookRanges}).
 */
public class SkillLooksDocProvider implements DataProvider {
	private final FabricPackOutput output;

	public SkillLooksDocProvider(final FabricPackOutput output) {
		this.output = output;
	}

	@Override
	public CompletableFuture<?> run(final CachedOutput cache) {
		List<LookRanges.Row> rows = LookRanges.tier4();
		long matched = rows.stream().filter(r -> !r.mismatched() && r.undrawn().isEmpty()).count();
		long off = rows.stream().filter(LookRanges.Row::mismatched).count();
		long undrawn = rows.stream().filter(r -> !r.mismatched() && !r.undrawn().isEmpty()).count();
		StringBuilder md = new StringBuilder();
		md.append("# 4차 스킬 이펙트 범위\n\n");
		md.append("> 이 문서는 `./gradlew runDatagen`이 코드 정의에서 생성합니다(`SkillLooksDocProvider`). 직접 고치지 마세요.\n\n");
		md.append("- **실제 범위**: 스킬 액션의 숫자 그대로(`HitArea`). r = 반경(블록), `at N` = N블록 앞 조준 지점, 원뿔 = 거리 / 각도, ")
			.append("광선·경로 = 길이 x 좌우 폭, (fixed) = 확장 각인으로 커지지 않음. 몬스터는 히트박스가 범위에 닿으면 맞습니다.\n");
		md.append("- **이펙트 경계**: 그려진 위험 경계(링·벽·돔·원뿔 끝·광선 테두리)가 놓이는 곳 = 렌더러 감사값(`LookEdges`) x 콘텐츠의 look 크기. ")
			.append("하늘·오라·석상 같은 장식은 경계가 아닙니다.\n");
		md.append("- **일치**: 경계가 실제 범위의 ±").append(Math.round(LookRanges.TOLERANCE * 100)).append("% 안이면 ✅. ")
			.append("◻ = 경계 없음(단일 대상·경로·오라), ◇ = 투사체 몸체가 닿는 반경보다 넓음(빛·형체; 폭발은 따로 그림), ")
			.append("❌ = 어긋남. \"그리지 않음\" = 범위가 있지만 그 범위를 그리는 이펙트가 없는 단계.\n");
		md.append("- `JobClientGameTest.checkLookRanges`가 같은 비교로 어긋남이 없는지 확인하고, 실제 범위를 겹쳐 그린 위에서 본 스크린샷 ")
			.append("`look_area_<무기>.<스킬>`을 찍습니다.\n\n");
		md.append("스킬 ").append(rows.size()).append("개: 모두 일치 ").append(matched).append(", 어긋남 ").append(off).append(", 일부 범위를 그리지 않음 ")
			.append(undrawn).append(".\n\n");
		JobClass current = null;
		for (LookRanges.Row row : rows) {
			if (row.weapon().job() != current) {
				current = row.weapon().job();
				md.append("## ").append(current.ko()).append("\n\n");
				md.append("| 무기 | 스킬 | 실제 범위 | 이펙트 경계 | 일치 |\n|---|---|---|---|---|\n");
			}
			String areas = row.areas().isEmpty() ? "-" : row.areas().stream().map(HitArea::describe).collect(Collectors.joining("<br>"));
			String drawn = row.drawn().isEmpty() ? "-" : row.drawn().stream().map(SkillLooksDocProvider::drawn).collect(Collectors.joining("<br>"));
			StringBuilder verdict = new StringBuilder();
			for (LookRanges.Drawn d : row.drawn()) {
				verdict.append(mark(d)).append(' ');
			}
			if (!row.undrawn().isEmpty()) {
				verdict.append("<br>그리지 않음: ").append(row.undrawn().stream().map(HitArea::describe).collect(Collectors.joining(", ")));
			}
			md.append("| ").append(row.weapon().ko()).append(" | ").append(row.skill().ko()).append(" | ").append(areas).append(" | ").append(drawn)
				.append(" | ").append(verdict.toString().trim().isEmpty() ? "-" : verdict.toString().trim()).append(" |\n");
		}
		md.append('\n');
		try {
			Path root = this.output.getOutputFolder().getParent().getParent().getParent();
			Path file = root.resolve("docs/SKILL-LOOKS.md");
			Files.createDirectories(file.getParent());
			Files.writeString(file, md.toString(), StandardCharsets.UTF_8);
		} catch (IOException | NullPointerException e) {
			MinecraftMode.LOGGER.warn("Could not write docs/SKILL-LOOKS.md", e);
		}
		return CompletableFuture.completedFuture(null);
	}

	private static String drawn(final LookRanges.Drawn d) {
		String name = "`" + d.look().name().toLowerCase(Locale.ROOT) + "`";
		return d.edge() == null ? name + " ?" : name + " " + d.drawn();
	}

	private static String mark(final LookRanges.Drawn d) {
		return switch (d.verdict()) {
			case MATCH -> "✅";
			case MISMATCH -> "❌ " + Math.round(d.error() * 100) + "%";
			case WIDER -> "◇";
			case NO_EDGE -> "◻";
			case UNAUDITED -> "❓";
		};
	}

	@Override
	public String getName() {
		return "Skill look ranges";
	}
}
