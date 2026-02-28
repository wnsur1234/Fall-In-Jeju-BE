# 🌴 제주 여행 플래너 백엔드

제주 여행객을 대상으로 **AI 기반 여행 일정 생성**, **실시간 재난 문자 알림**,  
**이동 경로 제공**, **챗봇 상담** 기능을 제공하는 백엔드 서버입니다.

약 **3주간의 단기 집중 개발 프로젝트**로,  
기능 단위 개발 → 통합 → 배포까지의 전체 백엔드 사이클을 경험하는 것을 목표로 했습니다.

---

## 🛠 Tech Stack

### Language & Framework
- Java 21
- Spring Boot 3.3.6
- Spring MVC
- Spring Data JPA

### Authentication & Security
- AWS Cognito (Google OAuth)
- JWT 기반 인증 (Cognito Access Token 활용)

### Database
- RDBMS: JPA 기반 관계형 데이터베이스
- NoSQL: DynamoDB
    - 챗봇 대화 로그
    - 일부 비정형 데이터 저장

### Infra / External
- AWS
- Docker
- AI Agent Server (여행 플래너 / 챗봇 처리)
- 행정안전부 재난문자 Open API
- TMAP API (도보 / 자동차 / 대중교통)

---

## 🧩 Core Features

### 1. 여행 플래너 요청 수신 (FE → BE)
- 사용자의 여행 조건(기간, 인원, 테마 등)을 요청으로 수신
- 입력값 검증 및 요청 구조 표준화

### 2. 여행 플래너 요청 송신 (BE → Agent)
- 정제된 요청을 AI Agent 서버로 전달
  장-
- Agent 응답을 도메인 구조로 변환하여 FE에 반환

---

### 3. 챗봇 요청 수신 (FE → BE)
- 자유 질의 기반 여행 상담 요청 처리
- 사용자 인증 정보와 함께 요청 수신

### 4. 챗봇 요청 송신 (BE → Agent)
- 챗봇 질의를 Agent로 전달
- 응답 결과를 DynamoDB에 로그로 저장

---

### 5. 실시간 재난 문자 알림
- Server-Sent Events(SSE) 기반 실시간 알림
- 재난 문자 API 주기적 수집
- 신규 재난 발생 시 즉시 클라이언트 전송
- 연결 유지 목적의 주기적 ping 전송

---

### 6. 추천 여행지 평점 내역 제공
- 추천된 여행지에 대한 사용자 평점 데이터 관리
- 추후 개인화 추천을 고려한 구조 설계

---

### 7. 이동 경로 제공
- 단일 / 다중 여행지 이동 경로 지원
- 이동 수단:
    - 도보
    - 자동차
    - 대중교통
- TMAP API 연동
- 거리, 소요 시간 기반 경로 응답

---

### 8. 여행 플래너 저장 (마이페이지)
- 생성된 여행 플래너 저장
- 사용자별 여행 기록 조회
- 인증 사용자만 접근 가능

---

### 9. Google 로그인 및 회원가입
- AWS Cognito + Google OAuth 연동
- Cognito Access Token을 JWT로 활용
- 서버는 토큰 검증 및 사용자 식별에 집중

---

## 🔐 Authentication Flow

1. 사용자가 Google 로그인 시도
2. AWS Cognito에서 인증 처리
3. FE가 Cognito Access Token 획득
4. 이후 모든 API 요청에 JWT 포함
5. BE는 토큰 검증 후 요청 처리

---

## 🗂 Database Design Overview

### RDBMS (JPA)
- 사용자 기본 정보
- 여행 플래너
- 여행지 및 평점 데이터

### DynamoDB
- 챗봇 대화 로그
- 비정형 데이터 및 빠른 조회 목적

---

## 🌿 Git Branch Strategy

| 브랜치 | 역할 |
|------|------|
| feat/* | 기능 단위 개발 |
| dev | 기능 통합 및 테스트 |
| main | 배포 브랜치 |

- 모든 기능은 `feat` 브랜치에서 개발
- `dev` 브랜치에서 통합 검증
- 안정화 후 `main` 반영

---

## ⏱ Development Period

- 총 개발 기간: 약 3주
- 단기 일정 특성상:
    - MVP 우선 개발
    - 빠른 기능 검증 및 반복 개선
    - 핵심 기능 중심 설계

---

## 📌 Architecture Highlights

- FE ↔ BE ↔ Agent 구조 분리
- 인증 책임과 비즈니스 로직 분리
- 실시간 기능(SSE)과 일반 REST API 병행
- 관계형 DB + NoSQL 혼합 사용


---

## Architecture
<img width="1070" height="603" alt="Image" src="https://github.com/user-attachments/assets/04577bbb-68db-4066-b4cf-2be5e963a709" />
