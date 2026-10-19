package com.minecraftmode.client.datagen;

import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider.TranslationBuilder;

/** Translations for the jump courses ({@code jump/JumpForest}): the Forest of Tenacity, its signs, titles and refusals. */
final class JumpLang {
	static void add(final TranslationBuilder b, final boolean ko) {
		b.add("jump.minecraft_mode.tenacity", ko ? "끈기의 숲" : "Forest of Tenacity");
		// entering and leaving
		b.add("message.minecraft_mode.jump.unknown", ko ? "그런 숲은 없습니다." : "There is no such course.");
		b.add("message.minecraft_mode.jump.already", ko ? "이미 숲에 들어가 있습니다." : "You are already in a course.");
		b.add("message.minecraft_mode.jump.busy", ko ? "지금은 숲에 들어갈 수 없습니다. (레이드·던전·캐릭터 선택 중에는 불가)"
			: "You cannot go into the forest right now (not from a raid, a dungeon or the character select).");
		b.add("message.minecraft_mode.jump.full", ko ? "숲이 붐빕니다. 잠시 후에 다시 오세요." : "The forest is crowded. Come back in a moment.");
		b.add("message.minecraft_mode.jump.opening", ko ? "%s의 길이 열리고 있습니다..." : "The way into the %s is opening...");
		b.add("message.minecraft_mode.jump.cancelled", ko ? "숲으로 가는 길이 닫혔습니다." : "The way into the forest closed.");
		b.add("message.minecraft_mode.jump.failed", ko ? "숲이 열리지 않았습니다. 다시 시도해 주세요." : "The forest did not open. Please try again.");
		b.add("message.minecraft_mode.jump.entered", ko ? "%s에 들어왔습니다. 꼭대기의 황금 단상까지 올라가세요!" : "You enter the %s. Climb to the golden goal at the top!");
		b.add("message.minecraft_mode.jump.rules", ko
			? "떨어지면 마지막 체크포인트로 돌아갑니다(피해 없음). 빛나는 이끼 단상을 밟으면 체크포인트가 저장됩니다. 탈것·펫·겉날개·비행·엔더 진주·후렴과·이동 스킬은 쓸 수 없습니다. 일시정지 메뉴의 '귀환'으로 언제든 나갈 수 있습니다."
			: "A fall takes you back to your last checkpoint (no damage). Step on a glowing moss bed to save a checkpoint. No mounts, pets, elytra, flying, ender pearls, chorus fruit or movement skills. Leave any time with 'Return home' in the pause menu.");
		b.add("message.minecraft_mode.jump.left", ko ? "숲을 나왔습니다." : "You leave the forest.");
		b.add("message.minecraft_mode.jump.fell", ko ? "떨어졌습니다! 체크포인트 %s(으)로 돌아갑니다." : "You fell! Back to checkpoint %s.");
		b.add("message.minecraft_mode.jump.cleared", ko ? "%s을(를) 완주했습니다! (떨어진 횟수 %s) %s초 뒤 돌아갑니다." : "You cleared the %s! (falls: %s) Going back in %s s.");
		// refusals
		b.add("message.minecraft_mode.jump.no_item", ko ? "숲에서는 쓸 수 없습니다." : "You cannot use that in the forest.");
		b.add("message.minecraft_mode.jump.no_skill", ko ? "숲에서는 이동 스킬을 쓸 수 없습니다. 직접 뛰어오르세요!" : "No movement skills in the forest - jump!");
		b.add("message.minecraft_mode.jump.no_mount", ko ? "숲에서는 탈것을 탈 수 없습니다." : "No mounts in the forest.");
		b.add("message.minecraft_mode.jump.no_glide", ko ? "숲에서는 활공할 수 없습니다." : "No gliding in the forest.");
		// titles
		b.add("message.minecraft_mode.jump.title_rules", ko ? "떨어져도 괜찮아요, 체크포인트에서 다시!" : "Fall and you start again from your checkpoint");
		b.add("message.minecraft_mode.jump.title_checkpoint", ko ? "체크포인트 %s/%s" : "Checkpoint %s/%s");
		b.add("message.minecraft_mode.jump.title_cleared", ko ? "완주!" : "Cleared!");
		// floating signs
		b.add("sign.minecraft_mode.jump.title", ko ? "~ %s ~" : "~ %s ~");
		b.add("sign.minecraft_mode.jump.rules", ko
			? "큰 나무를 따라 꼭대기의 황금 단상까지 오르세요.\n떨어지면 마지막 체크포인트(빛나는 이끼)로 돌아갑니다.\n탈것·겉날개·진주·이동 스킬 금지\n나가기: 일시정지 메뉴 → 귀환"
			: "Climb the great tree to the golden goal at its top.\nFall and you are back on your last checkpoint (glowing moss).\nNo mounts, elytra, pearls or movement skills.\nLeave: pause menu > Return home");
		b.add("sign.minecraft_mode.jump.checkpoint", ko ? "체크포인트 %s/%s" : "Checkpoint %s/%s");
		b.add("sign.minecraft_mode.jump.goal", ko ? "★ 골 ★" : "★ Goal ★");
		b.add("sign.minecraft_mode.jump.stage.1", ko ? "1단계: 새싹 오솔길" : "Stage 1: Sprout Trail");
		b.add("sign.minecraft_mode.jump.stage.2", ko ? "2단계: 버섯 계단" : "Stage 2: Mushroom Steps");
		b.add("sign.minecraft_mode.jump.stage.3", ko ? "3단계: 나뭇가지 길" : "Stage 3: Branch Walk");
		b.add("sign.minecraft_mode.jump.stage.4", ko ? "4단계: 잎사귀 징검다리" : "Stage 4: Leaf Hops");
		b.add("sign.minecraft_mode.jump.stage.5", ko ? "5단계: 그루터기 기둥" : "Stage 5: Stump Posts");
		b.add("sign.minecraft_mode.jump.stage.crown", ko ? "마지막: 우듬지로!" : "Last: up to the crown!");
	}

	private JumpLang() {
	}
}
