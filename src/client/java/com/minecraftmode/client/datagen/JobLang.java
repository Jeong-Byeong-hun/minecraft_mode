package com.minecraftmode.client.datagen;

import com.minecraftmode.economy.ShopType;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.engrave.Engraving;
import com.minecraftmode.job.skill.Actions;
import com.minecraftmode.job.skill.Skill;
import com.minecraftmode.job.skill.SkillKind;
import com.minecraftmode.job.weapon.Archetype;
import com.minecraftmode.job.weapon.JobWeapons;
import com.minecraftmode.job.weapon.ProjectileStyle;
import com.minecraftmode.job.weapon.WeaponDef;
import com.minecraftmode.registry.ModCreativeTabs;
import com.minecraftmode.registry.ModEffects;
import com.minecraftmode.registry.ModItems;
import java.util.Map;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider.TranslationBuilder;

/** Every class-system translation, in English or Korean. Names come from the content definitions. */
final class JobLang {
	static void add(final TranslationBuilder b, final boolean ko) {
		b.addCreativeModeTab(ModCreativeTabs.CLASSES, ko ? "마인크래프트 모드: 직업" : "Minecraft Mode: Classes");

		// items and blocks
		b.add(ModItems.ESSENCE, ko ? "정수" : "Essence");
		b.add(ModItems.CONDENSED_ESSENCE, ko ? "응축된 정수" : "Condensed Essence");
		b.add(ModItems.GOLEM_CORE, ko ? "골렘의 핵" : "Golem Core");
		b.add(ModItems.CLASS_RESET_SCROLL, ko ? "직업 초기화 주문서" : "Class Reset Scroll");
		b.add(ModItems.PROJECTILE_SHURIKEN, ko ? "표창" : "Shuriken");
		b.add(ModItems.PROJECTILE_KUNAI, ko ? "쿠나이" : "Kunai");
		b.add(ModItems.PROJECTILE_KNIFE, ko ? "투척 단검" : "Throwing Knife");
		b.add(ModItems.PROJECTILE_BULLET, ko ? "총알" : "Bullet");
		b.add(ModItems.PROJECTILE_CANNONBALL, ko ? "포탄" : "Cannonball");
		b.add(ModItems.PROJECTILE_ICICLE, ko ? "고드름" : "Icicle");
		b.add(ModItems.PROJECTILE_HARPOON, ko ? "작살" : "Harpoon");
		b.add(ModItems.GUILD_SHOP, ko ? "직업 길드" : "Class Guild");
		b.add(ModItems.ENGRAVING_TABLE, ko ? "정수 각인대" : "Essence Engraving Table");
		b.add(ShopType.GUILD.titleKey(), ko ? "직업 길드" : "Class Guild");
		b.add("container.minecraft_mode.engraving_table", ko ? "정수 각인대" : "Essence Engraving Table");

		b.add(ModEffects.STUN.value(), ko ? "기절" : "Stun");
		b.add(ModEffects.VULNERABLE.value(), ko ? "취약" : "Vulnerable");
		b.add(ModEffects.MANA_FLOW.value(), ko ? "마나 흐름" : "Mana Flow");

		// classes
		for (JobClass job : JobClass.values()) {
			b.add(job.nameKey(), ko ? job.ko() : job.en());
			for (int tier = 1; tier <= job.tierCount(); tier++) {
				JobClass.Tier t = job.tier(tier);
				b.add(job.tierKey(tier), ko ? t.ko() : t.en());
				b.add(job.passiveKey(tier), ko ? t.passiveKo() : t.passiveEn());
				b.add(job.passiveDescKey(tier), ko ? t.passiveDescKo() : t.passiveDescEn());
			}
		}

		// weapons and skills
		for (WeaponDef def : JobWeapons.all()) {
			b.add(JobWeapons.item(def), ko ? def.ko() : def.en());
			for (Skill skill : def.skills()) {
				b.add(skill.nameKey(), ko ? skill.ko() : skill.en());
			}
		}
		for (SkillKind kind : SkillKind.values()) {
			b.add(kind.nameKey(), ko ? kind.ko() : kind.en());
		}
		for (Archetype type : Archetype.values()) {
			b.add(type.nameKey(), ko ? type.ko() : type.en());
		}
		for (ProjectileStyle style : ProjectileStyle.values()) {
			b.add(style.nameKey(), ko ? style.ko() : style.en());
		}
		for (Actions.Summon summon : Actions.Summon.values()) {
			b.add(summon.nameKey(), ko ? summon.ko() : summon.en());
		}
		for (Map.Entry<String, String[]> text : Actions.texts().entrySet()) {
			b.add("skill_action.minecraft_mode." + text.getKey(), text.getValue()[ko ? 1 : 0]);
		}

		// engravings
		for (Engraving e : Engraving.values()) {
			b.add(e.nameKey(), ko ? e.ko() : e.en());
		}
		for (EngraveStat stat : EngraveStat.values()) {
			b.add(stat.key(), ko ? stat.ko() : stat.en());
		}

		// keys
		b.add("key.category.minecraft_mode.classes", ko ? "마인크래프트 모드: 직업" : "Minecraft Mode: Classes");
		for (int i = 1; i <= 4; i++) {
			b.add("key.minecraft_mode.skill_" + i, ko ? "스킬 " + i : "Skill " + i);
		}
		b.add("key.minecraft_mode.job_screen", ko ? "직업 창" : "Class Screen");

		// messages
		b.add("message.minecraft_mode.job.level_up", ko ? "레벨 업! 이제 %s레벨입니다." : "Level up! You are now level %s.");
		b.add("message.minecraft_mode.job.can_advance", ko
			? "다음 전직 시련을 받을 수 있습니다! 스톰홀드의 교관을 찾아가세요."
			: "Your next advancement trial is open! Visit your trainer in Stormhold.");
		b.add("message.minecraft_mode.job.can_choose", ko
			? "레벨 10 달성! 스톰홀드의 교관에게 가면 바로 전직할 수 있습니다."
			: "Level 10! Visit a trainer in Stormhold to take a class right away.");
		b.add("message.minecraft_mode.job.advanced", ko ? "%s님이 %s(으)로 전직했습니다!" : "%s has become a %s!");
		b.add("message.minecraft_mode.job.avalon", ko ? "아발론이 치명상을 막아냈습니다! (3분 후 재사용)" : "Avalon turned aside a lethal blow! (ready again in 3 minutes)");
		b.add("message.minecraft_mode.job.reset_none", ko ? "초기화할 직업이 없습니다." : "You have no class to reset.");
		b.add("message.minecraft_mode.essence.too_few", ko ? "정수가 %s개 있어야 응축할 수 있습니다." : "You need %s essence to condense.");
		b.add("tooltip.minecraft_mode.essence.use", ko ? "사용: 정수 9개 → 응축된 정수 1개 (웅크리고 사용: 전부)" : "Use: 9 essence → 1 condensed essence (sneak: all)");
		b.add("tooltip.minecraft_mode.condensed_essence.use", ko ? "사용: 정수 9개로 분해 (웅크리고 사용: 묶음 전체)" : "Use: break into 9 essence (sneak: whole stack)");
		b.add("tooltip.minecraft_mode.essence.change", ko ? "정수와 응축된 정수는 서로 대신 낼 수 있습니다 (잔돈은 정수로)" : "Essence and condensed essence pay for each other (change comes back as essence)");
		b.add("message.minecraft_mode.job.reset_done", ko ? "직업이 초기화되었습니다. 레벨은 유지됩니다." : "Your class was reset. Your level is kept.");
		b.add("message.minecraft_mode.skill.no_weapon", ko ? "직업 무기를 들고 있어야 합니다." : "Hold a class weapon to use skills.");
		b.add("message.minecraft_mode.skill.wrong_class", ko ? "%s 전용 무기입니다. 기본 공격만 가능합니다." : "Only a %s can use this weapon's skills. Basic attacks only.");
		b.add("message.minecraft_mode.skill.low_tier", ko ? "%s차 전직이 필요합니다." : "Requires class tier %s.");
		b.add("message.minecraft_mode.skill.low_level", ko ? "레벨 %s 이상이 필요합니다." : "Requires level %s.");
		b.add("message.minecraft_mode.skill.stunned", ko ? "기절 상태에서는 스킬을 쓸 수 없습니다." : "You cannot cast while stunned.");
		b.add("message.minecraft_mode.skill.cooldown", ko ? "%s 재사용 대기 중 (%s초)" : "%s is on cooldown (%ss)");
		b.add("message.minecraft_mode.skill.no_mana", ko ? "MP가 부족합니다." : "Not enough MP.");

		// commands
		b.add("commands.minecraft_mode.job.info", ko ? "%s: %s (%s차), Lv %s, 경험치 %s/%s, MP %s/%s" : "%s: %s (tier %s), Lv %s, EXP %s/%s, MP %s/%s");
		b.add("commands.minecraft_mode.job.set", ko ? "플레이어 %s명의 직업을 %s(으)로 설정했습니다" : "Set the class of %s player(s) to %s");
		b.add("commands.minecraft_mode.job.level", ko ? "플레이어 %s명의 레벨을 %s(으)로 설정했습니다" : "Set the level of %s player(s) to %s");
		b.add("commands.minecraft_mode.job.exp", ko ? "경험치 %s을(를) 플레이어 %s명에게 주었습니다" : "Gave %s class EXP to %s player(s)");
		b.add("commands.minecraft_mode.job.mana", ko ? "플레이어 %s명의 MP를 채웠습니다" : "Refilled the MP of %s player(s)");
		b.add("commands.minecraft_mode.job.cooldowns", ko ? "플레이어 %s명의 재사용 대기시간을 초기화했습니다" : "Reset the cooldowns of %s player(s)");

		// tooltips
		b.add("tooltip.minecraft_mode.weapon.tier", ko ? "%s차 %s" : "Tier %s %s");
		b.add("tooltip.minecraft_mode.weapon.level", ko ? "요구 Lv %s" : "Req. Lv %s");
		b.add("tooltip.minecraft_mode.weapon.locked", ko ? "조건 미충족: 기본 공격만 가능" : "Requirements not met: basic attacks only");
		b.add("tooltip.minecraft_mode.weapon.power", ko ? "위력 %s" : "Power %s");
		b.add("tooltip.minecraft_mode.weapon.shot", ko ? "우클릭 %s: 피해 %s, %s초" : "Right-click %s: %s damage, %ss");
		b.add("tooltip.minecraft_mode.weapon.draw", ko ? "당겨 쏘기 %s: 피해 %s, %s초 충전" : "Draw %s: %s damage, %ss full draw");
		b.add("tooltip.minecraft_mode.weapon.skills", ko ? "스킬" : "Skills");
		b.add("tooltip.minecraft_mode.weapon.skills_hint", ko ? "스킬 (Shift: 자세히)" : "Skills (hold Shift for details)");
		b.add("tooltip.minecraft_mode.weapon.engravings", ko ? "각인 %s/%s" : "Engravings %s/%s");
		b.add("tooltip.minecraft_mode.weapon.no_engravings", ko ? " 정수 각인대에서 각인할 수 있습니다" : " Engrave it at an Essence Engraving Table");
		b.add("tooltip.minecraft_mode.skill.cost", ko ? "MP %s · %s초" : "MP %s · %ss");

		// class screen
		b.add("screen.minecraft_mode.job", ko ? "직업" : "Class");
		b.add("screen.minecraft_mode.job.exp", ko ? "경험치 %s" : "EXP %s");
		b.add("screen.minecraft_mode.job.mana", ko ? "MP %s/%s (초당 +%s)" : "MP %s/%s (+%s/s)");
		b.add("screen.minecraft_mode.job.no_class", ko
			? "레벨 %s에 직업을 고를 수 있습니다. 몬스터를 처치하거나 철 이상의 광석을 캐면 레벨이 오릅니다."
			: "Choose a class at level %s. Gain levels by defeating monsters and mining iron or better ores.");
		b.add("screen.minecraft_mode.job.tier_line", ko ? "%s차 · %s" : "Tier %s · %s");
		b.add("screen.minecraft_mode.job.requires_level", ko ? "요구 레벨 %s" : "Requires level %s");
		b.add("screen.minecraft_mode.job.next_passive", ko ? "해금 패시브 %s: %s" : "Unlocks %s: %s");
		b.add("screen.minecraft_mode.job.final", ko ? "최종 전직을 달성했습니다!" : "You have reached the final tier!");
		b.add("screen.minecraft_mode.job.hint", ko
			? "스킬 키: %s %s %s %s. 정수는 몬스터와 철 이상 광석에서 얻고 정수 각인대에서 씁니다. 직업 무기는 직업 길드에서 삽니다."
			: "Skill keys: %s %s %s %s. Essence drops from monsters and iron+ ores; spend it at the Essence Engraving Table. Buy class weapons at the Class Guild.");

		// engraving screen
		b.add("screen.minecraft_mode.engraving.empty_line", ko ? "비어 있음" : "Empty");
		b.add("screen.minecraft_mode.engraving.offers", ko ? "각인 후보" : "Offers");
		b.add("screen.minecraft_mode.engraving.insert", ko ? "직업 무기를 왼쪽 슬롯에 넣으세요. 각인은 무기당 3줄, 같은 각인은 중첩됩니다." : "Put a class weapon in the slot. Up to 3 lines per weapon; the same line stacks.");
		b.add("screen.minecraft_mode.engraving.remove", ko ? "✕ 클릭: 이 줄 제거 (◆%s)" : "Click ✕ to remove this line (◆%s)");
		b.add("screen.minecraft_mode.engraving.after", ko ? "각인 후: " : "After: ");
		b.add("screen.minecraft_mode.engraving.cost", ko ? "정수 %s개 (응축된 정수 = 9개)" : "%s essence (condensed essence = 9)");
		b.add("screen.minecraft_mode.engraving.reroll", ko ? "후보 새로 고침" : "Reroll offers");

		TrialLang.add(b, ko);
		GearLang.add(b, ko);
		LootLang.add(b, ko);
		MonsterLang.add(b, ko);
		RaidLang.add(b, ko);
		EconomyLang.add(b, ko);
		ConsumableLang.add(b, ko);
		EndgameLang.add(b, ko);
		ContentLang.add(b, ko);
		MapLang.add(b, ko);
		TownLang.add(b, ko);
	}

	private JobLang() {
	}
}
