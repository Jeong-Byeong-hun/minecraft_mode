package com.minecraftmode.client.datagen;

import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider.TranslationBuilder;

/** Lang for the world map, the minimap and waypoints. */
final class MapLang {
	static void add(final TranslationBuilder b, final boolean ko) {
		b.add("key.minecraft_mode.map", ko ? "월드 지도" : "World Map");
		b.add("key.minecraft_mode.minimap", ko ? "미니맵 켜기/끄기" : "Toggle Minimap");
		b.add("message.minecraft_mode.map.minimap_on", ko ? "미니맵 켜짐" : "Minimap on");
		b.add("message.minecraft_mode.map.minimap_off", ko ? "미니맵 꺼짐" : "Minimap off");
		b.add("screen.minecraft_mode.map.title", ko ? "월드 지도" : "World Map");
		b.add("screen.minecraft_mode.map.capital", ko ? "스톰홀드" : "Stormhold");
		b.add("screen.minecraft_mode.map.death", ko ? "마지막 사망 지점" : "Last death");
		b.add("screen.minecraft_mode.map.me", ko ? "내 위치" : "Me");
		b.add("screen.minecraft_mode.map.add", ko ? "+ 웨이포인트" : "+ Waypoint");
		b.add("screen.minecraft_mode.map.minimap_on", ko ? "미니맵: 켜짐" : "Minimap: on");
		b.add("screen.minecraft_mode.map.minimap_off", ko ? "미니맵: 꺼짐" : "Minimap: off");
		b.add("screen.minecraft_mode.map.size", ko ? "크기 %s" : "Size %s");
		b.add("screen.minecraft_mode.map.zoom", ko ? "배율 x%s" : "Zoom x%s");
		b.add("screen.minecraft_mode.map.scale", ko ? "1칸 = %s px" : "%s px / block");
		b.add("screen.minecraft_mode.map.hint", ko ? "드래그: 이동 · 휠: 확대/축소 · 클릭: NPC 선택 · 우클릭: 웨이포인트 추가"
			: "Drag to pan - wheel to zoom - click an NPC to pick it - right-click to add a waypoint");
		b.add("screen.minecraft_mode.map.waypoints", ko ? "웨이포인트" : "Waypoints");
		b.add("screen.minecraft_mode.map.none", ko ? "아직 없습니다. 지도를 우클릭하거나 + 웨이포인트를 누르세요." : "None yet - right-click the map or press + Waypoint.");
		b.add("screen.minecraft_mode.map.npcs", ko ? "NPC" : "NPCs");
		b.add("screen.minecraft_mode.map.no_city", ko ? "NPC는 스톰홀드가 있는 오버월드 지도에만 표시됩니다." : "NPCs are shown on the overworld map, where Stormhold stands.");
		b.add("screen.minecraft_mode.map.new_waypoint", ko ? "새 웨이포인트" : "New waypoint");
		b.add("screen.minecraft_mode.map.edit_waypoint", ko ? "웨이포인트 편집" : "Edit waypoint");
		b.add("screen.minecraft_mode.map.name", ko ? "이름" : "Name");
		b.add("screen.minecraft_mode.map.default_name", ko ? "웨이포인트 %s" : "Waypoint %s");
		b.add("screen.minecraft_mode.map.color", ko ? "색상" : "Colour");
		b.add("screen.minecraft_mode.map.delete", ko ? "삭제" : "Delete");
	}

	private MapLang() {
	}
}
