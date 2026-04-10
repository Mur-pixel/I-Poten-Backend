# AI 면접 API — 신버전 요청/응답 명세

> **Base URL**: `https://[서버주소]`
> **인증**: `Cookie: userToken={token}` (모든 요청에 포함)
> **Content-Type**: `application/json`

---

## 면접 플로우

```
1. 면접 생성 (POST /create 또는 /create/normal)
        ↓  interviewId, interviewQAId, 첫 질문 받음
2. 면접 진행 (POST /progress) × N회
        ↓  다음 질문 + 새 interviewQAId 받음
3. 면접 종료 (POST /end)
        ↓  AI 평가 비동기 처리 시작
4. 결과 조회 (GET /result/{interviewId})
```

---

## 1. 면접 생성 — 커스텀 (회사 지정)

```
POST /api/interview/create
Cookie: userToken=abc123
Content-Type: application/json
```

### Request Body

```json
{
  "interviewType": "COMPANY",
  "company": "당근마켓",
  "major": "전공자",
  "career": "3년 이하",
  "projectExp": true,
  "job": "Backend",
  "techStacks": [
    { "name": "Java" },
    { "name": "Spring Boot" },
    { "name": "MySQL" }
  ],
  "interviewAccountProjectRequests": [
    {
      "projectName": "잡스틱",
      "projectDescription": "AI 기반 취업 플랫폼 개발, Spring Boot + React 사용"
    }
  ],
  "firstQuestion": "간단한 자기소개 부탁드립니다.",
  "firstAnswer": "안녕하세요, 백엔드 개발자 홍길동입니다. Spring과 Node.js를 주로 사용하며..."
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `interviewType` | String | ✅ | `COMPANY` \| `TECHNICAL` \| `NORMAL` |
| `company` | String | - | 지원 회사명 |
| `major` | String | - | `전공자` \| `비전공자` |
| `career` | String | - | `신입` \| `1년 이하` \| `3년 이하` \| `5년 이상` |
| `projectExp` | Boolean | - | 프로젝트 경험 여부 |
| `job` | String | - | `Backend` \| `Frontend` \| `Fullstack` \| `DevOps` 등 |
| `techStacks` | Array | - | `[{ "name": "Java" }]` 형태 |
| `interviewAccountProjectRequests` | Array | - | 프로젝트 목록 |
| `firstQuestion` | String | - | 첫 번째 질문 텍스트 |
| `firstAnswer` | String | - | 첫 번째 답변 텍스트 |

### Response `200 OK`

```json
{
  "interviewId": 42,
  "interviewQAId": 101,
  "interviewQuestion": "두 번째 질문입니다. Node.js와 Spring의 차이점을 설명해주세요.",
  "interviewQuestionText": "두 번째 질문입니다. Node.js와 Spring의 차이점을 설명해주세요."
}
```

| 필드 | 설명 |
|------|------|
| `interviewId` | 면접 세션 ID (이후 요청에 계속 사용) |
| `interviewQAId` | 현재 Q&A 아이템 ID (progress 요청 시 필요) |
| `interviewQuestion` | 다음 질문 텍스트 |

---

## 2. 면접 생성 — 일반 (회사 미지정)

```
POST /api/interview/create/normal
Cookie: userToken=abc123
Content-Type: application/json
```

### Request Body

```json
{
  "interviewType": "NORMAL",
  "candidateStatus": "STUDENT",
  "self_concern": "저는 자료구조와 알고리즘 부분이 약한 것 같습니다."
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `interviewType` | String | `NORMAL` \| `TECHNICAL` |
| `candidateStatus` | String | `STUDENT` \| `NEWCOMER` \| `EXPERIENCED` |
| `self_concern` | String | 자신이 부족하다고 느끼는 부분 (선택) |

### Response `200 OK`

```json
{
  "interviewId": 43,
  "interviewQAId": 110,
  "interviewQuestion": "간단한 자기소개를 부탁드립니다.",
  "interviewQuestionText": "간단한 자기소개를 부탁드립니다."
}
```

---

## 3. 면접 진행 (다음 질문 요청)

```
POST /api/interview/progress
Cookie: userToken=abc123
Content-Type: application/json
```

### Request Body

```json
{
  "interviewId": 42,
  "interviewQAId": 101,
  "interviewSequence": 2,
  "interviewType": "COMPANY",
  "answer": "Node.js는 비동기 이벤트 루프 기반이고, Spring은 동기 멀티스레드 방식입니다. 저는 트래픽이 높은 API 서버에는 Node.js를, 복잡한 비즈니스 로직에는 Spring을 선호합니다."
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `interviewId` | Long | ✅ | 면접 생성 시 받은 ID |
| `interviewQAId` | Long | ✅ | 직전 응답에서 받은 Q&A ID |
| `interviewSequence` | Int | ✅ | 현재 질문 순서 (2, 3, 4 ...) |
| `interviewType` | String | ✅ | 생성 시와 동일한 타입 |
| `answer` | String | ✅ | 현재 질문에 대한 사용자 답변 |

### Response `200 OK`

```json
{
  "interviewId": 42,
  "interviewQAId": 102,
  "interviewQuestion": "세 번째 질문입니다. 본인이 경험한 가장 어려웠던 기술적 문제와 해결 방법을 말씀해 주세요.",
  "interviewQuestionText": "세 번째 질문입니다. 본인이 경험한 가장 어려웠던 기술적 문제와 해결 방법을 말씀해 주세요."
}
```

> **마지막 질문(6번째) 이후** 동일 엔드포인트 호출 시 `interviewQuestion`이 빈 문자열이거나 null → 면접 종료 신호

---

## 4. 면접 종료

```
POST /api/interview/end
Cookie: userToken=abc123
Content-Type: application/json
```

### Request Body

```json
{
  "interviewId": 42,
  "interviewQAId": 106,
  "answer": "마지막 답변 텍스트입니다.",
  "sender": "user@example.com"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `interviewId` | Long | ✅ | 면접 ID |
| `interviewQAId` | Long | ✅ | 마지막 Q&A ID |
| `answer` | String | ✅ | 마지막 질문 답변 |
| `sender` | String | - | 결과 알림 이메일 주소 |

### Response `200 OK`

```
(본문 없음)
```

> 종료 후 FastAPI가 비동기로 AI 평가를 수행합니다.
> 평가 완료까지 **30초~2분** 소요될 수 있으며, 완료 시 FCM 푸시 알림이 발송됩니다.

---

## 5. 면접 결과 조회

```
GET /api/interview/result/{interviewId}
Cookie: userToken=abc123
```

### Response `200 OK`

```json
{
  "interviewResultList": [
    {
      "question": "간단한 자기소개 부탁드립니다.",
      "answer": "안녕하세요, 백엔드 개발자 홍길동입니다...",
      "intent": "자기소개·성장 의지",
      "feedback": "전공과 사용 기술을 명확하게 밝힌 점은 좋았습니다. 그러나 구체적인 프로젝트 성과나 수치를 언급하지 않아 역량 파악이 어렵습니다. 팀 내 기여도와 결과를 함께 언급하면 더욱 인상적인 자기소개가 됩니다.",
      "correction": "[이 질문의 핵심]\n면접관이 지원자의 배경, 핵심 역량, 지원 동기를 한 번에 파악하려는 질문입니다.\n\n[잘한 점]\n기술 스택을 구체적으로 나열한 점이 좋았습니다.\n\n[이렇게 보완해보세요]\n• 프로젝트에서 본인이 기여한 구체적인 성과(예: 응답속도 30% 개선)를 추가하세요.\n• 지원 동기나 이 회사에 관심 갖게 된 이유를 한 문장으로 연결하면 좋습니다.\n\n[표현 개선]\n❌ 관심이 있습니다 → ⭕ 사용자 경험을 개선하는 서비스 개발에 집중하고 있습니다"
    },
    {
      "question": "팀 프로젝트에서 갈등이 생겼을 때 어떻게 해결하셨나요?",
      "answer": "팀원과 코드 스타일 차이로 갈등이 있었는데...",
      "intent": "협업·소통 능력",
      "feedback": "갈등 상황을 구체적으로 묘사한 점이 좋습니다. 해결 과정에서 본인의 역할을 더 명확히 드러내면 좋겠습니다. 결과적으로 팀에 어떤 긍정적인 변화가 있었는지 언급하세요.",
      "correction": "[이 질문의 핵심]\n갈등 상황에서의 소통 방식과 문제 해결 능력을 평가하는 질문입니다.\n\n[잘한 점]\n갈등 원인과 상황을 명확하게 설명했습니다.\n\n[이렇게 보완해보세요]\n• 갈등 해결 후 팀에 생긴 긍정적 변화를 수치나 결과로 표현하세요.\n\n[표현 개선]\n개선 불필요"
    }
  ],
  "hexagonScore": {
    "communication": 8,
    "productivity": 6,
    "documentation_skills": 7,
    "flexibility": 5,
    "problem_solving": 7,
    "technical_skills": 9
  },
  "overallComment": "**전반적인 인상**: 기술적인 배경과 경험을 명확하게 전달하였으며, 답변 구조가 논리적입니다. 다만 일부 답변에서 구체적인 수치나 성과가 부족하여 역량 파악에 아쉬움이 있습니다.\n**강점**: Node.js와 Spring에 대한 깊이 있는 이해를 바탕으로 기술적 질문에 자신감 있게 답변하였습니다. 특히 트레이드오프를 설명한 부분이 인상적이었습니다.\n**개선점**: 자기소개와 협업 경험 답변에 구체적인 프로젝트 성과 수치를 추가하면 전반적인 완성도가 높아집니다. STAR 기법(상황-과제-행동-결과)을 활용해보세요.\n**최종 평가**: 탄탄한 기술 기반을 가진 지원자로, 경험의 구체화 작업을 통해 경쟁력을 더 높일 수 있습니다."
}
```

### hexagonScore 필드 설명

| JSON 키 | 표시 레이블 | 범위 |
|---------|-----------|------|
| `communication` | 표현력 | 1 ~ 10 |
| `productivity` | 실행력 | 1 ~ 10 |
| `documentation_skills` | 논리구조 | 1 ~ 10 |
| `flexibility` | 적응력 | 1 ~ 10 |
| `problem_solving` | 문제해결 | 1 ~ 10 |
| `technical_skills` | 기술역량 | 1 ~ 10 |

> 총점 계산: `(6개 합산 / 60) × 100` → 백분율 점수
> 등급: 90↑ A / 75↑ B / 50↑ C / 25↑ D / 미만 F

### correction 섹션 파싱 키

| 섹션 키 | 타입 | 설명 |
|---------|------|------|
| `[이 질문의 핵심]` | 단문 | 면접관이 평가하려는 역량 |
| `[잘한 점]` | 단문 | 답변의 강점 분석 |
| `[이렇게 보완해보세요]` | 불릿(`•`) | 보완할 내용 1~2개 |
| `[표현 개선]` | `❌원문 → ⭕개선문` | 없으면 `"개선 불필요"` → UI에서 숨김 처리 |

### overallComment 섹션 구조

```
**전반적인 인상**: (2문장)
**강점**: (2~3문장)
**개선점**: (2~3문장)
**최종 평가**: (1문장)
```

### Response `401 Unauthorized`

```
(본문 없음) — userToken 만료 또는 없음
```

---

## 6. 면접 결과 목록 조회

```
GET /api/interview/result/list
Cookie: userToken=abc123
```

### Response `200 OK`

```json
{
  "interviewResultList": [
    {
      "interviewId": 42,
      "interviewType": "COMPANY",
      "isFinished": true,
      "sender": "user@example.com",
      "createdAt": "2026-03-15T14:30:00"
    },
    {
      "interviewId": 41,
      "interviewType": "NORMAL",
      "isFinished": true,
      "sender": "user@example.com",
      "createdAt": "2026-03-10T09:15:00"
    }
  ]
}
```

| 필드 | 설명 |
|------|------|
| `isFinished` | `true` = AI 평가 완료, `false` = 평가 진행 중 |
| `interviewType` | `COMPANY` \| `TECHNICAL` \| `NORMAL` |

---

## 구버전 vs 신버전 데이터 비교

| 필드 | 구버전 (기존 DB) | 신버전 (신규 면접) |
|------|---------------|----------------|
| `intent` | 긴 피드백 문장 (60자↑) | 짧은 역량 레이블 (예: `"협업·소통 능력"`) |
| `feedback` | `[보강 추천]` / `[표현 첨삭]` 마커 포함 | 3문장 피드백 텍스트 |
| `correction` | `null` | `[이 질문의 핵심]` 등 4섹션 |

**판별 로직**:
- `intent.length > 60` → 구버전 → `intent`를 피드백으로 표시
- `feedback`에 `[보강 추천]` 포함 → 구버전 → `feedback`을 코칭 섹션으로 파싱
- `correction`이 존재 → 신버전 → 4섹션 코칭 카드로 파싱
