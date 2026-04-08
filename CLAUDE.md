# 에르피스 헬퍼 앱 - 안드로이드 프론트엔드

## 프로젝트 개요
에르피스(Ehrpis) 모바일 게임 헬퍼 앱.
**현재 작업: 쿠폰 알리미 안드로이드 앱 UI 구현**

## 앱 기본 정보
- 패키지명: `com.loxa.ehrpishelper`
- 레포: Ehrpis-helper-backend (백엔드), 안드로이드 레포 별도
- 언어: Kotlin + Jetpack Compose
- 최소 SDK: 26

## 완료된 것
- Firebase 프로젝트: `ehrpis-push`
- FCM 토픽 구독: `coupons` 토픽 구독 완료
- 푸시 알림 수신 확인 완료
- 알림 권한 요청 (Android 13+) 구현
- jsDelivr CDN에서 캐릭터 JSON 수신 확인 (Retrofit)

## 현재 MainActivity.kt 상태
```kotlin
class MainActivity : ComponentActivity() {
  // 알림 권한 요청 launcher 있음
  // FCM coupons 토픽 구독 완료
  // CouponScreen() - 현재 텍스트만 표시
}
```

## FCM Data Payload 구조
```json
{
  "route": "coupon_list",
  "feed_id": "7508947",
  "coupons": "GIFTS0406",
  "expiry_end": "2026-04-08 23:59",
  "link": "https://game.naver.com/lounge/Ehrpis/board/detail/7508947"
}
```
- `click_action`: `OPEN_COUPON_LIST`
- FCM topic: `coupons`

## Firestore 쿠폰 데이터 구조
컬렉션: `coupons`
```
feed_id: int
title: string
coupons: array[string]
expiry_start: string ("2026-04-06")
expiry_end: string ("2026-04-08 23:59")
link: string
created_date: string
notified: bool
```

## CDN 데이터
베이스 URL: `https://cdn.jsdelivr.net/gh/LoxaLovecarstone/Ehrpis-helper-backend@main/`
- 캐릭터 목록: `data/characters/index.json`
- 개별 캐릭터: `data/characters/{gamekee_id}.json`
- 공통 코드: `data/common/classes.json`, `elements.json`, `roles.json`, `badges.json`

## 의존성 (현재 추가된 것)
```kotlin
implementation(platform("com.google.firebase:firebase-bom:34.11.0"))
implementation("com.google.firebase:firebase-messaging")
implementation("com.google.firebase:firebase-firestore")
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
```

## 앱 구조 계획
- 바텀 네비게이션 2탭: 쿠폰 목록 / 캐릭터 정보
- 쿠폰 목록 화면:
  - Firestore에서 쿠폰 목록 실시간 구독
  - 쿠폰 코드 탭하면 클립보드 복사
  - 만료된 쿠폰 흐리게 표시
  - 알림 클릭 시 쿠폰 목록 화면으로 이동
- 캐릭터 정보 화면: (추후 구현)

## 지금 당장 할 것
1. FirebaseMessagingService 구현 (포그라운드 알림 처리)
2. 바텀 네비게이션 구조 잡기
3. Firestore 쿠폰 목록 화면 구현
4. 쿠폰 코드 복사 기능
5. 알림 클릭 → 쿠폰 목록 화면 이동

---

## 딜 계산기 관련 데이터 구조

### 딜 계산 공식
```
최종 공격력 = (base_atk + 각인 보정 + 각성 flat 보정) × (1 + 뱃지 atk_percent 합산 + 각성 percent 보정)

스킬 딜 = 최종 공격력 (or 기본 공격력, base_type 따라)
          × skill.multipliers.ratio
          × skill.hits
          × 속성 상성 (element_relations)
          × 치명타 보정 (1 + crit_rate × crit_dmg)
          × 스킬 타입별 뱃지 보정 (용맹/광폭/불꽃/...)
          × 피버 상태 보정 (fever_config.atk_spd_bonus)
```

### base_type 구분
- `total`: 뱃지/각성/버프 포함 최종 공격력 기준
- `base`: 기본 공격력만 (버프 미포함) → 일부 스킬이 기본 공격력에 비례

### 뱃지 effect_type 종류
- `stat_bonus`: 스탯 직접 증가 (target_stat: atk_percent, hp_percent 등)
- `skill_dmg`: 특정 스킬 타입 피해 증가 (target_skill_type: normal/battle/fever)
- `team_dmg_bonus`: 특정 직업 아군 전체 피해 증가 (target_class_id 참조)
- `dmg_reduction`: 피해 감소
- `dmg_bonus_stack`: 중첩형 피해 증가
- `dot_percent`: 지속 피해 증가
- `heal_percent`: 회복 효과 증가

### 캐릭터 스킬 구조
```json
{
  "id": "638318_battle",
  "type": "battle",          // normal / battle / fever
  "hits": 1,                 // 타격 횟수
  "multipliers": [
    {
      "stat": "atk",
      "base_type": "total",  // total or base
      "ratio": 1.00          // MAX 기준 배율
    }
  ],
  "effects": []              // 부가 효과 (heal, dot, shield, cc 등)
}
```

### 패시브 처리
패시브는 `character.passives` 배열에 있으며 항상 적용:
```kotlin
val defBonus = character.passives
    .filter { it.target_stat == "def" && it.value_type == "flat" }
    .sumOf { it.value }
```

### 각성 처리
유저가 선택한 각성 단계까지만 적용:
```kotlin
val hpBonus = character.awakenings
    .filter { it.step <= userAwakeningStep }
    .filter { it.effect_type == "stat_bonus" && it.target_stat == "hp" }
    .sumOf { it.value }
```

### 유저 육성 데이터 (Room DB 로컬 저장)
```kotlin
@Entity
data class UserCharacter(
    @PrimaryKey val characterId: Int,
    val awakeningStep: Int,        // 0~5
    val equippedBadgeId: String,   // badges.json의 id
    val equippedBadgeCount: Int,   // 2 or 3
    val customAtk: Int?,           // 유저 직접 입력 (null이면 stats.atk_max 사용)
    val customHp: Int?,
    val customDef: Int?
)
```

### 속성 상성
`element_relations.json` 참조.
매칭 없으면 기본값 1.0 적용:
```kotlin
val elementMultiplier = elementRelations
    .find { it.attacker_element_id == attackerElementId && it.defender_element_id == defenderElementId }
    ?.multiplier ?: 1.0
```

### 피버 상태
`fever_config.json` 참조.
피버 진입 시 공속 보정 적용 → DPS 재계산 필요.


# 캐릭터 정보 인덱스 예시
[
{
"id": 638318,
"name_ko": "고스트 사무라이",
"name_en": "Ghostsamurai",
"name_cn": "鬼侍",
"rarity": 5,
"class_id": 1,
"element_id": 2,
"role_id": 2,
"icon_url": ""
}
]

# 캐릭터 상세 정보 예시
{
"id": 638318,
"name_ko": "고스트 사무라이",
"name_en": "Ghostsamurai",
"name_cn": "鬼侍",
"rarity": 5,
"class_id": 1,
"element_id": 2,
"role_id": 2,
"icon_url": "",
"stats": {
"atk": 43,  "atk_max": 762,
"def": 56,  "def_max": 835,
"hp": 3112, "hp_max": 17937,
"crit_rate": 0.05, "crit_dmg": 0.30,
"block_rate": 0.05, "block_reduction": 0.30,
"dodge_rate": 0.0,
"move_spd": 90, "atk_spd": 2567, "range": 50, "heal_power": 0.0
},
"skills": [
{
"id": "638318_normal",
"type": "normal",
"name": "강타",
"hits": 1,
"multipliers": [{ "stat": "atk", "base_type": "total", "ratio": 1.00 }],
"effects": []
},
{
"id": "638318_battle",
"type": "battle",
"name": "근원 장악",
"hits": 0,
"multipliers": [],
"effects": [
{
"type": "heal",
"target": "ally_lowest_hp",
"condition": "ally_hp_below_50",
"stat": "hp",
"base_type": "total",
"ratio": 0.065,
"flat": 1000,
"max_charges": 3,
"charge_interval_sec": 7
}
]
},
{
"id": "638318_fever",
"type": "fever",
"name": "혼의 공명",
"cooldown_sec": 45,
"hits": 1,
"multipliers": [{ "stat": "atk", "base_type": "total", "ratio": 9.00 }],
"effects": [
{
"type": "shield",
"target": "ally_all",
"source": "resonance_value",
"duration": 8,
"note": "공명치 100% → 마음의 벽 변환, 생존 아군 균등 분배. 마음의 벽 보유 시 피해 감소 10%"
},
{
"type": "heal_dot",
"target": "ally_lowest_hp_4",
"stat": "hp",
"base_type": "total",
"ratio": 0.05,
"flat": 500,
"duration": 5
}
]
}
],
"passives": [
{ "id": "638318_passive1", "name": "보호 강화", "effect_type": "stat_bonus", "target_stat": "def", "value_type": "flat", "value": 165 },
{ "id": "638318_passive2", "name": "굳건한 투지", "effect_type": "stat_bonus", "target_stat": "block_rate", "value_type": "percent", "value": 0.15 },
{ "id": "638318_passive3", "name": "신체 능력 돌파", "effect_type": "stat_bonus", "target_stat": "hp", "value_type": "flat", "value": 1728 }
],
"awakenings": [
{ "step": 1, "name_cn": "鬥志激昂", "effect_type": "stat_bonus",
"target_stat": "hp", "value_type": "flat", "value": 1002, "condition": null,
"description": "체력 +1002" },
{ "step": 2, "name_cn": "潛能激發", "effect_type": "skill_level",
"target_stat": null, "value_type": null, "value": 1, "condition": "battle_and_fever",
"description": "전투 스킬 +1, 피버 스킬 +1" },
{ "step": 3, "name_cn": "啓示", "effect_type": "passive",
"target_stat": null, "value_type": "percent", "value": 0.50, "condition": "battle_start",
"description": "전투 시작 시 최대 체력의 50% 마음의 벽 획득 (10초 지속). 마음의 벽 보유 시 피해 감소 +10%" },
{ "step": 4, "name_cn": "不朽之輝", "effect_type": "stat_bonus",
"target_stat": "hp", "value_type": "flat", "value": 1441, "condition": null,
"description": "체력 +1441" },
{ "step": 5, "name_cn": "本源共鳴", "effect_type": "skill_level",
"target_stat": null, "value_type": null, "value": 1, "condition": "battle_and_fever",
"description": "전투 스킬 +1, 피버 스킬 +1" }
],
"badge_recommendation": "revival",
"leader_skill": {
"condition": "fever",
"effects": [
{ "effect_type": "stat_bonus", "target_stat": "atk_percent", "value": 0.05, "target": "ally_all" },
{ "effect_type": "stat_bonus", "target_stat": "heal_percent", "value": 0.15, "target": "ally_all" }
]
}
}

# 뱃지 정보 예시
[
{
"id": "brave",
"name_ko": "용맹",
"effects": [
{ "required_count": 2, "effect_type": "stat_bonus", "target_stat": "atk_percent", "value": 0.15, "condition": null },
{ "required_count": 3, "effect_type": "skill_dmg", "target_skill_type": "normal", "value": 0.10, "condition": null }
]
},
{
"id": "rampage",
"name_ko": "광폭",
"effects": [
{ "required_count": 2, "effect_type": "stat_bonus", "target_stat": "atk_percent", "value": 0.15, "condition": null },
{ "required_count": 3, "effect_type": "skill_dmg", "target_skill_type": "battle", "value": 0.30, "condition": null }
]
},
{
"id": "flame",
"name_ko": "불꽃",
"effects": [
{ "required_count": 2, "effect_type": "stat_bonus", "target_stat": "atk_percent", "value": 0.15, "condition": null },
{ "required_count": 3, "effect_type": "skill_dmg", "target_skill_type": "fever", "value": 0.30, "condition": null }
]
},
{
"id": "earth",
"name_ko": "대지",
"effects": [
{ "required_count": 2, "effect_type": "stat_bonus", "target_stat": "hp_percent", "value": 0.15, "condition": null },
{ "required_count": 3, "effect_type": "dmg_reduction", "target_stat": "hp", "value": 0.005, "per": 500, "max": 0.30, "condition": null }
]
},
{
"id": "revival",
"name_ko": "소생",
"effects": [
{ "required_count": 2, "effect_type": "stat_bonus", "target_stat": "heal_percent", "value": 0.20, "condition": null },
{ "required_count": 3, "effect_type": "heal_dot", "value": 0.07, "duration": 3, "condition": "target_hp_below_50" }
]
},
{
"id": "unbeaten",
"name_ko": "불패",
"effects": [
{ "required_count": 2, "effect_type": "stat_bonus", "target_stat": "shield_percent", "value": 0.30, "condition": null },
{ "required_count": 3, "effect_type": "dmg_bonus_stack", "value": 0.10, "per_shield": 1000, "max_stacks": 5, "condition": null }
]
},
{
"id": "guidance",
"name_ko": "인도",
"effects": [
{ "required_count": 2, "effect_type": "stat_bonus", "target_stat": "def_percent", "value": 0.10, "condition": null },
{ "required_count": 2, "effect_type": "stat_bonus", "target_stat": "hp_percent", "value": 0.10, "condition": null },
{ "required_count": 3, "effect_type": "team_dmg_bonus", "target_class_id": 2, "value": 0.10, "stackable": false, "condition": "battle_start" }
]
},
{
"id": "ruin",
"name_ko": "괴멸",
"effects": [
{ "required_count": 2, "effect_type": "stat_bonus", "target_stat": "hp_percent", "value": 0.10, "condition": null },
{ "required_count": 2, "effect_type": "stat_bonus", "target_stat": "atk_percent", "value": 0.05, "condition": null },
{ "required_count": 3, "effect_type": "team_dmg_bonus", "target_class_id": 3, "value": 0.10, "stackable": false, "condition": "battle_start" }
]
},
{
"id": "bisaek",
"name_ko": "비색",
"effects": [
{ "required_count": 2, "effect_type": "stat_bonus", "target_stat": "hp_percent", "value": 0.10, "condition": null },
{ "required_count": 2, "effect_type": "stat_bonus", "target_stat": "atk_spd_percent", "value": 0.05, "condition": null },
{ "required_count": 3, "effect_type": "team_dmg_bonus", "target_class_id": 4, "value": 0.10, "stackable": false, "condition": "battle_start" }
]
},
{
"id": "origin",
"name_ko": "근원",
"effects": [
{ "required_count": 2, "effect_type": "stat_bonus", "target_stat": "dot_percent", "value": 0.50, "condition": null },
{ "required_count": 3, "effect_type": "dmg_bonus", "value": 0.30, "condition": "fever_and_target_has_dot" }
]
}
]

# 직업 정보 예시
[
{
"id": 1,
"name_ko": "수호",
"name_cn": "坚甲"
},
{
"id": 2,
"name_ko": "돌격",
"name_cn": "异刃"
},
{
"id": 3,
"name_ko": "언령",
"name_cn": "言灵"
},
{
"id": 4,
"name_ko": "사수",
"name_cn": "猎影"
}
]

# 속성 상성
[
{
"attacker_element_id": 2,
"defender_element_id": 3,
"multiplier": 1.3
},
{
"attacker_element_id": 3,
"defender_element_id": 1,
"multiplier": 1.3
},
{
"attacker_element_id": 1,
"defender_element_id": 2,
"multiplier": 1.3
},
{
"attacker_element_id": 4,
"defender_element_id": 5,
"multiplier": 1.3
},
{
"attacker_element_id": 5,
"defender_element_id": 4,
"multiplier": 1.3
},
{
"attacker_element_id": 2,
"defender_element_id": 1,
"multiplier": 0.7
},
{
"attacker_element_id": 3,
"defender_element_id": 2,
"multiplier": 0.7
},
{
"attacker_element_id": 1,
"defender_element_id": 3,
"multiplier": 0.7
}
]

# 속성
[
{
"id": 1,
"name_ko": "수",
"color": "#4A90D9"
},
{
"id": 2,
"name_ko": "화",
"color": "#E85D30"
},
{
"id": 3,
"name_ko": "목",
"color": "#5BAD6F"
},
{
"id": 4,
"name_ko": "암",
"color": "#7B5EA7"
},
{
"id": 5,
"name_ko": "광",
"color": "#E8C84A"
}
]

# 피버
{
"atk_spd_bonus": 0.2,
"duration": 15,
"description": "피버 진입 시 전체 공속 20% 증가, 캐릭터별 피버스킬 발동"
}

# 역할
[
{
"id": 1,
"name_ko": "딜러"
},
{
"id": 2,
"name_ko": "탱커"
},
{
"id": 3,
"name_ko": "힐러"
},

"id": 4,
"name_ko": "서포터"
}
]

# 티어 투표 시스템
Firestore 구조
tier_votes/{자동ID}
├── character_id: 638318
├── content_type: "PVP"        // PVP or PVE
├── tier: "S"                   // S/1/2/3/4/5
├── user_fingerprint: string    // 중복 방지용 (앱 설치 시 UUID 생성)
└── created_at: timestamp

tier_summary/{character_id}_{content_type}
├── character_id: 638318
├── content_type: "PVP"
├── tier_S: 10
├── tier_1: 5
├── tier_2: 3
├── tier_3: 1
├── tier_4: 0
├── tier_5: 0
├── total_votes: 19
└── top_tier: "S"              // 최다 득표 티어 (캐릭터 아이콘에 표시)
# 티어 종류
S / 1 / 2 / 3 / 4 / 5
user_fingerprint
앱 최초 실행 시 UUID 생성 후 SharedPreferences에 저장.
로그인 없이 중복 투표 방지용:
kotlinval fingerprint = prefs.getString("user_id", null)
?: UUID.randomUUID().toString().also { prefs.edit().putString("user_id", it).apply() }
투표 저장
kotlinfirestore.collection("tier_votes").add(
mapOf(
"character_id" to characterId,
"content_type" to contentType,  // "PVP" or "PVE"
"tier" to tier,
"user_fingerprint" to fingerprint,
"created_at" to FieldValue.serverTimestamp()
)
)
티어 요약 조회 (캐릭터 목록/상세 화면)
kotlinfirestore.collection("tier_summary")
.document("${characterId}_PVP")
.get()
.addOnSuccessListener { doc ->
val topTier = doc.getString("top_tier")  // 아이콘 뱃지에 표시
val totalVotes = doc.getLong("total_votes")
}
tier_summary 갱신 방식
투표 저장 후 Cloud Function 또는 앱에서 직접 트랜잭션으로 갱신:
kotlinfirestore.runTransaction { transaction ->
val summaryRef = firestore.collection("tier_summary")
.document("${characterId}_${contentType}")
val snapshot = transaction.get(summaryRef)
val newCount = (snapshot.getLong("tier_$tier") ?: 0) + 1
transaction.update(summaryRef, "tier_$tier", newCount)
transaction.update(summaryRef, "total_votes", FieldValue.increment(1))
}