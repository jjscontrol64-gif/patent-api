# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 빌드 및 실행

```bash
# 컴파일 + 테스트 + JAR 생성
./gradlew build

# 테스트만
./gradlew test

# 단일 테스트 클래스
./gradlew test --tests "com.jjs.patentapi.PatentApiApplicationTests"

# 로컬 서버 실행 (http://localhost:8080)
./gradlew bootRun

# Docker 이미지 빌드 및 실행
docker build -t patent-api .
docker run -p 8080:10000 \
  -e SERP_API_KEY=... \
  -e DEEPL_API_KEY=... \
  patent-api
```

빌드 전 확인:
```bash
./gradlew clean build
```

## 환경 변수

API 키는 `.env` 파일(spring-dotenv로 자동 로드) 또는 OS 환경 변수로 주입한다.

| 변수 | 용도 | 기본값 |
|------|------|--------|
| `SERP_API_KEY` | SerpAPI (Google Patents) | 없음 (mock fallback) |
| `DEEPL_API_KEY` | DeepL 번역 | 없음 (mock fallback) |
| `PORT` | 서버 포트 | `8080` (Docker: `10000`) |

키가 없으면 두 Client 모두 mock 응답을 반환한다. 로컬 개발은 키 없이도 동작한다.

## 아키텍처

```
Controller → Service → Client (외부 API)
```

- **Controller**: 요청 파싱, 검증 위임, 응답 반환만. 비즈니스 로직 없음.
- **Service**: 현재는 Client 호출을 위임하는 얇은 레이어. 추후 조합 로직 추가 지점.
- **Client**: 외부 API 통신, JSON 매핑, mock fallback 전담.
- **DTO**: 불변 모델. `@Data`(request) 또는 `@Builder`(response).
- **Exception**: `PatentNotFoundException`, `ExternalApiException` → `GlobalExceptionHandler` → `ApiErrorResponse`.

의존성 주입은 Lombok `@RequiredArgsConstructor` 생성자 주입을 사용한다.

## 엔드포인트

| 메서드 | 경로 | 설명 |
|--------|------|------|
| `GET` | `/` | 서비스 상태 및 엔드포인트 목록 |
| `GET` | `/actuator/health` | 헬스 체크 |
| `GET` | `/api/patents/search` | 특허 검색 (`query`, `from`, `to`, `page`, `size`) |
| `GET` | `/api/patents/{id}` | 특허 상세 조회 |
| `POST` | `/api/translations` | 텍스트 번역 |
| `GET` | `/swagger-ui/index.html` | Swagger UI (springdoc-openapi) |

## 주요 동작 규칙

- `SerpApiClient`는 JP(일본) 공개번호만 필터링한다. 검색 결과에서 비JP 결과는 제외됨.
- `SerpApiClient.normalizePatentId()`: `patent/` 또는 `scholar/` 접두사 없으면 `patent/`를 자동 추가.
- `DeepLClient.normalizeLanguageCode()`: `JP→JA`, `KR→KO` 변환 (DeepL 언어코드 규격 맞춤).
- `SerpApiClient.serpApiPageSize()`: SerpAPI 실제 요청 size는 10~100 범위로 클램프.
- Jackson 3.x 사용(`tools.jackson.databind.JsonNode`) — Spring Boot 4.x 기본값.

## 테스트

테스트 파일은 `src/test/java/com/jjs/patentapi/` 아래, 클래스명 `*Tests` 접미사.

Controller, Validation, ExceptionHandler, Service 로직 변경 시 해당 동작 검증 테스트를 추가한다.

## 커밋 규칙

명령형, 짧고 명확하게. 예:
- `fix patent search JP filter`
- `add translation language normalization`
