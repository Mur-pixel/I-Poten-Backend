# I-Poten → I-Ptn 리네이밍 API 변경 사항

## 변경된 API 엔드포인트

| 변경 전 | 변경 후 | 비고 |
|---------|---------|------|
| `POST /api/review/ipoten` | `POST /api/review/iptn` | 서비스 리뷰 작성 |
| `GET /api/review/ipoten` | `GET /api/review/iptn` | 서비스 리뷰 조회 |

> ⚠️ 위 API를 사용하는 모바일 앱(Flutter)에서도 해당 엔드포인트를 업데이트해야 합니다.

## 변경되지 않은 API (앱 영향 없음)

아래 API들은 경로에 "poten"이 포함되어 있지 않아 변경 없이 그대로 사용 가능합니다:

- `/api/terms/*` - 용어 관련
- `/api/me/folders/*` - 단어장 관련
- `/api/interview/*` - 면접 관련
- `/api/quiz/*` - 퀴즈 관련
- `/api/admin/dashboard/*` - 관리자 대시보드
- `/api/account/*` - 계정 관련
- `/api/interests/*` - 관심사 관련
- `/api/ebooks/*` - 전자책 관련
- `/credit/*` - 크레딧 관련
- 모든 OAuth 인증 콜백 URL

## 코드 변경 사항

### Spring Backend (com.cygnus.ipoten → com.cygnus.iptn)

- **Java 패키지**: `com.cygnus.ipoten` → `com.cygnus.iptn` (전체)
- **메인 클래스**: `IPotenApplication` → `IPtnApplication`
- **엔티티**: `IpotenReview` → `IptnReview`
  - DB 테이블명은 `@Table(name = "ipoten_review")`로 기존 유지
- **패키지 디렉토리**: `ipoten_review/` → `iptn_review/`
- **Docker 네트워크**: `ipoten-net` → `iptn-net`
- **application.yml**: `name: ipoten` → `name: iptn`

### Frontend

- **디렉토리**: `poten-word-app/` → `ptn-word-app/`
- **모듈 페더레이션**: `potenWordApp` → `ptnWordApp`
- **컴포넌트명**: `PotenWord*` → `PtnWord*`, `PotenNote*` → `PtnNote*`, `PotenQuiz*` → `PtnQuiz*`, `PotenDialog` → `PtnDialog`
- **CSS 변수**: `--poten-*` → `--ptn-*`
- **localStorage 키**: `poten:*` → `ptn:*`, `ipoten:*` → `iptn:*`
- **환경변수**: `REACT_POTEN_WORD_APP` → `REACT_PTN_WORD_APP`
- **표시 텍스트**: `I-Poten` → `I-Ptn`, `I-POTEN` → `I-PTN`
- **Docker 네트워크**: `ipoten-net` → `iptn-net`

### FastAPI

- **Docker 네트워크**: `ipoten-net` → `iptn-net`
- **PROJECT_NAME**: `I-Poten-Backend-Fastapi` → `I-Ptn-Backend-Fastapi`

## 인프라 변경 필요 (수동)

아래 항목들은 코드에서 변경하면 서비스가 중단되므로, 인프라 리소스를 먼저 변경한 후 코드를 업데이트해야 합니다:

| 항목 | 현재 값 | 비고 |
|------|---------|------|
| 도메인 | `i-poten.com` | 새 도메인 등록 필요 |
| CDN | `cdn.i-poten.com` | CloudFront + Route53 변경 |
| S3 버킷 | `i-poten` | 새 버킷 생성 또는 유지 |
| DB명 | `poten_db` | MySQL 데이터베이스 이름 |
| DB 사용자 | `potenAD` | MySQL 사용자명 |
| Firebase 프로젝트 | `i-poten-1fdd0` | Firebase 프로젝트 ID (변경 불가) |
| SES 발신 이메일 | `noreply@i-poten.com` | AWS SES 인증 |
| GHCR 이미지 | `ghcr.io/i-cygnus/i-poten-frontend` | GitHub Packages |
| EC2 디렉토리 | `/home/ec2-user/i-poten/` | 서버 디렉토리 구조 |
| GitHub Runner 라벨 | `i-poten-front`, `i-poten-backend` | Self-hosted runner 설정 |
| Apple 클라이언트 ID | `com.cygnus.ipotenapp.signin` | Apple Developer 설정 |
| Android 패키지 | `com.cygnus.i_poten_app` | Play Store 패키지명 |
| GitHub 시크릿 | `POTEN_WORD_ENV` | GitHub Secrets 이름 |

## 앱(Flutter) 업데이트 필요 항목

```
# 변경 필요한 API 호출
POST /api/review/ipoten → POST /api/review/iptn
GET /api/review/ipoten → GET /api/review/iptn
```
