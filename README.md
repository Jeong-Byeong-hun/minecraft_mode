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
java tools/TextureGen.java src/main/resources/assets/minecraft_mode/textures  # 텍스처 재생성
```

설치: `build/libs/minecraft_mode-<ver>.jar`를 Fabric Loader + Fabric API가 설치된 26.3 클라이언트/서버의 `mods/` 폴더에 넣는다.

## 텍스처

모든 텍스처는 `tools/TextureGen.java`가 직접 그린다(바닐라 에셋을 읽지 않음).

- 아이템(주괴·조각·원석·갑옷·플라스틱 판): 16x16 문자 격자 + 5단계 팔레트
- 도구: 대각선 축 위의 모양 함수(자루 왼쪽 아래, 머리 오른쪽 위)
- 돌/심층암 바탕·광석 결정·금속 블록·원석 블록: 시드 고정 절차 생성(이어 붙여도 이음새 없음)
- 갑옷 착용 텍스처: 휴머노이드 모델 박스 UV에 맞춰 면 단위로 칠함(성인 64x32, 아기 64x64)

색이나 모양을 바꾸려면 파일 위쪽의 `Palette`와 격자를 고치고 다시 실행한다.
