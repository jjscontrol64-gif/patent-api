# Repository Guidelines

## 프로젝트 구조와 모듈 구성

이 저장소는 Gradle 기반 단일 모듈 Spring Boot API 프로젝트입니다. 주요 Java 코드는 `src/main/java/com/jjs/patentapi` 아래에 위치합니다.

- `controller/`: 특허 검색, 상세 조회, 번역, 루트 상태 확인 HTTP 엔드포인트
- `service/`: 애플리케이션 유스케이스와 비즈니스 흐름
- `client/`: SerpAPI, DeepL 등 외부 API 연동 어댑터
- `dto/`: 요청/응답 데이터 모델
- `exception/`: 도메인 예외와 전역 예외 처리
- `src/main/resources/application.properties`: 실행 설정
- `src/test/java/com/jjs/patentapi`: JUnit 테스트

`build/`는 Gradle 산출물 디렉터리이므로 직접 수정하지 않습니다.

## 빌드, 테스트, 실행 명령

환경 차이를 줄이기 위해 Gradle Wrapper를 사용합니다.

```bash
./gradlew build
```

컴파일, 테스트, 실행 가능한 JAR 생성을 모두 수행합니다.

```bash
./gradlew test
```

JUnit 테스트만 실행합니다.

```bash
./gradlew bootRun
```

로컬에서 API 서버를 `http://localhost:8080`으로 실행합니다.

```bash
java -jar build/libs/patent-api-0.0.1-SNAPSHOT.jar
```

빌드된 JAR 파일로 애플리케이션을 실행합니다.

## 코딩 스타일과 네이밍 규칙

Java 17과 기존 Spring Boot 관례를 따릅니다. 패키지는 `com.jjs.patentapi` 아래에 유지하고, Java 코드는 4칸 들여쓰기를 사용합니다.

클래스명은 역할이 드러나게 작성합니다. 예: `PatentController`, `PatentSearchService`, `SerpApiClient`, `PatentSearchRequest`.

의존성 주입은 Lombok `@RequiredArgsConstructor` 기반 생성자 주입을 우선합니다. Controller는 얇게 유지하고, 비즈니스 흐름은 Service에, 외부 HTTP 연동은 Client에 둡니다.

## 테스트 가이드라인

테스트는 JUnit 5와 Gradle `useJUnitPlatform()`을 사용합니다. 테스트 파일은 실제 코드와 같은 패키지 구조로 `src/test/java` 아래에 둡니다. 테스트 클래스명은 `*Tests` 접미사를 사용합니다.

변경 전후로 다음 명령을 실행합니다.

```bash
./gradlew clean build
```

Controller, Validation, Exception Handler, Service 로직을 변경할 때는 해당 동작을 검증하는 테스트를 추가합니다.

## 커밋과 Pull Request 규칙

현재 작업 디렉터리는 Git 저장소로 인식되지 않아 기존 커밋 규칙을 확인할 수 없습니다. 커밋 메시지는 명령형으로 짧고 명확하게 작성합니다.

예:

- `Fix root endpoint 500 response`
- `Add patent search validation`

Pull Request에는 변경 요약, 테스트 결과, 설정 변경 여부, API 계약 변경 여부를 포함합니다. 엔드포인트 변경 시에는 예시 요청/응답을 함께 적습니다.

## 보안과 설정

실제 API 키는 커밋하지 않습니다. 외부 연동 키는 환경 변수로 주입합니다.

```bash
SERP_API_KEY=your_serp_api_key
DEEPL_API_KEY=your_deepl_api_key
```

외부 키가 없는 로컬 환경에서도 예측 가능한 방식으로 동작하도록 기본값과 예외 처리를 유지합니다.
