# Minecraft Mode

Minecraft **26.3** (Java Edition) Fabric 모드. 새 광물·도구·갑옷, 출혈 효과, 적대적 몹, 인챈트, 코인 경제를 추가한다.

## 콘텐츠

| 분류 | 내용 |
|---|---|
| 미스릴 | 광석(Y -64~16, 철 곡괭이 이상) → 원석 → 주괴/조각/블록. 도구 5종 + 갑옷 4종 (철과 다이아 사이, 흑요석 채굴 가능) |
| 알루미늄 | 광석(Y -16~112, 돌 곡괭이 이상) → 원석 → 주괴/조각/블록 |
| 플라스틱 | 석탄×2 + 슬라임볼 → 판 4장 / 말린 켈프 블록 제련 → 판 1장 / 판 9장 → 블록 |
| 출혈 효과 | 40틱마다 1 피해(레벨당 주기 절반), 이동속도 -10%, 독과 달리 죽을 수 있음 |
| 광산 약탈자 | Y 40 미만 동굴에 스폰. 곡괭이 근접공격, 40% 확률로 출혈 5초. 미스릴 원석·동화·은화 드롭 |
| 인챈트 | 흡혈(공격 시 재생 II), 출혈의 칼날(공격 시 출혈), 동전 탐색(광석 채굴 시 동화 드롭). 모두 인챈트 테이블에 나옴 |
| 경제 | 동화 9 = 은화 1, 은화 9 = 금화 1. 플레이어가 죽인 적대 몹이 코인 드롭(보스는 금화). 상점 블록(우클릭)에서 아이템 사고팔기 |

상점 블록 조합법: `알루미늄 주괴 ×3 / 플라스틱 판·에메랄드·플라스틱 판 / 플라스틱 판·상자·플라스틱 판`

## 개발 환경

- JDK 25 (Temurin), Gradle 9.7.1(wrapper), Fabric Loom 1.18, Fabric Loader 0.19.5, Fabric API 0.162.0+26.3
- 26.x는 난독화가 없어서 매핑 없이 Mojang 공식 이름을 그대로 쓴다.

```bash
./gradlew build              # build/libs/minecraft_mode-<ver>.jar
./gradlew runClient          # 개발용 클라이언트 실행
./gradlew runDatagen         # 레시피/모델/태그/번역/월드젠/인챈트 JSON → src/main/generated
./gradlew runClientGameTest  # 자동 테스트 (월드 생성 → 검증 → 스크린샷 build/run/clientGameTest/screenshots)
java tools/TextureGen.java ~/.gradle/caches/fabric-loom/26.3/minecraft-client-only.jar src/main/resources/assets/minecraft_mode/textures
```

설치: `build/libs/minecraft_mode-<ver>.jar`를 Fabric Loader + Fabric API가 설치된 26.3 클라이언트/서버의 `mods/` 폴더에 넣는다.

## 텍스처 주의

`tools/TextureGen.java`가 만든 **임시 텍스처**다. 금속·광석·도구·갑옷은 바닐라 텍스처의 색조를 바꾼 것이라 공개 배포 전에는 직접 그린 그림으로 교체하는 것이 좋다. 코인·상점·플라스틱 블록·몹·알·효과 아이콘은 직접 그린 것이다.
