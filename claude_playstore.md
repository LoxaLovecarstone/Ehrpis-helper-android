# Play Store 출시 체크리스트

## 코드/빌드

| 항목 | 상태 | 내용 |
|------|------|------|
| 서명 키스토어 | ❌ | `release` 블록에 `signingConfig` 없음. keystore 생성 후 설정 필요 |
| minify (난독화) | ⚠️ | `isMinifyEnabled = false` — 출시 앱엔 `true` 권장 (R8) |
| 앱 패키지명 | ✅ | `com.loxa.ehrpishelper` |
| versionCode / versionName | ✅ | 1 / "1.0" |

## Play Console 제출

| 항목 | 상태 | 내용                                                                      |
|------|------|-------------------------------------------------------------------------|
| 개인정보처리방침 URL | ✅ | `https://loxalovecarstone.github.io/Ehrpis-helper-backend/privacy.html` |
| 데이터 안전 섹션 | ❌ | Play Console에서 직접 작성 (FCM 토큰 수집 여부 등 답변)                                |
| 앱 아이콘 | ✅ | `playstore_icon.png` (512×512) 생성 완료                                    |
| 스크린샷 | ❌ | 폰 기준 최소 2장 — 실기기/에뮬레이터에서 직접 촬영 필요                                       |
| 피처 그래픽 | ✅ | `playstore_feature.png` (1024×500) 생성 완료                                |
| 앱 이름 | ❌ | 예: "에르피스 도우미"                                                           |
| 짧은 설명 | ❌ | 80자 이내                                                                  |
| 전체 설명 | ❌ | 4000자 이내                                                                |
| 콘텐츠 등급 | ❌ | Play Console에서 IARC 설문 작성                                               |
| 개발자 연락처 | ✅ | loxa7284@gmail.com                                                      |

## 개인정보처리방침 내용 (작성 완료)

```
서비스 개요: 에르피스 모바일 게임의 쿠폰 정보를 알려주는 서비스
수집 정보: FCM 토큰 (알림 전송 목적, 별도 회원가입 없음)
이용 목적: 쿠폰 알림 발송 목적만 사용, 제3자 제공 없음
쿠폰 사용 기록: 기기 내부에만 저장, 서버 전송 없음
문의: loxa7284@gmail.com
```

## 진행 순서

1. ~~keystore 생성 → `app/build.gradle.kts` signingConfig 설정~~ (개발 완료 후 진행)
2. [✅] 앱 아이콘 `playstore_icon.png` (512×512) 생성
3. [✅] 피처 그래픽 `playstore_feature.png` (1024×500) 생성
4. [✅] 개인정보처리방침 HTML 작성 (`privacy_policy.html`)
5. [✅] 백엔드 레포 `docs/privacy.html` 업로드 + GitHub Pages 활성화
6. [ ] 스크린샷 최소 2장 촬영
7. [ ] Play Console 앱 등록 (이름/설명/데이터 안전/콘텐츠 등급)
8. ~~AAB 빌드~~ (개발 완료 후 진행)
9. ~~심사 제출~~ (개발 완료 후 진행)