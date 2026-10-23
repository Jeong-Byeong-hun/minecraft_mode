# 4차 스킬 이펙트 범위

> 이 문서는 `./gradlew runDatagen`이 코드 정의에서 생성합니다(`SkillLooksDocProvider`). 직접 고치지 마세요.

- **실제 범위**: 스킬 액션의 숫자 그대로(`HitArea`). r = 반경(블록), `at N` = N블록 앞 조준 지점, 원뿔 = 거리 / 각도, 광선·경로 = 길이 x 좌우 폭, (fixed) = 확장 각인으로 커지지 않음. 몬스터는 히트박스가 범위에 닿으면 맞습니다.
- **이펙트 경계**: 그려진 위험 경계(링·벽·돔·원뿔 끝·광선 테두리)가 놓이는 곳 = 렌더러 감사값(`LookEdges`) x 콘텐츠의 look 크기. 하늘·오라·석상 같은 장식은 경계가 아닙니다.
- **일치**: 경계가 실제 범위의 ±10% 안이면 ✅. ◻ = 경계 없음(단일 대상·경로·오라), ◇ = 투사체 몸체가 닿는 반경보다 넓음(빛·형체; 폭발은 따로 그림), ❌ = 어긋남. "그리지 않음" = 범위가 있지만 그 범위를 그리는 이펙트가 없는 단계.
- `JobClientGameTest.checkLookRanges`가 같은 비교로 어긋남이 없는지 확인하고, 실제 범위를 겹쳐 그린 위에서 본 스크린샷 `look_area_<무기>.<스킬>`을 찍습니다.

스킬 252개: 모두 일치 252, 어긋남 0, 일부 범위를 그리지 않음 0.

## 전사

| 무기 | 스킬 | 실제 범위 | 이펙트 경계 | 일치 |
|---|---|---|---|---|
| 엑스칼리버 | 약속된 승리의 검 | beam 24 x ±0.9 | `excalibur` beam 24 x ±0.9 | ✅ |
| 엑스칼리버 | 스트라이크 에어 | push r5<br>shoot hit ±1 | `air_release` ring r5<br>`strike_air` shot ±1 | ✅ ✅ |
| 엑스칼리버 | 마력방출 | dash 8 x ±1.2 | `mana_burst` along the path | ◻ |
| 엑스칼리버 | 아발론의 가호 | - | `avalon_sheath` on the caster | ◻ |
| 엑스칼리버 모르간 | 보티건 | beam 22 x ±0.9 | `vortigern` beam 22 x ±0.9 | ✅ |
| 엑스칼리버 모르간 | 흑마력 방출 | nova r5<br>push r5 | `dark_burst` ring r5 | ✅ |
| 엑스칼리버 모르간 | 폭군의 돌진 | dash 9 x ±1.2 | `tyrant_trail` along the path | ◻ |
| 엑스칼리버 모르간 | 흑기사의 갑주 | - | `black_armor` on the caster | ◻ |
| 롱고미니아드 | 땅끝의 탑 | lightning r5 at 20<br>strike r4 at 20 | `rhongomyniad` ring r5 | ✅ |
| 롱고미니아드 | 폭풍 찌르기 | beam 12 x ±0.9 | `storm_lance` beam 12 x ±0.9 | ✅ |
| 롱고미니아드 | 사자왕의 돌격 | dash 10 x ±1.2 | `lion_charge` along the path | ◻ |
| 롱고미니아드 | 성창의 가호 | ally boost r8 (fixed)<br>ally boost r8 (fixed) | `lance_ward` ring r8 (fixed) | ✅ |
| 아론다이트 | 박쇄전단・과중호광 | slash 5 / 200° | `overload_aura` on the caster<br>`arondight_arc` cone 5 / 200° | ◻ ✅ |
| 아론다이트 | 호수의 기사 | - | `lake_knight` on the caster | ◻ |
| 아론다이트 | 호수 베기 | shoot hit ±1 | `lake_wave` shot ±1.05 | ✅ |
| 아론다이트 | 무결 | - | `unblemished` on the caster | ◻ |
| 클라렌트 | 클라렌트 블러드 아서 | lightning r5 at 16<br>beam 18 x ±0.9 | `red_bolt` ring r5<br>`blood_arthur` beam 18 x ±0.9 | ✅ ✅ |
| 클라렌트 | 붉은 번개 | chain 1 target ≤12 | `red_chain` on the target | ◻ |
| 클라렌트 | 반역의 돌진 | dash 9 x ±1.2 | `rebel_trail` along the path | ◻ |
| 클라렌트 | 부정을 숨기는 투구 | - | `pedigree_helm` on the caster | ◻ |
| 갈라틴 | 전륜하는 승리의 검 | strike r5 at 16 | `galatine_sun` ring r5 | ✅ |
| 갈라틴 | 태양의 축복 | ally boost r10 (fixed)<br>ally boost r10 (fixed) | `sunlight` ring r10 (fixed) | ✅ |
| 갈라틴 | 태양 폭발 | nova r5 | `solar_flare` ring r5 | ✅ |
| 갈라틴 | 정오의 일격 | slash 5 / 150° | `noon_arc` cone 5 / 150° | ✅ |
| 아스칼론 | 그대는 용이니라 | mark 1 target ≤20<br>beam 16 x ±0.9 | `draconis_seal` on the target<br>`holy_lance` beam 16 x ±0.9 | ◻ ✅ |
| 아스칼론 | 용살 찌르기 | beam 10 x ±0.9 | `dragonbane` beam 10 x ±0.9 | ✅ |
| 아스칼론 | 순교자의 서약 | nova r5<br>debuff r10 | `martyr_shield` ring r5 | ✅ |
| 아스칼론 | 성자의 축복 | ally boost r10 (fixed)<br>ally boost r10 (fixed) | `saint_ward` ring r10 (fixed) | ✅ |
| 쿠사나기노츠루기 | 아메노무라쿠모 | pull r8<br>nova r6 | `murakumo` ring r6 | ✅ |
| 쿠사나기노츠루기 | 풀베기 | slash 6 / 300° | `grass_cutter` cone 6 / 300° | ✅ |
| 쿠사나기노츠루기 | 풍보 | dash 10 x ±1.2 | `wind_step` along the path | ◻ |
| 쿠사나기노츠루기 | 야마타의 화근 | shoot hit ±1 | `serpent` shot ±1 | ✅ |
| 원탁의 대방패 | 로드 카멜롯 | nova r6<br>ally boost r12 (fixed)<br>ally boost r12 (fixed) | `camelot` ring r6<br>`ally_ring` ring r12 (fixed) | ✅ ✅ |
| 원탁의 대방패 | 눈꽃의 깃발 | ally boost r8 (fixed) | `snow_bulwark` ring r8 (fixed) | ✅ |
| 원탁의 대방패 | 원탁 강타 | slash 5 / 160° | `round_table` cone 5 / 160° | ✅ |
| 원탁의 대방패 | 카멜롯 돌격 | dash 9 x ±1.2 | `shield_charge` along the path | ◻ |
## 도적

| 무기 | 스킬 | 실제 범위 | 이펙트 경계 | 일치 |
|---|---|---|---|---|
| 카사카의 독니 | 거대 독사의 소굴 | nova r6 | `serpent_lair` ring r6 | ✅ |
| 카사카의 독니 | 마비의 이빨 | shadowstep 1 target ≤12 | `venom_bite` on the target | ◻ |
| 카사카의 독니 | 독안개 | zone r5 at 14 | `venom_mist` ring r5 | ✅ |
| 카사카의 독니 | 질주 | - | `sprint_trail` along the path | ◻ |
| 나이트 킬러 | 기사의 재앙 | mark 1 target ≤16<br>shadowstep 1 target ≤16 | `knights_bane` on the target | ◻ |
| 나이트 킬러 | 난도 | slash 3.5 / 140° | `mutilation` cone 3.5 / 140° | ✅ |
| 나이트 킬러 | 살기 | debuff r8<br>debuff r8 | `killing_intent` ring r8 | ✅ |
| 나이트 킬러 | 되받아치기 | - | `riposte` on the caster | ◻ |
| 베르의 발톱 | 개미왕의 폭주 | dash 8 x ±1.2<br>nova r5 | `ant_dash` along the path<br>`ant_frenzy` ring r5 | ◻ ✅ |
| 베르의 발톱 | 포식 | execute 1 target ≤5 | `devour` on the target | ◻ |
| 베르의 발톱 | 빼앗은 비술 | ally boost r10 (fixed)<br>ally boost r10 (fixed) | `stolen_healing` ring r10 (fixed) | ✅ |
| 베르의 발톱 | 날개 돌격 | leap r4 on landing | `wing_burst` ring r4 | ✅ |
| 비뢰신 쿠나이 | 금빛 섬광 | shadowstep 1 target ≤20<br>nova r5 | `yellow_flash` on the target<br>`rasengan` on the caster<br>`rasengan_burst` ring r5 | ◻ ◻ ✅ |
| 비뢰신 쿠나이 | 술식 쿠나이 | shoot hit ±0.3<br>mark 1 target ≤20 | `formula_kunai` shot ±0.3 | ✅ |
| 비뢰신 쿠나이 | 비뢰신의 술 | - | `hiraishin` along the path | ◻ |
| 비뢰신 쿠나이 | 시공간 결계 | - | `space_time_barrier` on the caster | ◻ |
| 토츠카의 검 | 스사노오 | beam 16 x ±0.9 | `susanoo_itachi` on the caster<br>`totsuka` beam 16 x ±0.9 | ◻ ✅ |
| 토츠카의 검 | 아마테라스 | zone r3.5 at 16 | `black_flames` ring r3.5 | ✅ |
| 토츠카의 검 | 츠쿠요미 | beam 16 x ±0.9 | `tsukuyomi` beam 16 x ±0.9 | ✅ |
| 토츠카의 검 | 까마귀 분신술 | - | `crow_clone` on the caster | ◻ |
| 그림자 군주의 쿠사리가마 | 군주의 영역 | nova r8<br>zone r8 | `monarch_domain` ring r8 | ✅ |
| 그림자 군주의 쿠사리가마 | 지배자의 권능 | launch r7<br>nova r7 | `rulers_authority` ring r7 | ✅ |
| 그림자 군주의 쿠사리가마 | 사신의 사슬 | slash 7 / 240° | `reaper_chain` cone 7 / 240° | ✅ |
| 그림자 군주의 쿠사리가마 | 심연의 무장 | - | `abyssal_guard` on the caster | ◻ |
| 카무이 수리검 | 완성체 스사노오: 카무이 수리검 | shoot hit ±0.3, blast r4 | `kamui_shuriken` shot ±0.93<br>`kamui_vortex` burst r4 (fixed) | ◇ ✅ |
| 카무이 수리검 | 자전 | chain 1 target ≤16 | `purple_chain` on the target | ◻ |
| 카무이 수리검 | 카무이 | - | `kamui_warp` on the caster | ◻ |
| 카무이 수리검 | 카무이 뇌절 | nova r3.5 | `kamui_raikiri` ring r3.5 | ✅ |
| 카미쉬의 분노 | 일어나라 | nova r7 | `arise` ring r7 | ✅ |
| 카미쉬의 분노 | 드래곤 피어 | debuff r10<br>debuff r10 | `dragon_fear` ring r10 | ✅ |
| 카미쉬의 분노 | 용아 투척 | shoot hit ±0.3 | `dragon_fang` shot ±0.3 | ✅ |
| 카미쉬의 분노 | 그림자 교환 | - | `shadow_exchange` along the path | ◻ |
| 마다라의 군배 | 우치하 되돌리기 | nova r7 | `uchiha_return` ring r7 | ✅ |
| 마다라의 군배 | 호화멸각 | strike r5 at 12 | `destroyer_flame` ring r5 | ✅ |
| 마다라의 군배 | 군배 휩쓸기 | slash 6 / 180°<br>push r5 | `gunbai_sweep` cone 6 / 180° | ✅ |
| 마다라의 군배 | 군배 도약 | leap r4 on landing | `gunbai_leap` ring r4 | ✅ |
## 법사

| 무기 | 스킬 | 실제 범위 | 이펙트 경계 | 일치 |
|---|---|---|---|---|
| 콜키스의 홀 | 헤카틱 위저드 | rain r7 at 24 | `hecatic_sky` ring r7 | ✅ |
| 콜키스의 홀 | 파계해야 할 모든 부적 | shadowstep 1 target ≤10 | `rule_breaker` on the target | ◻ |
| 콜키스의 홀 | 고속신언 | - | `divine_words` on the caster | ◻ |
| 콜키스의 홀 | 황금양모 | - | `golden_fleece` on the caster | ◻ |
| 야타노카가미 | 수천일광천조팔야진석 | ally boost r12 (fixed)<br>ally buff r12 (fixed) | `amaterasu` ring r12 (fixed) | ✅ |
| 야타노카가미 | 염천 | strike r3.5 at 18 | `enten` ring r3.5 | ✅ |
| 야타노카가미 | 빙천 | rain r4 at 18 | `hyoten` ring r4 | ✅ |
| 야타노카가미 | 변화 | - | `fox_fire` on the caster | ◻ |
| 메구밍의 지팡이 | 익스플로전! | strike r8 at 24 | `explosion` ring r8 | ✅ |
| 메구밍의 지팡이 | 드레인 터치 | nova r3.5 | `drain_touch` ring r3.5 | ✅ |
| 메구밍의 지팡이 | 홍마족의 포즈 | - | `crimson_pose` on the caster | ◻ |
| 메구밍의 지팡이 | 마나타이트 | - | `manatite` on the caster | ◻ |
| 와룡의 병법서 | 석병팔진 | zone r8 at 20 | `stone_maze` ring r8 | ✅ |
| 와룡의 병법서 | 적벽의 동남풍 | push r4<br>shoot hit ±0.3, blast r2 | `east_wind` on the caster<br>`wind_fire` shot ±0.45 | ◻ ✅ |
| 와룡의 병법서 | 감식안 | mark 1 target ≤24<br>ally boost r10 (fixed) | `discerning_eye` on the target<br>`ally_ring` ring r10 (fixed) | ◻ ✅ |
| 와룡의 병법서 | 군사의 충언 | ally boost r10 (fixed)<br>ally boost r10 (fixed) | `war_banner` ring r10 (fixed) | ✅ |
| 루멘 이스트왈 | 페어리 로 | debuff r16<br>nova r16 | `fairy_law` ring r16 | ✅ |
| 루멘 이스트왈 | 페어리 글리터 | shoot hit ±0.3, blast r3.5 | `fairy_glitter` shot ±0.9<br>`glitter_burst` burst r3.5 (fixed) | ◇ ✅ |
| 루멘 이스트왈 | 페어리 스피어 | ally boost r10 (fixed) | `fairy_sphere` on the caster<br>`ally_ring` ring r10 (fixed) | ◻ ✅ |
| 루멘 이스트왈 | 요정군사 | ally buff r12 (fixed)<br>ally buff r12 (fixed) | `fairy_wings` on the caster<br>`ally_ring` ring r12 (fixed) | ◻ ✅ |
| 멀린의 지팡이 | 영원히 닫힌 이상향 | ally boost r10 (fixed)<br>ally boost r10 (fixed) | `avalon` ring r10 (fixed) | ✅ |
| 멀린의 지팡이 | 영웅 작성 | ally boost r10 (fixed)<br>ally boost r10 (fixed) | `hero_creation` ring r10 (fixed) | ✅ |
| 멀린의 지팡이 | 꽃보라 | shoot hit ±0.3 | `petal_orb` shot ±0.3 | ✅ |
| 멀린의 지팡이 | 환술 | - | `illusion` along the path | ◻ |
| 프리렌의 지팡이 | 졸트라크 일제 사격 | barrage hit ±0.3 | - | - |
| 프리렌의 지팡이 | 쥬드라질룸 | lightning r4.5 at 22 | `judradjim` ring r4.5 | ✅ |
| 프리렌의 지팡이 | 방어 마법 | - | `hex_barrier` on the caster | ◻ |
| 프리렌의 지팡이 | 꽃밭을 만드는 마법 | ally boost r7 (fixed)<br>ally boost r7 (fixed) | `flower_field` ring r7 (fixed) | ✅ |
| 레메게톤 | 아르스 알마델 살로모니스 | strike r7 at 24 | `ars_almadel` ring r7 | ✅ |
| 레메게톤 | 마신주 강림 | launch r6<br>nova r6 | `demon_pillars` ring r6 | ✅ |
| 레메게톤 | 천리안 | mark 1 target ≤24<br>debuff r14 | `clairvoyance` on the target | ◻ |
| 레메게톤 | 솔로몬의 반지 | - | `ten_rings` on the caster | ◻ |
| 룰 브레이커 | 파계해야 할 모든 부적 | mark 1 target ≤20<br>beam 14 x ±0.9 | `spell_break` beam 14 x ±1.05 | ✅ |
| 룰 브레이커 | 고속신언 | barrage hit ±0.3 | - | - |
| 룰 브레이커 | 마녀의 단검 | slash 4 / 180° | `witch_slash` cone 4 / 180° | ✅ |
| 룰 브레이커 | 진지작성 | ally boost r10 (fixed) | `territory` ring r10 (fixed) | ✅ |
## 궁수

| 무기 | 스킬 | 실제 범위 | 이펙트 경계 | 일치 |
|---|---|---|---|---|
| 케이론의 대궁 | 천갈일사 | mark 1 target ≤24<br>shoot hit ±0.3 | `antares_mark` on the target<br>`star_arrow` shot ±0.4 | ◻ ✅ |
| 케이론의 대궁 | 판크라티온 | slash 4 / 140°<br>push r4 | `pankration` cone 4 / 140° | ✅ |
| 케이론의 대궁 | 화살 회피의 가호 | - | `arrow_ward` on the caster | ◻ |
| 케이론의 대궁 | 켄타우로스의 질주 | dash 12 x ±1.2 | `gallop` along the path | ◻ |
| 간장·막야 (얼터) | 언리미티드 로스트 워크스 | shoot hit ±0.3<br>rain r5 at 14 | `tracer` shot ±0.25<br>`ulw_field` ring r5 | ✅ ✅ |
| 간장·막야 (얼터) | 건 카타 | slash 4 / 360°<br>shoot hit ±0.3 | `gun_kata` ring r4<br>`tracer` shot ±0.25 | ✅ ✅ |
| 간장·막야 (얼터) | 카운터 가디언 | - | `counter_guardian` on the caster | ◻ |
| 간장·막야 (얼터) | 잔상 사격 | shoot hit ±0.3 | `flash_trail` along the path<br>`tracer` shot ±0.25 | ◻ ✅ |
| 아폴론의 활 | 헤카테볼로스 | beam 26 x ±0.9 | `sun_beam` beam 26 x ±0.9 | ✅ |
| 아폴론의 활 | 아킬레스건 | execute 1 target ≤24 | `heel_strike` on the target | ◻ |
| 아폴론의 활 | 파이안 | ally boost r10 (fixed)<br>ally boost r10 (fixed) | `paean` ring r10 (fixed) | ✅ |
| 아폴론의 활 | 햇살 걸음 | - | `sun_step` along the path | ◻ |
| 아르테미스의 활 | 트라이스타 아모레 미오 | shoot hit ±0.3 | `moon_arrow` shot ±0.32 | ✅ |
| 아르테미스의 활 | 초승달 연사 | shoot hit ±0.3 | `crescent_arrow` shot ±0.34 | ✅ |
| 아르테미스의 활 | 달의 사냥개 | debuff r12 | `moon_sigil` ring r12 | ✅ |
| 아르테미스의 활 | 월광의 장막 | - | `moon_veil` on the caster | ◻ |
| 간디바 | 파괴신의 손짓 | lightning r6 at 24 | `pashupata_bolt` ring r6 | ✅ |
| 간디바 | 아그니 간디바 | shoot hit ±0.3, blast r2 | `flame_arrow` shot ±0.35<br>`flame_burst` burst r2 (fixed) | ✅ ✅ |
| 간디바 | 축복받는 영웅 | - | `endowed` on the caster | ◻ |
| 간디바 | 마력방출(불꽃) | dash 10 x ±1.2 | `mana_flame` along the path | ◻ |
| 마안나 | 앙갈타 키갈슈 | strike r6 at 24 | `venus_star` ring r6 | ✅ |
| 마안나 | 보석 사격 | shoot hit ±0.3 | `gem` shot ±0.3 | ✅ |
| 마안나 | 미의 현현 | ally boost r10 (fixed)<br>debuff r10 | `beauty` ring r10 | ✅ |
| 마안나 | 여신의 비행 | - | `venus_wings` on the caster | ◻ |
| 왕의 재보 | 왕의 재보: 전면 개방 | barrage hit ±0.3<br>rain r6 at 20 | `babylon_sky` ring r6 | ✅ |
| 왕의 재보 | 보구 사출 | barrage hit ±0.3 | - | - |
| 왕의 재보 | 하늘의 사슬 | pull r10<br>debuff r10<br>debuff r10 | `enkidu` ring r10 | ✅ |
| 왕의 재보 | 보물고의 무구 | - | `treasury_shields` on the caster | ◻ |
| 에아 & 메로닥 | 에누마 엘리시 | pull r10<br>beam 32 x ±0.9 | `ea_charge` ring r10<br>`enuma_elish` beam 32 x ±0.9 | ✅ ✅ |
| 에아 & 메로닥 | 원죄 (메로닥) | slash 5 / 200°<br>shoot hit ±1 | `slash` cone 5 / 200°<br>`sin_wave` shot ±1 | ✅ ✅ |
| 에아 & 메로닥 | 모든 것을 본 자 | debuff r16<br>mark 1 target ≤24 | `sha_naqba` ring r16 | ✅ |
| 에아 & 메로닥 | 황금률(몸) | - | `golden_rule` on the caster | ◻ |
| 파슈파타 | 파슈파타 | mark 1 target ≤20<br>strike r5 at 20 | `pashupata_disc` ring r5 | ✅ |
| 파슈파타 | 신성한 원반 | shoot hit ±0.3 | `discus` shot ±0.4 | ✅ |
| 파슈파타 | 마력방출(불꽃) | nova r5 | `flame_burst` ring r5 | ✅ |
| 파슈파타 | 축복받는 영웅 | - | `endowed` on the caster | ◻ |
## 해적

| 무기 | 스킬 | 실제 범위 | 이펙트 경계 | 일치 |
|---|---|---|---|---|
| 배틀쉽 캐논 | 배틀쉽 | shoot hit ±0.3, blast r3 | `battleship` on the caster<br>`cannon_shot` shot ±0.45<br>`cannon_blast` burst r3 (fixed) | ◻ ✅ ✅ |
| 배틀쉽 캐논 | 옥토퍼스 | zone r4 at 12 | `octopus` ring r4 | ✅ |
| 배틀쉽 캐논 | 에어 스트라이크 | strike r3.5 at 20 | `air_strike` ring r3.5 | ✅ |
| 배틀쉽 캐논 | 불스아이 | mark 1 target ≤24 | `bullseye` on the target | ◻ |
| 엘 드라케 | 황금 사슴과 폭풍의 밤 | barrage hit ±0.3<br>strike r5 at 22 | `wild_hunt` on the caster<br>`golden_hind` ring r5 | ◻ ✅ |
| 엘 드라케 | 황금률 | shoot hit ±0.3 | `coin_shot` shot ±0.24 | ✅ |
| 엘 드라케 | 폭풍의 항해자 | ally boost r10 (fixed)<br>ally buff r10 (fixed) | `storm_voyage` ring r10 (fixed) | ✅ |
| 엘 드라케 | 별의 개척자 | dash 10 x ±1.2 | `star_wake` along the path | ◻ |
| 앤과 메리의 머스킷 | 비익이자 연리 | shadowstep 1 target ≤16<br>shoot hit ±0.3 | `free_bird` on the target<br>`bird_shot` shot ±0.7 | ◻ ◇ |
| 앤과 메리의 머스킷 | 메리의 커틀러스 | slash 4.5 / 160° | `cutlass_arc` cone 4.5 / 160° | ✅ |
| 앤과 메리의 머스킷 | 앤의 화약 사격 | shoot hit ±0.3, blast r2 | `musket_shot` shot ±0.3<br>`cannon_blast` burst r2 (fixed) | ✅ ✅ |
| 앤과 메리의 머스킷 | 해적의 명예 | - | `twin_birds` on the caster | ◻ |
| 플라잉 더치맨의 작살 | 크라켄을 풀어라 | zone r6 at 16<br>strike r5 at 16 | `kraken` ring r6<br>`kraken_slam` ring r5 | ✅ ✅ |
| 플라잉 더치맨의 작살 | 촉수 채찍 | pull r6<br>slash 5 / 180° | `tentacle_lash` cone 5 / 180° | ✅ |
| 플라잉 더치맨의 작살 | 죽음이 두려운가? | debuff r10<br>debuff r10 | `davy_jones` ring r10 | ✅ |
| 플라잉 더치맨의 작살 | 심해에서 떠오르다 | - | `deep_rise` along the path | ◻ |
| 나폴레옹 | 위국 | shoot hit ±1<br>push r6 | `ikoku_swing` ring r6<br>`ikoku_wave` shot ±1.1 | ✅ ✅ |
| 나폴레옹 | 소울 포커스 | nova r6 | `soul_pocus` ring r6 | ✅ |
| 나폴레옹 | 제우스: 뇌정 | lightning r4 at 18 | `zeus` ring r4 | ✅ |
| 나폴레옹 | 크로캉부슈 발작 | - | `croquembouche` on the caster | ◻ |
| 모비딕호의 닻 | 섬 흔들기 | nova r8<br>launch r8 | `quake` ring r8 | ✅ |
| 모비딕호의 닻 | 해진 | shoot hit ±1 | `tsunami` shot ±1.1 | ✅ |
| 모비딕호의 닻 | 닻 내리기 | leap r5 on landing | `anchor_drop` ring r5 | ✅ |
| 모비딕호의 닻 | 내 아들들 | ally boost r12 (fixed)<br>ally boost r12 (fixed)<br>ally boost r12 (fixed) | `my_sons` ring r12 (fixed) | ✅ |
| 니카의 너클 | 고무고무 바즈랑 건 | strike r6 at 20 | `bajrang_gun` ring r6 | ✅ |
| 니카의 너클 | 고무고무 레드 호크 | beam 10 x ±0.9 | `red_hawk` beam 10 x ±0.9 | ✅ |
| 니카의 너클 | 고무고무 제트 개틀링 | slash 4 / 100° | `jet_gatling` cone 4 / 100° | ✅ |
| 니카의 너클 | 해방의 드럼 | - | `liberation_drums` on the caster | ◻ |
| 해적왕의 검 | 카무사리 | slash 6 / 180°<br>shoot hit ±1 | `kamusari` cone 6 / 180°<br>`kamusari_wave` shot ±1.05 | ✅ ✅ |
| 해적왕의 검 | 패왕색 충돌 | nova r6<br>push r8 | `haki_clash` ring r6 | ✅ |
| 해적왕의 검 | D의 의지 | - | `will_of_d` on the caster | ◻ |
| 해적왕의 검 | 대해적시대의 개막 | ally boost r12 (fixed)<br>ally boost r12 (fixed) | `pirate_era` ring r12 (fixed) | ✅ |
| 벽력 나팔총 | 벽력 일제사격 | rain r6 at 18 | `broadside` ring r6 | ✅ |
| 벽력 나팔총 | 산탄 폭풍 | slash 6 / 100° | `buckshot` cone 6 / 100° | ✅ |
| 벽력 나팔총 | 무장색 탄환 | shoot hit ±0.3 | `haki_shot` shot ±0.3 | ✅ |
| 벽력 나팔총 | 반동 도약 | push r4<br>dash 8 x ±1.2 | `recoil` ring r4 | ✅ |
## 사신

| 무기 | 스킬 | 실제 범위 | 이펙트 경계 | 일치 |
|---|---|---|---|---|
| 천쇄참월 | 월아천충 | shoot hit ±1 | `getsuga` shot ±1.05 | ✅ |
| 천쇄참월 | 만해의 속도 | dash 6 x ±1.2 | `flash_trail` along the path | ◻ |
| 천쇄참월 | 검은 월아천충 | shoot hit ±1 | `getsuga` shot ±1.05 | ✅ |
| 천쇄참월 | 호로화 | - | `reiatsu` on the caster | ◻ |
| 천본앵경엄 | 천 개의 칼날 | nova r6 | `petal_storm` ring r6 | ✅ |
| 천본앵경엄 | 항경 | pull r8<br>nova r6 | `petal_sphere` ring r6 | ✅ |
| 천본앵경엄 | 섬경 | zone r6 | `senkei` ring r6 | ✅ |
| 천본앵경엄 | 종경 백제검 | beam 26 x ±0.9<br>dash 8 x ±1.2 | `hakuteiken` beam 26 x ±0.9<br>`flash_trail` along the path | ✅ ◻ |
| 대홍련빙륜환 | 용산가 | shadowstep 1 target ≤10 | `ice_flower` on the target | ◻ |
| 대홍련빙륜환 | 천년빙뢰 | rain r4 at 16 | `ice_prison` ring r4 | ✅ |
| 대홍련빙륜환 | 얼음 날개 | leap r3 on landing | `ice_wings` on the caster<br>`landing_ring` ring r3 | ◻ ✅ |
| 대홍련빙륜환 | 빙천백화장 | zone r7 at 14 | `ice_bloom` ring r7 | ✅ |
| 류인약화 | 만상일체 잿더미가 되어라 | nova r6 | `flame_burst` ring r6 | ✅ |
| 류인약화 | 성곽염상 | zone r6 | `flame_wall` ring r6 | ✅ |
| 류인약화 | 횃불 | shoot hit ±0.3, blast r4 | `taimatsu` shot ±0.85<br>`flame_burst` burst r4 (fixed) | ◇ ✅ |
| 류인약화 | 잔화태도 | slash 8 / 360° | `zanka` ring r8 | ✅ |
| 신살창 | 13킬로미터 | beam 30 x ±0.9 | `spear` beam 30 x ±0.9 | ✅ |
| 신살창 | 무답연인 | beam 20 x ±0.9 | `spear` beam 20 x ±0.9 | ✅ |
| 신살창 | 먼지로 | debuff r8<br>mark 1 target ≤20 | `dust` ring r8 | ✅ |
| 신살창 | 죽여라, 신살창 | beam 30 x ±0.9 | `spear_kill` beam 30 x ±0.9 | ✅ |
| 경화수월 | 완전 최면 | debuff r8 | `mirror` ring r8 | ✅ |
| 경화수월 | 파도의 90 흑관 | zone r4 at 16 | `kurohitsugi` ring r4 | ✅ |
| 경화수월 | 환영 보법 | - | `mirror` ring r1, no area | ◻ |
| 경화수월 | 붕옥 | nova r6 | `hogyoku` ring r6 | ✅ |
| 화천광골 고송심중 | 첫째 단 주저자분합 | slash 5 / 200° | `slash` cone 5 / 200° | ✅ |
| 화천광골 고송심중 | 둘째 단 참괴의 깔개 | slash 4 / 160° | `cross_slash` cone 4 / 160° | ✅ |
| 화천광골 고송심중 | 셋째 단 단어연 | zone r6 at 10 | `abyss` ring r6 | ✅ |
| 화천광골 고송심중 | 마지막 단 사절협혈염후 | execute 1 target ≤6 | `binding` on the target | ◻ |
| 진짜 참월 | 월아천충 | shoot hit ±1 | `getsuga_dual` shot ±1.05 | ✅ |
| 진짜 참월 | 쌍 월아천충 | shoot hit ±1<br>slash 5 / 180° | `getsuga_dual` shot ±1.05<br>`slash` cone 5 / 180° | ✅ ✅ |
| 진짜 참월 | 최후의 순보 | - | `flash_trail` along the path | ◻ |
| 진짜 참월 | 무월 | zone r8 at 12<br>beam 30 x ±0.9 | `mugetsu_void` ring r8<br>`mugetsu` beam 30 x ±0.9 | ✅ ✅ |
| 쌍왕 사미환 | 비아절교 | pull r6<br>nova r6 | `bone_fangs` ring r6 | ✅ |
| 쌍왕 사미환 | 사아철포 | beam 16 x ±0.9 | `bone_snake` beam 16 x ±0.9 | ✅ |
| 쌍왕 사미환 | 쌍왕 채찍 | slash 7 / 220° | `bone_lash` cone 7 / 220° | ✅ |
| 쌍왕 사미환 | 비골 갑주 | - | `bone_cage` on the caster | ◻ |
## 헌터

| 무기 | 스킬 | 실제 범위 | 이펙트 경계 | 일치 |
|---|---|---|---|---|
| 신속 | 전광석화 | shadowstep 1 target ≤12 | `godspeed_step` along the path | ◻ |
| 신속 | 질풍신뢰 | - | `godspeed_aura` on the caster | ◻ |
| 신속 | 낙뢰 | lightning r3 at 16 | `thunderbolt` ring r3 | ✅ |
| 신속 | 나루카미 | chain 1 target ≤14<br>lightning r4 at 14 | `narukami` on the target<br>`thunderbolt` ring r4 | ◻ ✅ |
| 엠퍼러 타임 | 저지먼트 체인 | beam 14 x ±0.9 | `judgment_chain` beam 14 x ±0.9 | ✅ |
| 엠퍼러 타임 | 스틸 체인 | debuff r8 | `steal_chain` ring r8 | ✅ |
| 엠퍼러 타임 | 체인 제일 | zone r4 at 12 | `chain_jail` ring r4 | ✅ |
| 엠퍼러 타임 | 붉은 눈 | - | `scarlet_eyes` on the caster | ◻ |
| 어른 곤 | 분노의 서약 | - | `gon_rage` on the caster | ◻ |
| 어른 곤 | 처음에는 주먹 | leap r4 on landing<br>launch r4 | `jajanken_rock` ring r4 | ✅ |
| 어른 곤 | 긴 가위 | beam 18 x ±0.9 | `long_scissors` beam 18 x ±0.9 | ✅ |
| 어른 곤 | 이것으로 끝나도 좋아 | nova r7 | `gon_rock` ring r7<br>`rock_blast` ring r7 | ✅ ✅ |
| 도적의 극의 | 한 쌍의 파괴자 | mark 1 target ≤18<br>chain 1 target ≤14 | `sun_moon` on the target<br>`seal_chain` on the target | ◻ ◻ |
| 도적의 극의 | 밀실유어 | shoot hit ±0.3 | `indoor_fish` shot ±0.3 | ✅ |
| 도적의 극의 | 불가사의하고 편리한 보자기 | - | `fun_cloth` on the caster | ◻ |
| 도적의 극의 | 도적의 비법 | barrage hit ±0.3 | `bandit_book` on the caster | ◻ |
| 제노의 드래곤 헤드 | 드래곤 헤드 | shoot hit ±1, blast r2.5 | `dragon_head` shot ±1<br>`dragon_burst` burst r2.5 (fixed) | ✅ ✅ |
| 제노의 드래곤 헤드 | 드래곤 랜스 | beam 20 x ±0.9 | `dragon_lance` beam 20 x ±0.9 | ✅ |
| 제노의 드래곤 헤드 | 조르딕가의 인내 | - | `zeno_aura` on the caster | ◻ |
| 제노의 드래곤 헤드 | 드래곤 다이브 | rain r7 at 24 | `dragon_dive` ring r7 | ✅ |
| 실버의 넨구 | 아우라 구 | shoot hit ±0.3, blast r3 | `silva_orb` shot ±0.32<br>`orb_burst` burst r3 (fixed) | ✅ ✅ |
| 실버의 넨구 | 넨구 연사 | shoot hit ±0.3, blast r1.5 | `silva_orb` shot ±0.32<br>`orb_burst` burst r1.5 (fixed) | ✅ ✅ |
| 실버의 넨구 | 가주의 위엄 | debuff r10<br>debuff r10 | `silva_presence` ring r10 | ✅ |
| 실버의 넨구 | 암살가의 심판 | execute 1 target ≤8<br>shoot hit ±0.3, blast r3 | `assassin_hand` on the target<br>`silva_orb` shot ±0.32<br>`orb_burst` burst r3 (fixed) | ◻ ✅ ✅ |
| 메르엠의 꼬리 | 꼬리 관통 | beam 10 x ±0.9 | `tail_pierce` beam 10 x ±0.9 | ✅ |
| 메르엠의 꼬리 | 광자 | nova r6 | `photon` ring r6 | ✅ |
| 메르엠의 꼬리 | 왕의 아우라 | debuff r12<br>debuff r12 | `kings_aura` ring r12 | ✅ |
| 메르엠의 꼬리 | 키메라 앤트의 왕 | nova r7 | `king_rose` ring r7 | ✅ |
| 백식관음 | 기도 | - | `netero_prayer` on the caster | ◻ |
| 백식관음 | 삼내장 | slash 8 / 200°<br>launch r6 | `guanyin_sweep` cone 8 / 200° | ✅ |
| 백식관음 | 구십구장 | nova r7 | `palm_slam` ring r7 | ✅ |
| 백식관음 | 제로 | zone r9 at 6<br>beam 30 x ±0.9 | `zero_hand` ring r9<br>`zero_beam` beam 30 x ±0.9 | ✅ ✅ |
| 이르미의 침 | 천침 | rain r6 at 16 | `needle_rain` ring r6 | ✅ |
| 이르미의 침 | 침 투척 | shoot hit ±0.3 | `nen_needle` shot ±0.3 | ✅ |
| 이르미의 침 | 침 변장 | - | `needle_disguise` on the caster | ◻ |
| 이르미의 침 | 조르딕가의 암살술 | execute 1 target ≤5 | `needle_puppet` on the target | ◻ |

