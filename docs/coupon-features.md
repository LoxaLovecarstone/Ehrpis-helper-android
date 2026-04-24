# 쿠폰 화면 기능 로드맵

## 구현 예정

### NEW 배지
- 최근 24시간 이내 추가된 쿠폰 카드에 "NEW" 배지 표시
- `createdDate` 필드가 이미 Firestore에 있으므로 프론트 단독 구현 가능
- 기준: `LocalDate.now(seoulZone) - createdDate <= 1일`

---

### 전체 리세마라 해제
- 사용완료 체크된 코드를 한 번에 전부 해제하는 버튼
- 리세마라 시작 시 매번 하나씩 해제하는 불편함 해소
- Room DB `used_coupon_codes` 테이블 전체 delete
- 위치: 사용완료 섹션 상단 또는 앱바 메뉴

---

### 오늘 마감 알림 (로컬 푸시)
- FCM은 새 쿠폰 등록 알림, 이건 만료 임박 알림 (별개)
- 당일 만료되는 쿠폰이 있을 때 오전 중 로컬 알림 발송
- WorkManager로 매일 오전 9시 체크 → Room/Firestore 조회 → 조건 충족 시 알림
- 사용자가 이미 사용 완료한 쿠폰은 알림 제외

---

### 오팔/그림자 필터
리세마라 유저는 재화 지급 쿠폰만 관심 있음.

**게임 재화 종류**
- 오팔(Opal): 유료 프리미엄 재화
- 그림자: 뽑기에 사용하는 재화
- 그 외: 아이템, 경험치 등 리세마라와 무관한 보상

**Firestore 스키마 변경 (백엔드 협의 필요)**
```
coupons/{id}
└── reward_types: array<string>  // ["opal"], ["shadow"], ["opal", "shadow"], ["item"] 등
```

**프론트 변경**
- `Coupon` 도메인 모델에 `rewardTypes: List<String>` 추가
- 필터칩에 "오팔/그림자" 옵션 추가 (기존 4개 → 5개)
- `CouponFilter.REROLL` = `rewardTypes`에 "opal" 또는 "shadow" 포함된 쿠폰만 표시

---

## 의도적으로 미구현

### 원문 링크 열기
- Firestore `link` 필드(네이버 게임 라운지 URL)는 데이터로는 존재하나 UI에 노출하지 않음
- 쿠폰 코드 복사/사용 여부 체크가 주 목적인 앱이므로 불필요하다고 판단
