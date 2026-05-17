# Patent API — Spring Boot 백엔드

기존 Next.js 기반 일본 특허 검색 MVP에서 API 역할을 분리한 Spring Boot 백엔드 서버입니다.

## 목적

Next.js 단일 프로젝트에서 화면과 API가 혼재하던 구조를, 프론트엔드와 백엔드로 분리했습니다.

- 검색·상세·번역 API를 Spring Boot 계층으로 이동
- Controller → Service → Client 계층 분리로 책임 명확화
- API key 미설정 로컬 환경에서는 mock fallback으로 예측 가능한 응답 제공
- 공통 예외 처리 및 응답 표준화 적용
- Swagger UI로 API 문서화

## 아키텍처

```
Next.js Frontend
 ├─ 검색 화면
 ├─ 검색 결과 화면
 └─ 상세 화면
        ↓ HTTP
Spring Boot API (본 서버)
 ├─ PatentController
 ├─ PatentService
 ├─ ExternalPatentClient
 ├─ TranslateClient
 ├─ MockFallbackService
 ├─ GlobalExceptionHandler
 └─ Logging / Validation
        ↓
External API
 ├─ SerpAPI
 └─ DeepL API
```

## 패키지 구조

```
src/main/java/com/jjs/patentapi/
 ├─ PatentApiApplication.java
 ├─ controller/
 │   ├─ PatentController.java
 │   └─ TranslationController.java
 ├─ service/
 │   ├─ PatentSearchService.java
 │   ├─ PatentDetailService.java
 │   └─ TranslationService.java
 ├─ client/
 │   ├─ SerpApiClient.java
 │   └─ DeepLClient.java
 ├─ dto/
 │   ├─ PatentSearchRequest.java
 │   ├─ PatentSearchResponse.java
 │   ├─ PatentDetailResponse.java
 │   └─ ApiErrorResponse.java
 ├─ config/
 │   └─ RestClientConfig.java
 └─ exception/
     ├─ GlobalExceptionHandler.java
     ├─ ExternalApiException.java
     └─ PatentNotFoundException.java
```

## API

### 특허 검색

```http
GET /api/patents/search?query=인공지능&from=20200101&to=20251231&page=1&size=10
```

```json
{
  "query": "인공지능",
  "page": 1,
  "size": 10,
  "totalCount": 2,
  "items": [
    {
      "id": "JP2024000001",
      "title": "AI search system",
      "applicant": "Example Corp",
      "filingDate": "2024-01-10",
      "publicationNumber": "JP2024-000001"
    }
  ]
}
```

### 특허 상세

```http
GET /api/patents/{id}
```

```json
{
  "id": "JP2024000001",
  "title": "AI search system",
  "abstractText": "본 발명은...",
  "applicant": "Example Corp",
  "filingDate": "2024-01-10",
  "publicationNumber": "JP2024-000001",
  "originalUrl": "..."
}
```

### 번역

```http
POST /api/translations
Content-Type: application/json

{
  "sourceText": "人工知能検索システム",
  "sourceLang": "JA",
  "targetLang": "KO"
}
```

```json
{
  "translatedText": "인공지능 검색 시스템"
}
```

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 4.0.6 |
| Web | Spring Web MVC |
| Validation | Spring Validation |
| HTTP Client | RestClient |
| API Docs | Springdoc OpenAPI (Swagger UI) |
| Monitoring | Spring Boot Actuator |
| Boilerplate | Lombok |
| Test | JUnit 5, Mockito |
| Build | Gradle |

## 실행 방법

### 환경변수 설정

```bash
SERP_API_KEY=your_serp_api_key
DEEPL_API_KEY=your_deepl_api_key
SERP_API_BASE_URL=https://serpapi.com
DEEPL_API_BASE_URL=https://api-free.deepl.com
```

API Key 미설정 시 mock fallback이 자동 적용됩니다.

### 로컬 실행

```bash
./gradlew bootRun
```

### 빌드

```bash
./gradlew build
java -jar build/libs/patent-api-0.0.1-SNAPSHOT.jar
```

### Docker 배포

Render 같은 Docker 기반 Web Service에서는 `PORT` 환경변수를 제공합니다. 애플리케이션은 `server.port=${PORT:8080}` 설정으로 해당 포트를 사용합니다.

로컬에서 Docker 이미지로 확인하려면 다음 명령을 사용합니다.

```bash
docker build -t patent-api .
docker run --rm -p 10000:10000 \
  -e PORT=10000 \
  -e SERP_API_KEY=your_serp_api_key \
  -e DEEPL_API_KEY=your_deepl_api_key \
  patent-api
```

Render에서는 Web Service 생성 시 `Language`를 `Docker`로 선택하고, Dockerfile Path는 저장소 루트의 `Dockerfile`을 사용합니다. Health Check Path는 `/actuator/health`를 권장합니다.

### Render 배포 권장 환경변수

작은 인스턴스에서는 JVM heap이 컨테이너 메모리를 모두 차지하지 않도록 `JAVA_TOOL_OPTIONS`를 설정합니다.

```bash
JAVA_TOOL_OPTIONS=-XX:+UseSerialGC -XX:InitialRAMPercentage=15 -XX:MaxRAMPercentage=60 -XX:MaxMetaspaceSize=160m -XX:+ExitOnOutOfMemoryError
APP_LOG_LEVEL=INFO
SERVER_TOMCAT_THREADS_MAX=50
SERVER_TOMCAT_THREADS_MIN_SPARE=5
SERVER_TOMCAT_ACCEPT_COUNT=50
```

메모리가 1GB 이상이고 동시 요청이 늘어나면 기본 G1 GC를 사용하는 아래 설정이 더 적합할 수 있습니다.

```bash
JAVA_TOOL_OPTIONS=-XX:+UseG1GC -XX:InitialRAMPercentage=20 -XX:MaxRAMPercentage=65 -XX:MaxMetaspaceSize=192m -XX:+ExitOnOutOfMemoryError
SERVER_TOMCAT_THREADS_MAX=100
```

### Swagger UI

```
http://localhost:8080/swagger-ui.html
```

## 관련 프로젝트

- **Next.js Frontend**: [patent-project-jjs](https://github.com/jjscontrol64-gif/patent-project-jjs)
