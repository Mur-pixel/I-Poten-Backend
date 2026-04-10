# I-Poten Mobile API Reference

> Base URL: `https://[서버주소]`
> 인증: 모든 인증 필요 요청은 `Cookie: userToken={token}` 헤더 포함
> 모바일 소셜 로그인은 `Authorization: Bearer {accessToken}` 헤더 사용

---

## ⚠️ 모바일 업데이트 필요 사항

> 백엔드 API 키는 변경 없음. **UI 표시 레이블과 correction 구조**가 변경됨.

### 1. hexagonScore 표시 레이블 변경

백엔드 JSON 키는 동일하나, 화면에 표시되는 한글 레이블이 변경되었습니다.
`lib/features/interview/constants/hexagon_score_type.dart` 수정 필요.

| 백엔드 JSON 키 | Dart 필드 | 기존 레이블 | **변경 레이블** |
|--------------|-----------|-----------|--------------|
| `communication` | `communication` | 의사소통 | **표현력** |
| `productivity` | `productivity` | 생산성 | **실행력** |
| `documentation_skills` | `documentationSkills` | 문서작성 | **논리구조** |
| `flexibility` | `flexibility` | 유연성 | **적응력** |
| `problem_solving` | `problemSolving` | 문제해결 | 문제해결 *(동일)* |
| `technical_skills` | `technicalSkills` | 기술능력 | **기술역량** |

**수정 대상 파일**: `lib/features/interview/constants/hexagon_score_type.dart`

```dart
// 변경 전
enum HexagonScoreType {
  communication('의사소통'),
  productivity('생산성'),
  documentationSkills('문서작성'),
  flexibility('유연성'),
  problemSolving('문제해결'),
  technicalSkills('기술능력');
  ...
}

// 변경 후
enum HexagonScoreType {
  communication('표현력'),
  productivity('실행력'),
  documentationSkills('논리구조'),
  flexibility('적응력'),
  problemSolving('문제해결'),
  technicalSkills('기술역량');
  ...
}
```

> `HexagonScore.fromJson` 매핑 로직(`technical_skills` → `technicalSkills` 등)은 **변경 없음**.

---

### 2. correction 필드 구조 변경

`InterviewQa.correction` 필드의 섹션 형식이 변경되었습니다.

| | 기존 형식 | **새 형식** |
|-|---------|----------|
| 섹션 1 | `[보강 추천]` | `[이 질문의 핵심]` |
| 섹션 2 | `[표현 첨삭]` | `[잘한 점]` |
| 섹션 3 | *(없음)* | `[이렇게 보완해보세요]` |
| 섹션 4 | *(없음)* | `[표현 개선]` |

**파싱 방법**: `[섹션명]` 이후 다음 `[` 전까지가 해당 섹션 내용

```dart
// correction 파싱 예시 (Dart)
Map<String, String> parseCorrection(String? text) {
  if (text == null || text.isEmpty) return {};
  final result = <String, String>{};
  final regex = RegExp(r'\[([^\]]+)\]\s*\n?([\s\S]*?)(?=\n?\[|$)');
  for (final match in regex.allMatches(text)) {
    final title = match.group(1)?.trim() ?? '';
    final content = match.group(2)?.trim() ?? '';
    if (title.isNotEmpty && content.isNotEmpty) {
      result[title] = content;
    }
  }
  return result;
}

// 사용 예
final parsed = parseCorrection(qa.correction);
final core    = parsed['이 질문의 핵심'];   // 면접관 의도
final good    = parsed['잘한 점'];
final improve = parsed['이렇게 보완해보세요']; // • 로 구분된 불릿
final expr    = parsed['표현 개선'];          // ❌원문 → ⭕개선문, 없으면 "개선 불필요"

// 구버전 하위 호환
final legacyBogang = parsed['보강 추천'];
final legacyExpr   = parsed['표현 첨삭'];
```

> **`[표현 개선]` 불릿이 없고 "개선 불필요" 텍스트인 경우 UI에서 섹션 숨김 처리 권장**

---

### 3. overallComment 파싱

`overallComment` 필드는 `**섹션명**:` 형식으로 구성됩니다.

```
**전반적인 인상**: (2문장)
**강점**: (2~3문장)
**개선점**: (2~3문장)
**최종 평가**: (1문장)
```

```dart
// overallComment 파싱 예시 (Dart)
List<Map<String, String>> parseOverallComment(String? text) {
  if (text == null || text.isEmpty) return [];
  final sections = <Map<String, String>>[];
  final lines = text.split('\n').where((l) => l.trim().isNotEmpty).toList();
  Map<String, String>? current;
  for (final line in lines) {
    final match = RegExp(r'\*\*(.+?)\*\*').firstMatch(line);
    if (match != null) {
      if (current != null) sections.add(current);
      final title = match.group(1)!.replaceAll(RegExp(r':$'), '').trim();
      final content = line.replaceAll(RegExp(r'\*\*.+?\*\*:?\s*'), '').trim();
      current = {'title': title, 'content': content};
    } else if (current != null) {
      current['content'] = '${current['content']}\n${line.trim()}';
    }
  }
  if (current != null) sections.add(current);
  return sections;
}
```

---

## 목차
1. [인증 (Authentication)](#인증)
2. [계정 프로필 (Account Profile)](#계정-프로필)
3. [AI 면접 (Interview)](#ai-면접)
4. [퀴즈 (Quiz)](#퀴즈)
5. [단어장 (Wordbook)](#단어장)
6. [용어 검색 (Terms)](#용어-검색)
7. [기타](#기타)

---

## 인증

### 소셜 로그인 (모바일 전용)

#### Google 로그인
```
GET /authentication/google/login/mobile
Authorization: Bearer {googleAccessToken}
```
**응답**
```json
{
  "userToken": "string",
  "nickname": "string",
  "isNewUser": true
}
```

#### Kakao 로그인
```
GET /kakao-authentication/login/mobile
Authorization: Bearer {kakaoAccessToken}
```
**응답** *(Google과 동일)*

---

### 토큰 갱신
```
POST /api/mobile/auth/refresh
```
**요청**
```json
{
  "refreshToken": "string"
}
```
**응답**
```json
{
  "newAccessToken": "string",
  "newRefreshToken": "string",
  "nickname": "string"
}
```

---

### 회원가입 후 토큰 발급
```
POST /api/mobile/auth/register
Cookie: userToken={temporaryToken}
```
**응답** *(토큰 갱신과 동일)*

---

### 로그아웃
```
POST /api/mobile/auth/logout
Cookie: userToken={token}
```
**요청** *(선택)*
```json
{
  "refreshToken": "string"
}
```
**응답**: `"success"`

---

### 토큰 유효성 검증
```
GET /api/authentication/token/verification
Cookie: userToken={token}
```
**응답**
```json
{
  "isValid": true,
  "nickname": "string"
}
```

---

### 회원 탈퇴
```
POST /api/account/withdraw
Cookie: userToken={token}
```
**응답**: `204 No Content`

---

## 계정 프로필

### 프로필 조회
```
GET /account-profile/profile
Cookie: userToken={token}
```
**응답**
```json
{
  "nickname": "string",
  "email": "string"
}
```

### 닉네임 조회
```
GET /account-profile/nickname
Cookie: userToken={token}
```

### 이메일 조회
```
GET /account-profile/email
Cookie: userToken={token}
```

### 닉네임 변경
```
PUT /account-profile/update-nickname
Cookie: userToken={token}
```
**요청**
```json
{
  "nickname": "string"
}
```

---

## AI 면접

### 면접 생성

#### 기본 면접 생성
```
POST /api/interview/create
Cookie: userToken={token}
```
**요청**
```json
{
  "interviewType": "COMPANY | TECHNICAL",
  "firstQuestion": "string"
}
```
**응답**
```json
{
  "interviewId": 1,
  "question": "첫 번째 질문 텍스트",
  "questionId": 1
}
```

#### 일반 면접 생성
```
POST /api/interview/create/normal
Cookie: userToken={token}
```
**요청**
```json
{
  "interviewType": "COMPANY | TECHNICAL"
}
```

---

### 면접 진행 (다음 질문)
```
POST /api/interview/progress
Cookie: userToken={token}
```
**요청**
```json
{
  "interviewId": 1,
  "interviewType": "COMPANY | TECHNICAL",
  "answer": "사용자 답변 텍스트"
}
```
**응답**
```json
{
  "interviewId": 1,
  "question": "다음 질문 텍스트",
  "questionId": 2,
  "isFinished": false
}
```

---

### 면접 종료
```
POST /api/interview/end
Cookie: userToken={token}
```
**요청**
```json
{
  "interviewId": 1
}
```
**응답**: `204 No Content`
> 종료 후 AI 평가가 비동기로 처리됩니다. 결과는 잠시 후 `/result/{interviewId}` 로 조회 가능합니다.

---

### 면접 결과 조회
```
GET /api/interview/result/{interviewId}
Cookie: userToken={token}
```
**응답**
```json
{
  "interviewResultList": [
    {
      "question": "질문 텍스트",
      "answer": "사용자 답변",
      "intent": "이 질문이 평가하는 역량 (예: 기술 이해도)",
      "feedback": "AI 피드백 3문장 이상",
      "correction": "[이 질문의 핵심]\n...\n[잘한 점]\n...\n[이렇게 보완해보세요]\n• ...\n[표현 개선]\n❌ 원문 → ⭕ 개선문"
    }
  ],
  "hexagonScore": {
    "communication": 7,
    "productivity": 6,
    "documentation_skills": 5,
    "flexibility": 8,
    "problem_solving": 7,
    "technical_skills": 9
  },
  "overallComment": "**전반적인 인상**: ...\n**강점**: ...\n**개선점**: ...\n**최종 평가**: ..."
}
```

> **correction 파싱 가이드**
> `correction` 필드는 `[섹션명]` 형식으로 구분됩니다.
> ```
> [이 질문의 핵심]   → 면접관 의도 (단문)
> [잘한 점]          → 강점 분석 (단문)
> [이렇게 보완해보세요] → 보완점 (불릿 리스트, • 로 구분)
> [표현 개선]        → ❌ 원문 → ⭕ 개선문 (없으면 "개선 불필요")
> ```
> 구버전 데이터는 `[보강 추천]` / `[표현 첨삭]` 형식일 수 있음 (하위 호환 필요)

> **hexagonScore 레이블 매핑**
> | 필드 | 표시 레이블 |
> |------|------------|
> | `communication` | 표현력 |
> | `technical_skills` | 기술역량 |
> | `problem_solving` | 문제해결 |
> | `productivity` | 실행력 |
> | `documentation_skills` | 논리구조 |
> | `flexibility` | 적응력 |

---

### 면접 결과 목록 조회
```
GET /api/interview/result/list
Cookie: userToken={token}
```
**응답**
```json
{
  "interviewResultList": [
    {
      "interviewId": 1,
      "interviewType": "COMPANY",
      "createdAt": "2026-03-15 14:30"
    }
  ]
}
```

---

### 내 기술 스택 조회
```
GET /api/interview/my-techstack
Cookie: userToken={token}
```

---

## 퀴즈

### 퀴즈 세트 구성

#### 퀴즈 세트 조회 (카테고리/타입/레벨 기반)
```
GET /api/me/quiz/sets/resolve?termCategoryId={id}&type={type}&level={level}&count={count}
Cookie: userToken={token}
```
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `termCategoryId` | Long | 카테고리 ID |
| `type` | String | CHOICE \| OX \| INITIALS |
| `level` | String | EASY \| MEDIUM \| HARD |
| `count` | Int | 문제 수 |

---

### 퀴즈 세션

#### 세션 시작
```
POST /api/me/quiz/sessions/start
Cookie: userToken={token}
```
**요청**
```json
{
  "source": "QUIZ_SET | WORDBOOK | CUSTOM",
  "quizSetId": 1,
  "count": 10,
  "customTitle": "나만의 퀴즈"
}
```
**응답**
```json
{
  "sessionId": "uuid-string",
  "title": "퀴즈 제목",
  "totalCount": 10
}
```

#### 세션 문제 목록 조회
```
GET /api/me/quiz/sessions/{sessionId}/items?offset=0&limit=10&includeAnswers=false
Cookie: userToken={token}
```

#### 퀴즈 제출
```
POST /api/me/quiz/sessions/{sessionId}/submit
Cookie: userToken={token}
```
**요청**
```json
{
  "answers": [
    { "questionId": 1, "answer": "string" }
  ]
}
```

#### 세션 목록 조회
```
GET /api/me/quiz/sessions?limit=20&status=COMPLETED
Cookie: userToken={token}
```

#### 세션 요약 조회
```
GET /api/me/quiz/sessions/{sessionId}
Cookie: userToken={token}
```

#### 세션 복습
```
GET /api/me/quiz/sessions/{sessionId}/review
Cookie: userToken={token}
```

#### 오답만 재시도
```
POST /api/me/quiz/sessions/{sessionId}/retry-wrong
Cookie: userToken={token}
```

#### 세션 삭제
```
DELETE /api/me/quiz/sessions/{sessionId}
Cookie: userToken={token}
```

#### 세션 제목 변경
```
PATCH /api/me/quiz/sessions/{sessionId}/title
Cookie: userToken={token}
```
**요청**
```json
{ "title": "새 제목" }
```

---

### 데일리 퀴즈

#### 데일리 퀴즈 시작
```
POST /api/me/quiz/daily/general/start?mode={mode}
Cookie: userToken={token}
```

#### 데일리 퀴즈 문제 채점
```
POST /api/me/quiz/daily/sessions/{sessionId}/questions/{questionId}/check
Cookie: userToken={token}
```
**요청**
```json
{ "answer": "string" }
```
**응답**
```json
{
  "correct": true,
  "feedback": "해설 텍스트"
}
```

---

### 오답노트

#### 오답 목록 조회
```
GET /api/me/quiz/reviews/wrong?page=0&size=20&type=ALL&unresolvedOnly=false
Cookie: userToken={token}
```

#### 오답 해결 처리
```
PATCH /api/me/quiz/reviews/{wrongNoteId}
Cookie: userToken={token}
```
**요청**
```json
{ "resolved": true }
```

#### 오답 삭제 (단건)
```
DELETE /api/me/quiz/reviews/{wrongNoteId}
Cookie: userToken={token}
```

#### 오답 삭제 (다건)
```
DELETE /api/me/quiz/reviews/wrong
Cookie: userToken={token}
```
**요청**
```json
{ "reviewIds": [1, 2, 3] }
```

---

### 퀴즈 통계

#### 트렌드 데이터
```
GET /api/me/quiz/metrics?metric={metric}&span=30d
Cookie: userToken={token}
```

#### 타임라인
```
GET /api/me/quiz/timeline?type=ALL&page=0&size=20
Cookie: userToken={token}
```

---

## 단어장

### 폴더 관리

#### 폴더 목록 조회
```
GET /api/me/folders
Cookie: userToken={token}
```
**응답**
```json
[
  { "id": 1, "wordbookName": "폴더명", "sortOrder": 0 }
]
```

#### 폴더 생성
```
POST /api/me/folders
Cookie: userToken={token}
```
**요청**
```json
{ "wordbookName": "새 폴더" }
```

#### 폴더 이름 변경
```
PATCH /api/me/folders/{wordbookId}
Cookie: userToken={token}
```
**요청**
```json
{ "newName": "새 이름" }
```

#### 폴더 삭제
```
DELETE /api/me/folders/{wordbookId}?mode=purge
Cookie: userToken={token}
```
| 파라미터 | 값 | 설명 |
|---------|---|------|
| `mode` | `purge` | 폴더와 단어 모두 삭제 |
| `mode` | `move` | 다른 폴더로 이동 후 삭제 |
| `targetWordbookId` | Long | `move` 시 이동 대상 폴더 |

#### 폴더 순서 변경
```
PATCH /api/me/folders:reorder
Cookie: userToken={token}
```

---

### 단어 관리

#### 폴더 내 단어 목록
```
GET /api/me/folders/{folderId}/terms?page=0&size=20&sort=name
Cookie: userToken={token}
```

#### 단어 추가 (단건)
```
POST /api/me/folders/{folderId}/terms
Cookie: userToken={token}
```
**요청**
```json
{ "termId": 123 }
```

#### 단어 추가 (다건)
```
POST /api/me/folders/{folderId}/terms:bulk
Cookie: userToken={token}
```
**요청**
```json
{ "termIds": [1, 2, 3] }
```

#### 단어 다른 폴더로 이동
```
PATCH /api/me/folders/{sourceWordbookId}/terms:move
Cookie: userToken={token}
```
**요청**
```json
{
  "targetWordbookId": 2,
  "termIds": [1, 2, 3]
}
```

#### 단어 암기 상태 업데이트
```
PATCH /api/me/terms/{termId}/memorization
Cookie: userToken={token}
```
**요청**
```json
{ "status": "MEMORIZED | UNMEMORIZED | VAGUE" }
```

---

## 용어 검색

### 용어 검색
```
GET /api/terms/search?q={검색어}&page=0&size=20
Cookie: userToken={token}  (선택)
```
**응답**
```json
{
  "terms": [
    {
      "termId": 1,
      "title": "용어명",
      "description": "설명",
      "tags": ["tag1", "tag2"],
      "categoryId": 5
    }
  ],
  "totalCount": 100
}
```

### 카테고리 조회
```
GET /api/categories?depth={depth}&parentId={parentId}
```

### 트렌딩 용어
```
GET /api/terms/trending
```

### 태그로 검색
```
GET /api/terms/search/by-tag?tag={tag}&page=0&size=20
```

---

## 기타

### FCM 토큰 등록
```
POST /api/fcm/token
Cookie: userToken={token}
```
**요청**
```json
{ "token": "fcm-device-token" }
```
**응답**: `204 No Content`

### TTS 변환
```
POST /google_tts
```
**요청**
```json
{ "text": "변환할 텍스트" }
```

### 크레딧 잔액 조회
```
GET /credit/account
Cookie: userToken={token}
```

### 면접 리뷰 작성
```
POST /api/review/interview
Cookie: userToken={token}
```

---

## 에러 응답 형식

| 상태코드 | 의미 |
|---------|------|
| `200` | 성공 |
| `201` | 생성 성공 |
| `204` | 성공 (응답 없음) |
| `401` | 인증 실패 (토큰 만료 또는 없음) |
| `403` | 권한 없음 |
| `404` | 리소스 없음 |
| `500` | 서버 오류 |

> 401 응답 시 `/api/mobile/auth/refresh` 로 토큰 갱신 후 재요청

---

## 면접 결과 hexagonScore 계산 예시 (Flutter)

```dart
// 총점 계산 (최대 60점 → 100점 환산)
double totalScore = (communication + productivity + documentationSkills +
                     flexibility + problemSolving + technicalSkills).toDouble();
int scorePercent = (totalScore / 60 * 100).round();

// 등급 계산
String grade;
if (scorePercent >= 90) grade = 'A';
else if (scorePercent >= 75) grade = 'B';
else if (scorePercent >= 50) grade = 'C';
else if (scorePercent >= 25) grade = 'D';
else grade = 'F';
```
