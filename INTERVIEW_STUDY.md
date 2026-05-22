# Java/Spring 면접 학습자료
> patent-api 프로젝트 기반 실전 사례 정리

---

## 목차

1. [Spring Boot 기초](#1-spring-boot-기초)
2. [레이어드 아키텍처](#2-레이어드-아키텍처)
3. [IoC / DI (의존성 주입)](#3-ioc--di)
4. [REST API 설계](#4-rest-api-설계)
5. [Bean Validation](#5-bean-validation)
6. [예외 처리 전략](#6-예외-처리-전략)
7. [RestClient - 외부 API 호출](#7-restclient---외부-api-호출)
8. [Lombok](#8-lombok)
9. [설정 관리 (@Configuration / @Value)](#9-설정-관리)
10. [면접 예상 Q&A](#10-면접-예상-qa)

---

## 1. Spring Boot 기초

### @SpringBootApplication

```java
// PatentApiApplication.java
@SpringBootApplication
public class PatentApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(PatentApiApplication.class, args);
    }
}
```

**구성 요소 (3개 합성 어노테이션):**

| 어노테이션 | 역할 |
|---|---|
| `@SpringBootConfiguration` | `@Configuration` 의 특수형. Bean 정의 클래스임을 표시 |
| `@EnableAutoConfiguration` | classpath의 의존성을 보고 자동으로 Bean 등록 |
| `@ComponentScan` | 현재 패키지 하위를 스캔해서 `@Component`, `@Service` 등 자동 등록 |

**이 프로젝트에서 Auto-configuration 결과:**
- `spring-boot-starter-webmvc` → `DispatcherServlet`, `Jackson ObjectMapper` 자동 구성
- `spring-boot-starter-validation` → `MethodValidationPostProcessor` 자동 구성
- `spring-boot-starter-actuator` → `/actuator/health` 엔드포인트 자동 노출

### dotenv 환경 변수 로딩

```java
// PatentApiApplication.java
private static void loadLocalDotenv() {
    Dotenv dotenv = Dotenv.configure()
            .ignoreIfMissing()  // .env 파일 없어도 예외 없음
            .load();

    dotenv.entries().forEach(entry -> {
        String key = entry.getKey();
        // 이미 시스템 환경변수나 JVM 프로퍼티가 있으면 덮어쓰지 않음
        if (System.getenv(key) == null && System.getProperty(key) == null) {
            System.setProperty(key, entry.getValue());
        }
    });
}
```

**핵심 포인트:** Docker/CI 환경에서는 환경변수가 이미 주입되므로 `.env` 파일이 없어도 동작. 로컬 개발 편의를 위한 fallback.

---

## 2. 레이어드 아키텍처

### 이 프로젝트의 레이어 구조

```
[Controller] → [Service] → [Client]
                              ↓
                        외부 API (SerpAPI, DeepL)
```

> **주목:** DB 레이어 없음. Repository 대신 외부 API를 Client 레이어로 추상화.

### 각 레이어의 책임

| 레이어 | 클래스 | 책임 |
|---|---|---|
| Controller | `PatentController`, `TranslationController` | HTTP 요청/응답 매핑, 유효성 검사 트리거 |
| Service | `PatentSearchService`, `PatentDetailService`, `TranslationService` | 비즈니스 로직 (한국어 감지 → 번역 → 검색) |
| Client | `SerpApiClient`, `DeepLClient` | 외부 API 통신, 응답 매핑, 예외 변환 |

### 레이어 간 규칙

```java
// Service가 Client를 직접 주입받아 사용
@Service
@RequiredArgsConstructor
public class PatentSearchService {
    private final SerpApiClient serpApiClient;
    private final DeepLClient deepLClient;
    // ...
}
```

- Controller → Service 방향으로만 의존
- Service가 여러 Client를 조합해 비즈니스 흐름 제어 (검색 전 한국어 번역)
- Client는 외부 API 오류를 `ExternalApiException`으로 변환해서 상위에 위임

---

## 3. IoC / DI

### IoC (Inversion of Control) 개념

> 객체의 생성·생명주기 관리를 개발자가 아닌 Spring 컨테이너(IoC Container)가 담당.

```
개발자 코드 → new 객체 생성 (X)
Spring Container → Bean 생성 → 의존성 주입 (O)
```

### 의존성 주입 3가지 방식

#### ① 생성자 주입 (권장)

```java
// PatentSearchService.java
@Service
@RequiredArgsConstructor  // Lombok: final 필드 생성자 자동 생성
public class PatentSearchService {
    private final SerpApiClient serpApiClient;  // final → 불변
    private final DeepLClient deepLClient;
}
```

**왜 생성자 주입인가?**
- `final` 키워드 → 주입 후 변경 불가, 불변성 보장
- 순환 의존성을 컴파일 타임에 감지
- 테스트 시 `new Service(mockClient)` 형태로 주입 가능

#### ② @Value를 통한 값 주입

```java
// SerpApiClient.java
public SerpApiClient(
        RestClient.Builder restClientBuilder,
        @Value("${external.serp-api.key:}") String apiKey,    // :뒤가 default값
        @Value("${external.serp-api.base-url:https://serpapi.com}") String baseUrl
) {
    this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    this.apiKey = apiKey;
}
```

**`${key:defaultValue}` 패턴:** 환경변수 없어도 애플리케이션 기동됨. API key 없을 시 mock 응답으로 fallback.

#### ③ @Bean을 통한 수동 등록

```java
// RestClientConfig.java
@Configuration
public class RestClientConfig {
    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
```

`RestClient.Builder`는 프레임워크 객체라 `@Component` 스캔으로 등록 불가 → `@Bean`으로 수동 등록.

### Bean 스테레오타입 어노테이션

| 어노테이션 | 사용 위치 | 이 프로젝트 사례 |
|---|---|---|
| `@Component` | 범용 | `SerpApiClient`, `DeepLClient` |
| `@Service` | 비즈니스 로직 | `PatentSearchService`, `TranslationService` |
| `@RestController` | HTTP 요청 처리 | `PatentController`, `TranslationController` |
| `@Configuration` | Bean 설정 클래스 | `RestClientConfig` |

---

## 4. REST API 설계

### Controller 어노테이션 분석

```java
// PatentController.java
@RestController           // @Controller + @ResponseBody: 모든 메서드 반환값을 JSON으로
@RequestMapping("/api/patents")
@RequiredArgsConstructor
public class PatentController {

    @GetMapping("/search")
    // GET /api/patents/search?query=AI&page=1&size=10
    public PatentSearchResponse search(@Valid @ModelAttribute PatentSearchRequest request) {
        return searchService.search(request);
    }

    @GetMapping("/{id}")
    // GET /api/patents/JP2024000001
    public PatentDetailResponse detail(@PathVariable String id) {
        return detailService.findById(id);
    }
}
```

```java
// TranslationController.java
@PostMapping
// POST /api/translations  body: {"sourceText":"...", "sourceLang":"KO", "targetLang":"JA"}
public TranslationResponse translate(@Valid @RequestBody TranslationRequest request) {
    return translationService.translate(request);
}
```

### @ModelAttribute vs @RequestBody 차이

| | `@ModelAttribute` | `@RequestBody` |
|---|---|---|
| 사용 상황 | GET 쿼리 파라미터, Form 데이터 | POST/PUT JSON body |
| 검증 실패 예외 | `BindException` | `MethodArgumentNotValidException` |
| Content-Type | 없음 (URL 파라미터) | `application/json` |
| 이 프로젝트 | `PatentSearchRequest` | `TranslationRequest` |

---

## 5. Bean Validation

### DTO 유효성 선언

```java
// PatentSearchRequest.java
@Data
public class PatentSearchRequest {
    @NotBlank(message = "검색어를 입력하세요")
    private String query;

    @Min(value = 1, message = "page는 1 이상이어야 합니다")
    private int page = 1;

    @Min(value = 1, message = "size는 1 이상이어야 합니다")
    @Max(value = 50, message = "size는 50 이하이어야 합니다")
    private int size = 10;
}
```

### 유효성 검사 트리거

```java
// Controller 파라미터에 @Valid 붙여야 검사 실행
public PatentSearchResponse search(@Valid @ModelAttribute PatentSearchRequest request)
public TranslationResponse translate(@Valid @RequestBody TranslationRequest request)
```

**@Valid vs @Validated:**
- `@Valid` (jakarta): 기본 Bean Validation, 메서드 파라미터에 사용
- `@Validated` (Spring): 그룹 지정 가능, 클래스 레벨에도 적용 가능

### 주요 제약 어노테이션

| 어노테이션 | 대상 | 의미 |
|---|---|---|
| `@NotBlank` | String | null, 빈 문자열, 공백 문자열 모두 거부 |
| `@NotNull` | Object | null만 거부 |
| `@NotEmpty` | String/Collection | null, 빈값 거부 (공백은 허용) |
| `@Min` / `@Max` | int, long | 최솟값/최댓값 |
| `@Size` | String/Collection | 길이 범위 |
| `@Pattern` | String | 정규식 매칭 |

---

## 6. 예외 처리 전략

### 커스텀 예외 계층

```java
// PatentNotFoundException.java - 도메인 특정 예외
public class PatentNotFoundException extends RuntimeException {
    public PatentNotFoundException(String id) {
        super("Patent not found: " + id);
    }
}

// ExternalApiException.java - 외부 API 실패
public class ExternalApiException extends RuntimeException {
    public ExternalApiException(String message) { super(message); }
    public ExternalApiException(String message, Throwable cause) { super(message, cause); }
}
```

**RuntimeException 상속 이유:** Checked Exception 강제 처리 없이 자연스럽게 상위로 전파.

### @RestControllerAdvice - 전역 예외 처리

```java
@RestControllerAdvice  // @ControllerAdvice + @ResponseBody
@Slf4j
public class GlobalExceptionHandler {

    // 도메인 예외 → 404
    @ExceptionHandler(PatentNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(PatentNotFoundException ex, HttpServletRequest request) {
        log.warn("Patent not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorResponse(404, "Not Found", ex.getMessage(), request.getRequestURI()));
    }

    // 외부 API 오류 → 502 (내 서버 문제 아님을 명시)
    @ExceptionHandler(ExternalApiException.class)
    public ResponseEntity<ApiErrorResponse> handleExternalApi(ExternalApiException ex, HttpServletRequest request) {
        log.error("External API error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(errorResponse(502, "Bad Gateway", "외부 API 호출 중 오류가 발생했습니다.", request.getRequestURI()));
    }

    // @RequestBody 유효성 실패 → 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleRequestBodyValidation(...) { ... }

    // @ModelAttribute 유효성 실패 → 400
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiErrorResponse> handleQueryParamValidation(...) { ... }

    // 미처리 예외 → 500 (메시지 은닉)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneral(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse(500, "Internal Server Error", "서버 내부 오류가 발생했습니다.", request.getRequestURI()));
    }
}
```

**설계 원칙:**
- 500 응답에서 내부 구현 상세(`ex.getMessage()`) 노출 금지 → 보안
- 502 Bad Gateway: "내 서버는 정상, 외부 연동 문제"를 클라이언트에 전달
- `log.warn` vs `log.error`: 예상 가능한 오류(404)는 warn, 예상 불가(500)는 error

### 표준 에러 응답 DTO

```java
// ApiErrorResponse.java
@Data
@Builder
public class ApiErrorResponse {
    private int status;       // HTTP 상태코드
    private String error;     // 상태코드 이름 (e.g. "Not Found")
    private String message;   // 사람이 읽을 수 있는 설명
    private String path;      // 요청 URI
    private LocalDateTime timestamp;
}
```

---

## 7. RestClient - 외부 API 호출

### RestClient란?

Spring 6.1 (Boot 3.2+)에서 도입된 **동기 HTTP 클라이언트**. `RestTemplate`의 후속.

| | `RestTemplate` | `RestClient` | `WebClient` |
|---|---|---|---|
| 방식 | 동기 | 동기 | 비동기 (Reactive) |
| 상태 | Deprecated 예정 | 현재 권장 | Reactive Stack 필요 |
| 스타일 | 메서드 기반 | 빌더/체이닝 | 빌더/체이닝 |

### SerpApiClient 분석

```java
// SerpApiClient.java
@Component
@Slf4j
public class SerpApiClient {

    private final RestClient restClient;

    public SerpApiClient(
            RestClient.Builder restClientBuilder,
            @Value("${external.serp-api.base-url:https://serpapi.com}") String baseUrl
    ) {
        // baseUrl 바인딩된 RestClient 인스턴스 생성
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    public PatentSearchResponse search(PatentSearchRequest request, String searchQuery) {
        JsonNode root = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search.json")
                        .queryParam("engine", "google_patents")
                        .queryParam("q", searchQuery)
                        .queryParam("api_key", apiKey)
                        .build())
                .retrieve()
                .body(JsonNode.class);  // Jackson으로 역직렬화

        assertNoSerpApiError(root);
        return mapSearchResponse(request, root);
    }
}
```

### 예외 처리 패턴

```java
try {
    // RestClient 호출
} catch (RestClientResponseException ex) {
    // HTTP 4xx/5xx 응답 → 상태코드 포함
    throw new ExternalApiException("SerpAPI failed: HTTP " + ex.getStatusCode().value(), ex);
} catch (RestClientException ex) {
    // 네트워크 오류, 타임아웃 등
    throw new ExternalApiException("SerpAPI request failed.", ex);
}
```

**원인 체인(cause chain) 보존:** `new ExternalApiException(message, ex)` → 로그에서 근본 원인 추적 가능.

### mock fallback 패턴

```java
public PatentSearchResponse search(PatentSearchRequest request) {
    if (!hasApiKey()) {
        log.warn("SerpAPI key not configured — mock fallback");
        return mockSearchResponse(request);  // 하드코딩 샘플 데이터 반환
    }
    // 실제 API 호출
}
```

**장점:** API key 없이도 로컬 개발/테스트 가능. 환경 독립성 확보.

### DeepL 연동 - POST 방식

```java
// DeepLClient.java
JsonNode root = restClient.post()
        .uri("/v2/translate")
        .header("Authorization", "DeepL-Auth-Key " + apiKey)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .body(requestBody(request))  // Map<String, Object> 자동 직렬화
        .retrieve()
        .body(JsonNode.class);
```

---

## 8. Lombok

### 이 프로젝트에서 사용한 Lombok 어노테이션

#### @Data

```java
// PatentSearchRequest.java, TranslationRequest.java
@Data
public class PatentSearchRequest {
    // getter, setter, toString, equals, hashCode 자동 생성
}
```

**DTO(요청 객체)에 `@Data` 사용:** setter가 있어야 `@ModelAttribute`, `@RequestBody` 바인딩 가능.

#### @Builder + @Data

```java
// ApiErrorResponse.java, PatentSearchResponse.java
@Data
@Builder
public class PatentSearchResponse {
    private String query;
    private int page;
    // ...
}

// 사용
PatentSearchResponse.builder()
    .query(request.getQuery())
    .page(request.getPage())
    .build();
```

**응답 객체에 `@Builder` 사용:** 명시적 필드 지정, 순서 무관, 가독성 우수.

#### @RequiredArgsConstructor

```java
@Service
@RequiredArgsConstructor  // final 필드를 파라미터로 받는 생성자 자동 생성
public class PatentSearchService {
    private final SerpApiClient serpApiClient;
    private final DeepLClient deepLClient;
}
```

Spring의 생성자 주입과 완벽 조합. `@Autowired` 없이도 DI 동작.

#### @Slf4j

```java
@Slf4j  // private static final Logger log = LoggerFactory.getLogger(this.class);
public class SerpApiClient {
    // log.warn(...), log.error(...), log.debug(...) 바로 사용 가능
}
```

---

## 9. 설정 관리

### @Configuration + @Bean

```java
// RestClientConfig.java
@Configuration
public class RestClientConfig {
    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
```

**왜 `RestClient.Builder`를 Bean으로?**
- `SerpApiClient`와 `DeepLClient`가 각각 다른 `baseUrl`로 빌드
- Builder를 주입받아 각자 `.baseUrl().build()` 호출 → 인스턴스 분리
- `RestClient` 자체가 아닌 `Builder`를 공유 → 싱글톤 RestClient가 되지 않음

### application.properties 대신 @Value

```java
// 프로퍼티 키 : 기본값
@Value("${external.serp-api.key:}") String apiKey
@Value("${external.serp-api.base-url:https://serpapi.com}") String baseUrl
```

환경변수 `EXTERNAL_SERP_API_KEY` 또는 `.env` 파일 또는 `application.properties` 중 어느 방법으로든 주입 가능. Spring의 PropertySource 추상화 덕분.

---

## 10. 면접 예상 Q&A

### Q1. Spring Bean의 기본 스코프는? 왜 그것을 기본으로 쓰나?

**Singleton.** 애플리케이션 컨텍스트에 인스턴스 하나만 생성. 이 프로젝트의 `SerpApiClient`, `PatentSearchService` 모두 싱글톤. 상태를 갖지 않는(stateless) 서비스 클래스에 적합. 매 요청마다 생성하지 않으니 메모리·GC 부담 없음.

---

### Q2. @Component와 @Service의 차이는?

기술적으로 동일(둘 다 Bean 등록). `@Service`는 비즈니스 레이어임을 의미론적으로 표현. AOP 포인트컷 지정 시 레이어 구분에 활용 가능. 팀 컨벤션·가독성 목적.

---

### Q3. 생성자 주입이 필드 주입보다 권장되는 이유는?

1. **불변성:** `final` 필드 → 주입 후 변경 불가
2. **테스트 용이성:** `new Service(mockDep)` 직접 생성 가능
3. **순환 의존성 조기 감지:** 앱 기동 시 즉시 실패 (필드 주입은 런타임에 발견)
4. **NPE 방지:** null 주입 불가 (컨테이너가 보장)

---

### Q4. @RestControllerAdvice가 동작하는 원리는?

Spring MVC의 `DispatcherServlet`이 예외를 캐치 → `HandlerExceptionResolver` 체인 탐색 → `ExceptionHandlerExceptionResolver`가 `@ExceptionHandler` 메서드를 찾아 호출. `@ControllerAdvice`는 모든 Controller에 전역 적용. `@ResponseBody`가 합쳐져 JSON 응답 반환.

---

### Q5. @ModelAttribute와 @RequestBody의 차이는?

`@ModelAttribute`: URL 쿼리 파라미터/폼 데이터 → 필드명 매칭으로 객체 바인딩. `@RequestBody`: HTTP body의 JSON → Jackson으로 역직렬화. 유효성 실패 예외도 다름 (`BindException` vs `MethodArgumentNotValidException`). 이 프로젝트에서 GET 검색은 `@ModelAttribute`, POST 번역은 `@RequestBody`.

---

### Q6. RestClient와 RestTemplate의 차이는?

`RestTemplate`은 Spring 3.x부터 존재하던 동기 클라이언트. 메서드 수가 많고 API가 일관성 없음. `RestClient`는 Spring 6.1에서 도입, 빌더/체이닝 스타일로 가독성 향상. `RestTemplate`은 Deprecated 예정. Reactive가 필요하면 `WebClient`.

---

### Q7. 502 Bad Gateway를 반환한 이유는?

외부 API(`SerpAPI`, `DeepL`) 호출 실패 시 500은 "우리 서버 버그"를 시사해 오해 소지가 있음. 502는 "우리 서버는 정상이나 업스트림 서버 문제"를 명시. 클라이언트가 재시도 전략을 다르게 가져갈 수 있도록 의미를 정확히 전달.

---

### Q8. @NotBlank, @NotNull, @NotEmpty 차이는?

| | null | `""` | `" "` |
|---|---|---|---|
| `@NotNull` | ❌ | ✅ | ✅ |
| `@NotEmpty` | ❌ | ❌ | ✅ |
| `@NotBlank` | ❌ | ❌ | ❌ |

String 입력값에는 `@NotBlank`가 가장 엄격하고 일반적으로 올바른 선택.

---

### Q9. Lombok @Builder와 직접 생성자의 차이는?

Builder 패턴: 파라미터가 많을 때 필드명을 명시해서 가독성 향상, 순서 무관, 선택적 필드 설정 용이. 직접 생성자는 파라미터 순서 실수 위험. 응답 DTO처럼 내부에서 직접 생성하는 객체에 `@Builder`가 적합.

---

### Q10. Mock fallback 패턴의 장단점은?

**장점:**
- API key 없이 로컬 개발/테스트 가능
- 외부 API 다운 시 개발 중단 없음

**단점:**
- Mock 데이터가 실제 API 응답과 다를 경우 통합 문제 늦게 발견
- 프로덕션에서 실수로 mock 응답이 나갈 위험 (key 미설정 시)

**대안:** 프로덕션 환경에서는 key 미설정 시 애플리케이션 기동 자체를 실패시키는 방어 코드 추가 (`@PostConstruct` + 환경별 검사).

---

## 프로젝트 핵심 흐름 요약

```
GET /api/patents/search?query=인공지능
         ↓
PatentController.search() [@Valid @ModelAttribute]
         ↓
PatentSearchService.search()
    ├── containsKorean("인공지능") → true
    ├── DeepLClient.translate("인공지능", KO→JA) → "人工知能"
    └── SerpApiClient.search(request, "人工知能")
              ↓
         SerpAPI (google_patents engine)
              ↓
         JsonNode 파싱 → JP 특허만 필터링
              ↓
PatentSearchResponse (query, page, size, totalCount, items[])
         ↓
JSON 응답 반환
```

---

*작성 기준: Spring Boot 4.0.6 / Java 17 / 2026-05*
