package com.minecraftmode.client.datagen;

import com.minecraftmode.registry.ModItems;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider.TranslationBuilder;

/** Translations for the residence and the waymaster: travel places, land deeds, claim messages, the action bar and the land screen. */
final class ResidenceLang {
	static void add(final TranslationBuilder b, final boolean ko) {
		String m = "message.minecraft_mode.land.";
		String s = "screen.minecraft_mode.land.";
		b.add(ModItems.LAND_DEED, ko ? "토지 소유권 증서" : "Land Deed");
		b.add("item.minecraft_mode.land_deed.tooltip", ko
			? "거주구역에서 사용: 서 있는 청크 기둥 전체(16×16, 바닥부터 건축 한계까지)가 내 땅이 됩니다. 웅크리고 사용 또는 /land: 땅 관리"
			: "Use in the residence: the whole chunk column you stand in (16x16, from the bottom of the world to the build limit) becomes yours. Sneak-use or /land: manage your land");
		// claiming
		b.add(m + "claimed", ko ? "땅을 얻었습니다: X %s~%s, Z %s~%s (높이 제한 없이 위아래 모두)" : "This land is yours: X %s..%s, Z %s..%s (all the way up and down)");
		b.add(m + "spawn", ko ? "거주구역 광장과 대로는 점유할 수 없습니다." : "The residence square and its avenues cannot be claimed.");
		b.add(m + "taken", ko ? "이 청크 기둥에는 이미 다른 사람의 땅이 있습니다." : "Someone else already owns land in this chunk column.");
		b.add(m + "overlap", ko ? "이 청크 기둥은 이미 내 땅입니다. 위아래 모두 내 땅이라 증서가 더 필요 없습니다." : "This chunk column is already yours, all the way up and down.");
		b.add(m + "limit", ko ? "땅은 최대 %s개까지 가질 수 있습니다." : "You can own at most %s plots.");
		b.add(m + "height", ko ? "세계 범위 밖에서는 땅을 점유할 수 없습니다." : "Land cannot be claimed outside the world.");
		b.add(m + "refunded", ko ? "땅이 청크 기둥 전체로 바뀌었습니다. 같은 기둥에 겹쳐 쓴 증서 %s장을 돌려드렸고, 다음 증서 가격도 그만큼 내려갑니다."
			: "Land now covers the whole chunk column: the %s deeds you stacked in one column came back, and the next deed costs that much less.");
		b.add(m + "wrong_world", ko ? "토지 소유권 증서는 거주구역에서만 쓸 수 있습니다. 광장 분수 서쪽의 이동술사 오린에게 가세요."
			: "Land deeds only work in the residence. Ask Waymaster Orin, west of the plaza fountain.");
		// the waymaster
		b.add("travel.minecraft_mode.residence", ko ? "거주구역" : "The Residence");
		b.add("travel.minecraft_mode.plaza", ko ? "수도 광장" : "The Plaza");
		b.add("screen.minecraft_mode.travel.title", ko ? "어디로 갈까요?" : "Where to?");
		b.add("screen.minecraft_mode.travel.none", ko ? "여기서는 갈 수 있는 곳이 없습니다." : "There is nowhere to go from here.");
		b.add("message.minecraft_mode.travel.arrived", ko ? "%s에 도착했습니다." : "You arrive at %s.");
		// trust list
		b.add(m + "not_online", ko ? "%s님은 접속해 있지 않습니다." : "%s is not online.");
		b.add(m + "trust_self", ko ? "자기 자신은 추가할 필요가 없습니다." : "You do not need to add yourself.");
		b.add(m + "trusted", ko ? "%s님이 이제 내 땅을 쓸 수 있습니다." : "%s may now use your land.");
		b.add(m + "trusted_you", ko ? "%s님이 자기 땅을 쓸 수 있게 허용했습니다." : "%s has let you use their land.");
		b.add(m + "trust_full", ko ? "이미 목록에 있거나 목록이 가득 찼습니다." : "Already on the list, or the list is full.");
		b.add(m + "untrusted", ko ? "%s님의 허용을 취소했습니다." : "%s may no longer use your land.");
		b.add(m + "abandoned", ko ? "땅을 포기했습니다. 땅은 처음 초원으로 돌아갔고, 증서와 보관함 물건 %s묶음을 돌려받았습니다."
			: "You gave up the land: it is grassland again, and the deed and %s stacks from its containers came back to you.");
		b.add(s + "abandon_hint", ko ? "포기하면 처음 초원으로 돌아갑니다 (물건은 반환)" : "Giving up resets the land (items come back)");
		// refusals and the action bar
		b.add(m + "refused", ko ? "%s님의 땅입니다." : "This is %s's land.");
		b.add(m + "refused_wild", ko ? "주인 없는 땅에는 지을 수 없습니다. 토지 소유권 증서로 점유하세요." : "Nobody can build on unclaimed land. Claim it with a land deed.");
		b.add(m + "hud.square", ko ? "거주구역 광장" : "Residence square");
		b.add(m + "hud.avenue", ko ? "거주구역 대로" : "Residence avenue");
		b.add(m + "hud.wild", ko ? "주인 없는 땅 · 증서로 점유할 수 있습니다" : "Unclaimed land · a deed can claim it");
		b.add(m + "hud.mine", ko ? "내 땅" : "Your land");
		b.add(m + "hud.trusted", ko ? "%s님의 땅 · 허용됨" : "%s's land · you may build");
		b.add(m + "hud.other", ko ? "%s님의 땅 · 출입만 가능" : "%s's land · look, don't touch");
		// land screen
		b.add(s + "title", ko ? "내 땅 관리" : "My Land");
		b.add(s + "plots", ko ? "내 땅 (%s/%s)" : "My plots (%s/%s)");
		b.add(s + "no_plots", ko ? "아직 땅이 없습니다. 거주구역에서 토지 소유권 증서를 쓰세요." : "No land yet. Use a land deed in the residence.");
		b.add(s + "plot", ko ? "X %s~%s · Z %s~%s · 바닥 Y %s" : "X %s..%s · Z %s..%s · floor Y %s");
		b.add(s + "abandon", ko ? "포기" : "Give up");
		b.add(s + "confirm", ko ? "확인?" : "Sure?");
		b.add(s + "friends", ko ? "허용한 플레이어 (%s/%s) · 내 모든 땅에 적용" : "Trusted players (%s/%s) · all your land");
		b.add(s + "name_hint", ko ? "접속 중인 플레이어 이름" : "Name of an online player");
		b.add(s + "add", ko ? "추가" : "Add");
		b.add(s + "no_friends", ko ? "아무도 없습니다. 나만 짓고 열 수 있습니다." : "Nobody yet: only you can build and open things.");
	}

	private ResidenceLang() {
	}
}
