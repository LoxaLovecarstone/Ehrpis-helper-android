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
