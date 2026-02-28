# CloudFront 및 Route 53 설정과 주요 트러블슈팅 가이드 (CI/CD 이후)

## 🎯 도입: 프로젝트 목표 및 아키텍처 개요

우리는 외국인 관광객을 위한 제주 여행 플래너 서비스를 개발하고 있습니다. 이 서비스의 주요 목표는 다음과 같습니다.

*   **글로벌 사용자 대상**: 동아시아(중국, 대만, 일본) 사용자가 97%, 미국 사용자가 3%를 차지하는 글로벌 서비스입니다.
*   **AI Agent 연동**: 플래너 생성 시 "미국 리전"에서만 사용 가능한 AI Agent를 반드시 호출해야 합니다.
*   **백엔드 단일 리전 구성**: 백엔드 서비스는 AWS Seoul 리전(ap-northeast-2)에 단일 구성됩니다.
*   **CloudFront를 통한 글로벌 접속**: 전 세계 사용자들이 빠르고 안전하게 서비스에 접속할 수 있도록 AWS CloudFront(글로벌 엣지 네트워크)를 사용합니다.

**전체 요청 흐름**: 사용자 → CloudFront → Seoul 백엔드(EKS) → 미국 AI Agent

이 가이드는 CI/CD 파이프라인이 구축된 이후, CloudFront와 Route 53 설정을 통해 글로벌 사용자에게 서비스를 제공하고, 이 과정에서 발생했던 여러 트러블슈팅 경험을 공유하며, 각 설정의 개념과 이유를 자세히 설명합니다.

---

## 🚀 1. CloudFront 및 Route 53 설정 가이드

CloudFront와 Route 53은 전 세계 사용자에게 빠르고 안정적인 서비스를 제공하기 위한 핵심 AWS 서비스입니다.

### 1.1. 🌐 ACM (AWS Certificate Manager) 인증서 발급 (us-east-1, 버지니아 북부)

*   **개념**: HTTPS 통신을 위한 디지털 신분증입니다. 웹사이트의 신뢰성을 보장하고, 클라이언트와 서버 간의 데이터를 암호화하여 보안을 강화합니다.
*   **왜 필요한가요?**: CloudFront는 전 세계에 분산된 엣지 로케이션(Edge Location)을 통해 콘텐츠를 배포합니다. 이 엣지 로케이션에서 SSL/TLS 암호화를 처리하기 위해서는 CloudFront가 인증서를 인식할 수 있어야 합니다. AWS CloudFront 서비스는 `us-east-1` (버지니아 북부) 리전에서 발급된 ACM 인증서만 지원합니다. 따라서 서울 리전에서 발급받은 인증서는 CloudFront에서 사용할 수 없습니다.
*   **설정 방법**:
    1.  AWS 콘솔에서 `AWS Certificate Manager` 서비스로 이동합니다.
    2.  리전을 반드시 `미국 동부 (버지니아 북부) us-east-1`으로 변경합니다.
    3.  "인증서 요청"을 클릭합니다.
    4.  "퍼블릭 인증서 요청"을 선택하고 "다음"을 클릭합니다.
    5.  도메인 이름에 `fall-in-jeju.com`과 `*.fall-in-jeju.com`을 추가합니다. (와일드카드 도메인으로 서브도메인도 함께 보호)
    6.  "DNS 검증" 방식을 선택합니다. (권장)
    7.  "요청"을 클릭합니다.
    8.  생성된 인증서의 세부 정보에서 "Route 53에서 레코드 생성" 버튼을 클릭하여 DNS 레코드(CNAME)를 자동으로 생성하고 검증을 완료합니다. (이 과정은 Route 53에 도메인이 등록되어 있어야 가능합니다.)
*   **기대 효과**: `api.fall-in-jeju.com`과 같은 서브도메인을 포함하여 `fall-in-jeju.com` 도메인에 대한 HTTPS 연결이 가능해져 보안이 강화됩니다.

### 1.2. ☁️ CloudFront Distribution (분배) 생성

*   **개념**: CloudFront는 웹 콘텐츠를 사용자에게 더 가깝게 캐싱하고 배포하여 전송 속도를 향상시키고 부하를 줄이는 CDN(콘텐츠 전송 네트워크) 서비스입니다.
*   **설정 방법**:
    1.  AWS 콘솔에서 `CloudFront` 서비스로 이동합니다.
    2.  "CloudFront 배포 생성"을 클릭합니다.
    3.  **원본 도메인 (Origin Domain)**:
        *   우리 백엔드 서비스가 배포된 EKS 클러스터 앞에 있는 `ALB (Application Load Balancer)`의 DNS 이름을 입력합니다. (예: `k8s-default-backend-xxxxxxxx-xxxxxxxxx.ap-nnortheast-2.elb.amazonaws.com`)
        *   **개념**: CloudFront가 콘텐츠를 가져올 원본 서버를 지정하는 것입니다. 사용자 요청이 CloudFront 엣지 캐시에 없으면, CloudFront는 이 원본 서버로 요청을 전달하여 콘텐츠를 가져옵니다.
        *   **이유**: EKS에 배포된 백엔드 애플리케이션으로의 트래픽을 ALB를 통해 라우팅하기 위함입니다.
    4.  **원본 액세스 (Origin Access)**:
        *   **"Origin access control 설정(권장)"**을 선택하고 새 OAC(Origin Access Control) 설정을 생성합니다.
        *   **개념**: CloudFront가 원본(ALB)에 안전하게 접근할 수 있도록 하는 보안 기능입니다. 이를 통해 CloudFront를 통해서만 원본에 접근할 수 있도록 제한하여 직접적인 ALB 접근을 차단합니다.
        *   **이유**: 원본에 대한 무단 접근을 방지하고 보안을 강화하기 위함입니다.
    5.  **뷰어 프로토콜 정책 (Viewer Protocol Policy)**:
        *   `Redirect HTTP to HTTPS`를 선택합니다.
        *   **개념**: 사용자가 HTTP로 접속해도 자동으로 HTTPS로 리다이렉션하여 항상 암호화된 통신을 사용하도록 강제하는 정책입니다.
        *   **이유**: 보안을 강화하고, SEO(검색 엔진 최적화)에 긍정적인 영향을 줍니다.
    6.  **웹 애플리케이션 방화벽 (WAF)**:
        *   `보안 방어 활성화`를 선택합니다.
        *   **개념**: 웹 애플리케이션을 일반적인 웹 공격으로부터 보호하는 서비스입니다.
        *   **이유**: SQL 인젝션, XSS(크로스 사이트 스크립팅) 등 다양한 웹 공격으로부터 애플리케이션을 보호합니다.
    7.  **대체 도메인 이름 (CNAMEs)**:
        *   `api.fall-in-jeju.com`을 입력합니다.
        *   **개념**: 사용자가 접속할 실제 도메인 이름(사용자 정의 도메인)을 CloudFront 배포와 연결하는 설정입니다.
        *   **이유**: CloudFront가 제공하는 기본 도메인(예: `d111111abcdef8.cloudfront.net`) 대신, 우리가 구매한 `api.fall-in-jeju.com` 도메인으로 서비스에 접속할 수 있도록 합니다.
    8.  **사용자 정의 SSL 인증서 (Custom SSL Certificate)**:
        *   `us-east-1` 리전에서 발급받은 ACM 인증서를 선택합니다.
        *   **개념**: CloudFront에서 HTTPS 통신을 위해 사용할 인증서를 지정하는 것입니다.
        *   **이유**: 엣지 로케이션에서 HTTPS 연결을 종단(terminate) 처리하여 사용자에게 안전한 연결을 제공합니다.
    9.  **Custom Header (사용자 정의 헤더) - `X-Origin-Verify`**:
        *   **트러블슈팅 과정에서 추가**: 처음에는 `OAC`만 사용했지만, 추가적인 보안 강화를 위해 CloudFront가 원본(ALB)으로 요청을 보낼 때 특정 헤더를 포함하도록 설정했습니다.
        *   **개념**: CloudFront가 원본으로 요청을 보낼 때, 미리 정의된 특정 키-값 쌍의 HTTP 헤더를 추가하는 기능입니다. 백엔드 애플리케이션은 이 헤더의 유효성을 검사하여 CloudFront를 거치지 않은 직접적인 접근을 차단할 수 있습니다.
        *   **이유**: `OAC`는 S3 버킷이나 특정 유형의 ALB에 대한 접근을 제한하지만, 모든 시나리오에서 완벽하게 직접 접근을 막기 어려울 수 있습니다. `Custom Header`는 애플리케이션 레벨에서 추가적인 검증을 수행하여 보안 계층을 추가하는 역할을 합니다.
        *   **설정 방법**:
            1.  CloudFront 배포 생성/편집 시 "원본(Origins)" 탭으로 이동합니다.
            2.  원본을 선택하고 "편집"을 클릭합니다.
            3.  "사용자 정의 헤더 추가" 섹션에서 헤더 이름 `X-Origin-Verify`와 값 (예: `fall-in-jeju-header`)을 입력하고 저장합니다. 이 값은 이후 Kubernetes Secret과 Spring Boot 애플리케이션에서 동일하게 사용됩니다.
*   **기대 효과**: 전 세계 사용자에게 빠르고 안전하게 콘텐츠를 전송하고, 웹 공격으로부터 애플리케이션을 보호하며, 사용자 정의 도메인을 통해 서비스에 접근할 수 있게 됩니다. 또한 `X-Origin-Verify` 헤더를 통해 원본 서버에 대한 직접적인 접근을 더욱 강력하게 차단하여 보안을 강화합니다.

### 1.3. 🗺️ Route 53 DNS 설정

*   **개념**: Route 53은 AWS의 도메인 이름 시스템(DNS) 웹 서비스입니다. 도메인 이름을 IP 주소로 변환하여 사용자가 웹사이트를 찾을 수 있도록 돕습니다.
*   **설정 방법**:
    1.  AWS 콘솔에서 `Route 53` 서비스로 이동합니다.
    2.  호스팅 영역(Hosted Zones)에서 `fall-in-jeju.com` 도메인을 선택합니다.
    3.  "레코드 생성"을 클릭합니다.
    4.  **레코드 이름**: `api`를 입력합니다. (이렇게 하면 `api.fall-in-jeju.com` 도메인이 됩니다.)
    5.  **레코드 유형**: `A - IPv4 주소 및 일부 AWS 리소스에 트래픽 라우팅`을 선택합니다.
    6.  **별칭 (Alias)**: "별칭" 스위치를 고, "CloudFront 배포에 대한 별칭"을 선택합니다.
    7.  값/트래픽 라우팅 대상: CloudFront 배포 목록에서 방금 생성한 CloudFront 배포를 선택합니다.
    8.  "레코드 생성"을 클릭합니다.
*   **기대 효과**: 이제 사용자가 `api.fall-in-jeju.com`으로 접속하면, Route 53이 이 요청을 CloudFront 배포로 라우팅하게 됩니다.

---

## 💻 2. 백엔드 애플리케이션 (Spring Boot on EKS) 설정 변경

CloudFront와의 연동 및 안정적인 서비스 운영을 위해 백엔드 애플리케이션 코드와 EKS 배포 설정에 여러 변경이 필요했습니다.

### 2.1. 🔒 `OriginVerifyFilter` 구현 및 적용

CloudFront의 Custom Header 설정과 연동하여 백엔드에 대한 보안을 강화합니다.

*   **개념**: Spring Security의 `OncePerRequestFilter`를 상속받아 사용자 정의 필터를 구현합니다. 이 필터는 모든 요청이 들어올 때마다 `X-Origin-Verify` 헤더의 존재 여부와 값이 일치하는지 검사합니다.
*   **왜 필요한가요?**: CloudFront를 통해서만 백엔드에 접근하도록 보장하기 위함입니다. 만약 누군가 CloudFront를 우회하여 직접 ALB로 요청을 보내더라도, 이 필터가 유효한 `X-Origin-Verify` 헤더가 없음을 감지하고 요청을 차단(`403 Forbidden`)하여 보안을 강화합니다.
*   **설정 방법**:

    1.  **`src/main/java/com/jeju/ormicamp/common/config/OriginVerifyFilter.java` 파일 생성**:

        ```java
        package com.jeju.ormicamp.common.config;

        import jakarta.servlet.FilterChain;
        import jakarta.servlet.ServletException;
        import jakarta.servlet.http.HttpServletRequest;
        import jakarta.servlet.http.HttpServletResponse;
        import org.springframework.beans.factory.annotation.Value;
        import org.springframework.stereotype.Component;
        import org.springframework.web.filter.OncePerRequestFilter;

        import java.io.IOException;
        import java.util.Arrays;
        import java.util.List;

        @Component
        public class OriginVerifyFilter extends OncePerRequestFilter {

            @Value("${origin.verify-secret}")
            private String originVerifySecret;

            // /actuator/** 경로는 검증에서 제외 (헬스 체크 때문)
            private static final List<String> EXCLUDE_URLS = Arrays.asList(
                    "/actuator"
            );

            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                    throws ServletException, IOException {

                String requestUri = request.getRequestURI();

                // EXCLUDE_URLS에 해당하는 경로에 대한 요청은 필터 검증을 건너뜀
                if (EXCLUDE_URLS.stream().anyMatch(requestUri::startsWith)) {
                    filterChain.doFilter(request, response);
                    return;
                }

                String header = request.getHeader("X-Origin-Verify");

                // 헤더가 없거나, 값이 일치하지 않으면 403 Forbidden 응답
                if (header == null || !header.equals(originVerifySecret)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid X-Origin-Verify header");
                    return;
                }
                filterChain.doFilter(request, response);
            }
        }
        ```

    2.  **`src/main/java/com/jeju/ormicamp/common/config/SecurityConfig.java` 수정**:

        ```java
        // ... (생략)
        import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

        @Configuration
        @EnableWebSecurity
        public class SecurityConfig {

            private final JWTUtil jwtUtil;
            private final OriginVerifyFilter originVerifyFilter; // OriginVerifyFilter 주입

            public SecurityConfig(JWTUtil jwtUtil, OriginVerifyFilter originVerifyFilter) {
                this.jwtUtil = jwtUtil;
                this.originVerifyFilter = originVerifyFilter;
            }

            @Bean
            public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

                http
                        .csrf(AbstractHttpConfigurer::disable)
                        .formLogin(AbstractHttpConfigurer::disable)
                        .httpBasic(AbstractHttpConfigurer::disable)
                        .sessionManagement(sess
                                -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                        .authorizeHttpRequests(auth -> auth
                                .requestMatchers("/", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                                .requestMatchers("/api/test","/api/test/**").permitAll()
                                .requestMatchers("/actuator/**").permitAll() // Health Check 엔드포인트 허용
                                // TODO : 로그인 구현 시 삭제 예정
                                .requestMatchers("/api/planner/date","/api/planner/update/**").permitAll()
                                .anyRequest().authenticated()
                        )
                        .addFilterBefore(
                                originVerifyFilter, // OriginVerifyFilter를 JwtAuthorizationFilter 이전에 추가
                                UsernamePasswordAuthenticationFilter.class
                        )
                        .addFilterBefore(
                                new JwtAuthorizationFilter(jwtUtil),
                                UsernamePasswordAuthenticationFilter.class
                        );
                return http.build();
            }
        }
        ```

*   **기대 효과**: CloudFront를 통한 합법적인 요청만 백엔드에 도달하게 되어 보안이 크게 강화됩니다.

### 2.2. 🔑 Kubernetes Secret 생성 (`origin-verify-secret`)

`OriginVerifyFilter`에서 사용할 비밀 값을 안전하게 EKS 클러스터에 저장합니다.

*   **개념**: 쿠버네티스 Secret은 암호, OAuth 토큰, SSH 키와 같이 민감한 정보를 저장하는 데 사용되는 오브젝트입니다. Base64로 인코딩되어 저장되며, Pod에 환경 변수나 볼륨으로 주입될 수 있습니다.
*   **왜 필요한가요?**: `X-Origin-Verify` 헤더의 값은 중요한 보안 정보이므로, 코드 내에 직접 하드코딩하는 대신 Secret으로 관리해야 합니다.
*   **설정 방법**:

    1.  다음 명령어를 사용하여 Secret을 생성합니다. (`<YOUR_SECRET_VALUE>`는 CloudFront에 설정한 값과 동일해야 합니다.)

        ```bash
        kubectl create secret generic origin-verify-secret --from-literal=ORIGIN_VERIFY_SECRET='<YOUR_SECRET_VALUE>' -n prod
        ```

        *   `-n prod`: `prod` 네임스페이스에 Secret을 생성합니다.

*   **기대 효과**: 민감한 정보가 안전하게 관리되며, 애플리케이션은 이 Secret을 환경 변수로 주입받아 사용할 수 있습니다.

### 2.3. 📝 `k8s-manifests/base/deployment.yml` 환경 변수 설정

Pod가 `OriginVerifyFilter`에서 사용할 값을 환경 변수로 받을 수 있도록 `deployment.yml`을 수정합니다.

*   **`ORIGIN_VERIFY_SECRET` 환경 변수 주입**:

    ```yaml
    # ... (생략)
             env:
             # SPRING_PROFILES_ACTIVE는 overlay에서 정의됨 (dev/prod)
             - name: SERVER_PORT # Explicitly set server port
               value: "8001"
             # ... (기존 환경 변수들) ...
             - name: ORIGIN_VERIFY_SECRET
               valueFrom:
                 secretKeyRef:
                   name: origin-verify-secret # 위에 생성한 Secret 이름
                   key: ORIGIN_VERIFY_SECRET  # Secret 내부의 키 이름
    ```

*   **`SERVER_PORT: 8001` 명시적 설정 (포트 불일치 문제 해결)**:

    ```yaml
    # ... (생략)
             ports:
             - containerPort: 8001 # 애플리케이션이 8001 포트에서 실행되도록 명시
             resources:
               requests:
                 cpu: "250m"
                 memory: "512Mi"
               limits:
                 cpu: "1000m"
                 memory: "1Gi"
             envFrom:
             # ... (생략)
             env:
             - name: SERVER_PORT # Spring Boot 애플리케이션의 포트를 8001로 강제 설정
               value: "8001"
    ```

    *   **개념 및 이유**: Spring Boot 애플리케이션은 기본적으로 8080 포트에서 실행될 수 있지만, 컨테이너 환경에서는 명시적으로 지정해주는 것이 좋습니다. 이전에 애플리케이션이 8080에서 실행되고 있는데 쿠버네티스 서비스와 ALB Target Group이 8001을 보고 있어 `502 Bad Gateway` 오류가 발생한 적이 있습니다. `SERVER_PORT` 환경 변수를 통해 애플리케이션의 포트를 8001로 강제 설정하고, `containerPort` 및 `service.yml`의 `targetPort`도 8001로 일관되게 맞춰주어 이 문제를 해결했습니다.

*   **기대 효과**: Pod는 `ORIGIN_VERIFY_SECRET` 값을 환경 변수로 주입받아 `OriginVerifyFilter`에서 사용할 수 있으며, 애플리케이션은 의도한 포트(8001)에서 안정적으로 실행됩니다.

### 2.4. ⚙️ `application-*.yml` 설정

Spring Boot 애플리케이션이 `ORIGIN_VERIFY_SECRET` 값을 `@Value` 어노테이션을 통해 주입받을 수 있도록 설정합니다.

*   **`src/main/resources/application-prod.yml`, `application-dev.yml`, `application-local.yml` 수정**:

    ```yaml
    # ... (생략)
    origin:
      verify-secret: ${ORIGIN_VERIFY_SECRET} # 환경 변수에서 값 주입
    server:
      port: 8001 # 애플리케이션 포트 명시
    ```

*   **개념 및 이유**: `@Value("${origin.verify-secret}")` 어노테이션은 Spring 애플리케이션의 설정 파일(yml)에서 `origin.verify-secret`이라는 이름의 속성을 찾아 값을 주입합니다. `$ {변수}` 문법은 해당 속성 값을 환경 변수에서 가져오도록 지시합니다. 따라서 `deployment.yml`에서 주입된 `ORIGIN_VERIFY_SECRET` 환경 변수가 이 속성에 매핑되어 사용됩니다. `server.port` 설정은 애플리케이션 포트를 8001로 명시하여 혼란을 방지합니다.

*   **기대 효과**: `OriginVerifyFilter`가 `ORIGIN_VERIFY_SECRET` 값을 안전하게 주입받아 사용할 수 있습니다.

### 2.5. 🧩 `build.gradle` Actuator 의존성 추가

Spring Boot Actuator를 통해 헬스 체크 엔드포인트(` /actuator/health`)를 활성화합니다.

*   **개념**: Spring Boot Actuator는 Spring Boot 애플리케이션을 모니터링하고 관리하는 데 도움이 되는 기능을 제공합니다. 헬스 체크, 메트릭, 환경 정보 등을 포함합니다.
*   **왜 필요한가요?**: ALB Target Group 헬스 체크가 `/actuator/health` 엔드포인트를 사용하기 때문에, 이 엔드포인트가 제대로 작동해야 Pod가 `Healthy` 상태로 인식됩니다. 이전에 Actuator 의존성이 없어 `500 Internal Server Error`가 발생했습니다.
*   **설정 방법**: `build.gradle` 파일의 `dependencies` 블록에 다음 라인을 추가합니다.

    ```gradle
    // ... (생략)
    dependencies {
        // ... (기존 의존성) ...
        implementation 'org.springframework.boot:spring-boot-starter-actuator' // Actuator 의존성 추가
    }
    ```

*   **기대 효과**: `/actuator/health` 엔드포인트가 활성화되어 ALB 헬스 체크가 정상적으로 이루어지고, Pod가 `Healthy` 상태로 유지됩니다.

### 2.6. ✅ `application-prod.yml` Actuator 설정 추가

프로덕션 환경에서 Actuator 엔드포인트를 노출하고 상세 정보를 확인하도록 설정합니다.

*   **개념**: Actuator의 엔드포인트는 기본적으로 노출되지 않거나 제한적으로 노출됩니다. `management.endpoints.web.exposure.include` 설정을 통해 어떤 엔드포인트를 HTTP로 노출할지 지정할 수 있습니다. `management.endpoint.health.show-details`는 헬스 체크의 상세 정보를 보여줄지 여부를 결정합니다.
*   **왜 필요한가요?**: `/actuator/health` 엔드포인트가 활성화되더라도, 명시적으로 노출 설정을 해주지 않으면 외부에서 접근할 수 없습니다. 상세 정보를 노출하는 것은 헬스 체크 실패 시 디버깅에 유용합니다.
*   **설정 방법**: `src/main/resources/application-prod.yml` 파일에 다음 내용을 추가합니다.

    ```yaml
    management:
      endpoints:
        web:
          exposure:
            include: health,prometheus # health 엔드포인트 노출
      endpoint:
        health:
          show-details: always # 상세 정보 항상 노출
    ```

*   **기대 효과**: `/actuator/health` 엔드포인트가 외부에서 접근 가능해지고, 헬스 체크 실패 시 더 자세한 정보를 얻을 수 있어 트러블슈팅에 도움이 됩니다.

### 2.7. 🚫 `application-prod.yml` Redis 헬스 체크 비활성화

EKS 환경에 Redis 서버가 배포되지 않은 상태에서 발생하는 Redis 연결 오류를 방지합니다.

*   **개념**: Spring Boot Actuator는 애플리케이션에 포함된 다양한 컴포넌트(DB, Redis 등)에 대한 헬스 체크를 자동으로 수행합니다. `management.health.redis.enabled: false`는 Redis에 대한 자동 헬스 체크를 비활성화하는 설정입니다.
*   **왜 필요한가요?**: EKS 클러스터에 Redis 서버가 배포되어 있지 않다면, 애플리케이션은 Redis 연결을 시도하다가 "Connection refused" 오류를 발생시키고 헬스 체크가 실패하여 `500 Internal Server Error` (그리고 결국 `502 Bad Gateway`)를 유발할 수 있습니다. 헬스 체크만 비활성화하면 애플리케이션 자체의 Redis 연결 시도는 막지 못하지만, 최소한 헬스 체크 실패로 인한 서비스 다운은 방지할 수 있습니다.
*   **설정 방법**: `src/main/resources/application-prod.yml` 파일에 다음 내용을 추가합니다.

    ```yaml
    management:
    # ... (기존 Actuator 설정) ...
      endpoint:
        health:
    # ... (기존 health 설정) ...
          redis:
            enabled: false # Redis 헬스 체크 비활성화
    ```

*   **기대 효과**: Redis 서버가 EKS에 배포되지 않은 상태에서도 애플리케이션의 헬스 체크가 성공적으로 이루어져 `500 Internal Server Error` 및 `502 Bad Gateway` 문제를 해결할 수 있습니다.

---

## 🛠️ 3. CI/CD 파이프라인 및 Git 관리 관련 트러블슈팅

CloudFront 및 백엔드 설정을 진행하면서 Git 브랜치 관리 및 CI/CD 파이프라인 운영과 관련하여 여러 문제가 발생했습니다.

### 3.1. 🔁 GitHub `main` 브랜치 자동 커밋 문제

*   **개념**: AWS CodeBuild는 CI/CD 파이프라인의 일부로, 코드를 빌드하고 Docker 이미지를 생성한 후, 이 이미지 태그를 Kubernetes `deployment.yml` 파일에 업데이트하고 `main` 브랜치에 자동으로 커밋 및 푸시하도록 설정되어 있었습니다.
*   **이유**: `buildspec.yml` 파일에 다음과 같은 명령어가 포함되어 있기 때문입니다.
    ```yaml
    post_build:
      commands:
        # ...
        - sed -i "s|image:.*|image:\ $REPOSITORY_URI:$IMAGE_TAG|g" k8s-manifests/base/deployment.yml
        # ...
        - git commit -m "chore: Update image to $IMAGE_TAG [skip ci]"
        - git push origin HEAD:main
    ```
    이 명령어들은 CodeBuild가 성공적으로 빌드를 마치면 `k8s-manifests/base/deployment.yml` 파일의 Docker 이미지 태그를 최신으로 업데이트하고, 이 변경 사항을 `main` 브랜치에 커밋하고 푸시합니다. `[skip ci]`는 이 자동 커밋이 또 다른 CI/CD 트리거를 발생시키지 않도록 하는 지시어입니다.
*   **트러블슈팅 및 해결**:
    *   이는 의도된 CI/CD 동작이므로, `main` 브랜치에 새로운 커밋이 계속 남는 것은 정상입니다.
    *   **문제**: 로컬 `main` 브랜치에서 작업하거나 `dev` 브랜치를 `main`으로 병합하려고 할 때, 원격 `main` 브랜치에 CodeBuild가 푸시한 새로운 커밋들이 존재하여 병합 충돌(`merge conflict`)이나 푸시 거부(`non-fast-forward`)가 자주 발생했습니다.
    *   **해결**: `git pull origin main --rebase` 명령어를 사용하여 원격 `main` 브랜치의 변경 사항을 먼저 가져와 로컬 변경 사항 위에 재배치(rebase)하는 전략을 사용했습니다. 이렇게 하면 불필요한 병합 커밋을 줄이고 선형적인 Git 기록을 유지할 수 있습니다.

### 3.2. 💥 Git 병합 충돌 (`deployment.yml`, `application-prod.yml` 등)

*   **개념**: Git 병합 충돌은 두 개 이상의 브랜치가 동일한 파일의 동일한 부분을 다르게 변경했을 때 Git이 어떤 변경을 적용해야 할지 자동으로 결정할 수 없을 때 발생합니다.
*   **이유**:
    *   `deployment.yml`: CodeBuild의 자동 이미지 태그 업데이트 (`main` 브랜치)와 로컬 `dev` 브랜치에서 수동으로 또는 다른 이유로 `deployment.yml`이 변경되었을 때.
    *   `application-prod.yml`: `dev` 브랜치에서 `Redis` 헬스 체크 비활성화 설정이 추가되었고, `main` 브랜치에는 이 설정이 없었을 때.
*   **트러블슈팅 및 해결**:
    1.  **충돌 확인**: `git status` 명령어로 충돌이 발생한 파일을 확인합니다.
    2.  **파일 열기**: 충돌이 발생한 파일을 텍스트 편집기로 엽니다. 파일 내에서 `<<<<<<< HEAD`, `=======`, `>>>>>>> <branch_name>`과 같은 충돌 마커를 찾습니다.
    3.  **수동 편집**: 원하는 내용을 선택하거나 두 브랜치의 변경 사항을 조합하여 파일을 수동으로 편집합니다. 이때 충돌 마커는 반드시 제거해야 합니다.
        *   예: `application-prod.yml`의 Redis 헬스 체크 충돌 시, `redis.enabled: false` 설정이 포함된 `dev` 브랜치의 내용을 유지하도록 선택했습니다.
    4.  **스테이징**: `git add <충돌 해결된 파일 경로>` 명령어로 충돌 해결된 파일을 스테이징 영역에 추가합니다. (예: `git add src/main/resources/application-prod.yml`)
    5.  **병합 커밋**: `git commit -m "Merge branch 'dev' into main, resolving conflicts"`와 같이 병합 커밋 메시지를 작성하고 커밋합니다.

### 3.3. ⛔ `git push rejected (non-fast-forward)`

*   **개념**: 원격 저장소에 로컬 저장소에 없는 변경 사항(새로운 커밋)이 존재할 때, Git은 로컬 브랜치를 원격 브랜치에 "non-fast-forward" 방식으로 푸시하는 것을 거부합니다. 이는 로컬과 원격 간의 기록이 분기되어 있을 때 발생하며, 강제로 푸시하면 원격 저장소의 기록이 손실될 수 있기 때문입니다.
*   **이유**: 주로 CodeBuild의 자동 커밋으로 인해 원격 `main` 브랜치가 로컬 `main` 브랜치보다 앞서 나갈 때 발생했습니다.
*   **트러블슈팅 및 해결**:
    1.  **원격 변경 사항 가져오기**: `git pull origin <branch_name>` 명령어를 사용하여 원격 브랜치의 변경 사항을 먼저 가져옵니다. 이때 `main` 브랜치의 경우 `git pull origin main --rebase`를 사용하여 자동 커밋으로 인한 불필요한 병합 커밋을 피했습니다.
    2.  **재시도**: 풀(pull) 작업이 완료된 후, 다시 `git push origin <branch_name>` 명령어를 실행하여 변경 사항을 푸시합니다.

### 3.4. ❓ `@Value` 어노테이션 인식 문제

*   **개념**: Spring Boot 애플리케이션에서 `@Value` 어노테이션은 설정 파일(예: `application.yml`, `application-prod.yml`)이나 환경 변수로부터 속성 값을 주입받는 데 사용됩니다.
*   **이유**: `JwtConfig.java`와 같은 `Config` 클래스에서 `@Value("${jwt.secret}")`와 같은 형태로 값을 주입받으려 했으나, 해당 속성(`jwt.secret`, `cognito.jwks-url` 등)이 `application-*.yml` 파일에 정의되어 있지 않았거나, 환경 변수 매핑이 제대로 되지 않았을 때 발생했습니다.
*   **트러블슈팅 및 해결**:
    1.  **`application-*.yml` 파일에 속성 정의**: `jwt.secret`, `cognito.user-pool-id`, `cognito.issuer` 등의 필요한 속성들을 `application-local.yml`, `application-dev.yml`, `application-prod.yml` 파일에 정의했습니다. 이때 `$ {변수}` 문법을 사용하여 환경 변수에서 값을 가져오도록 설정했습니다.
        *   예:
            ```yaml
            jwt:
              secret: ${JWT_SECRET}
            cognito:
              user-pool-id: ${COGNITO_USER_POOL_ID}
              issuer: ${COGNITO_ISSUER}
              jwks-url: ${COGNITO_JWKS_URL}
              client-id: ${COGNITO_CLIENT_ID}
              client-secret: ${COGNITO_CLIENT_SECRET}
              redirect-uri: ${COGNITO_REDIRECT_URI}
              domain: ${COGNITO_DOMAIN}
            ```
    2.  **`.env` 파일 설정 (로컬 환경)**: 로컬 개발 환경에서는 `spring-dotenv` 라이브러리를 통해 `.env` 파일에 정의된 환경 변수들을 로드하도록 했습니다. `JWT_SECRET`, `COGNITO_USER_POOL_ID` 등 모든 Cognito 및 JWT 관련 환경 변수를 `.env` 파일에 추가했습니다.
    3.  **Kubernetes Secret 생성 및 `deployment.yml` 환경 변수 주입 (배포 환경)**: EKS 배포 환경에서는 `kubectl create secret` 명령어로 Kubernetes Secret을 생성하고, `deployment.yml` 파일에서 `valueFrom.secretKeyRef`를 사용하여 이 Secret의 값을 Pod의 환경 변수로 주입하도록 설정했습니다.
        *   예:
            ```yaml
            - name: JWT_SECRET
              valueFrom:
                secretKeyRef:
                  name: jwt-secret
                  key: JWT_SECRET
            ```
*   **기대 효과**: `@Value` 어노테이션이 설정 파일이나 환경 변수로부터 값을 정확하게 주입받아 애플리케이션이 정상적으로 초기화되고 실행됩니다.

### 3.5. 💀 Pod `CrashLoopBackOff` (OOMKilled)

*   **개념**: `CrashLoopBackOff`는 Kubernetes Pod가 반복적으로 시작 및 종료되는 상태를 나타냅니다. `OOMKilled` (Out Of Memory Killed)는 컨테이너가 할당된 메모리 제한을 초과하여 Linux 커널에 의해 강제 종료되었을 때 발생하는 특정 유형의 `CrashLoopBackOff`입니다. 종료 코드는 일반적으로 `137`입니다.
*   **이유**: `k8s-manifests/base/deployment.yml`에 설정된 Pod의 CPU/메모리 리소스 요청량(`requests`) 및 제한량(`limits`)이 실제 Spring Boot 애플리케이션의 요구사항보다 너무 낮게 설정되어 있었기 때문입니다. 특히 Spring Boot 애플리케이션은 시작 시 상당한 메모리를 사용합니다.
*   **트러블슈팅 및 해결**:
    1.  **로그 및 이벤트 확인**: `kubectl describe pod <pod_name> -n <namespace>` 및 `kubectl logs <pod_name> -n <namespace>` 명령어를 사용하여 Pod의 이벤트와 로그에서 `OOMKilled` 메시지를 확인했습니다.
    2.  **`k8s-manifests/base/deployment.yml` 수정**: `deployment.yml` 파일에서 `resources.requests` 및 `resources.limits`의 `cpu`와 `memory` 값을 증대했습니다.
        *   예:
            ```yaml
            resources:
              requests:
                cpu: "250m" # 0.25 코어 요청
                memory: "512Mi" # 512 MiB 메모리 요청
              limits:
                cpu: "1000m" # 1 코어 제한
                memory: "1Gi" # 1 GiB 메모리 제한
            ```
*   **기대 효과**: Pod가 충분한 리소스를 할당받아 안정적으로 실행되며, `CrashLoopBackOff` 상태가 해결되어 서비스 가용성이 향상됩니다.

### 3.6. 🚪 `502 Bad Gateway` 및 `500 Internal Server Error`

*   **개념**:
    *   `502 Bad Gateway`: 게이트웨이 또는 프록시 서버(여기서는 CloudFront 또는 ALB)가 업스트림 서버(여기서는 EKS Pod)로부터 유효하지 않은 응답을 받았을 때 발생합니다.
    *   `500 Internal Server Error`: 서버 내부에서 예상치 못한 오류가 발생하여 요청을 처리할 수 없을 때 발생합니다.
*   **이유 및 트러블슈팅 & 해결 과정**:
    1.  **초기 `502 Bad Gateway` (ALB Target Group Unhealthy)**:
        *   **문제**: CloudFront를 통해 접속했을 때 `502 Bad Gateway`가 발생했고, AWS 콘솔에서 ALB의 Target Group에 등록된 Pod들이 `Unhealthy` 상태였습니다.
        *   **원인**: Pod 내부에서 애플리케이션이 정상 작동하지 않거나, ALB 헬스 체크 경로에 대한 응답이 유효하지 않았기 때문입니다.
    2.  **포트 불일치 문제**:
        *   **문제**: Pod 로그를 확인했을 때 애플리케이션이 8080 포트에서 실행되고 있었으나, `k8s-manifests/base/deployment.yml`의 `containerPort`와 `k8s-manifests/service.yml`의 `targetPort`는 8001로 설정되어 있었습니다. 또한 ALB Target Group의 헬스 체크도 8001 포트를 보고 있었습니다.
        *   **원인**: 애플리케이션의 실제 실행 포트와 Kubernetes Service, ALB Target Group이 바라보는 포트 간의 불일치.
        *   **해결**:
            *   사용자님의 요청에 따라 **애플리케이션이 8001 포트에서 실행되도록 통일**하기로 결정했습니다.
            *   `k8s-manifests/base/deployment.yml`의 `containerPort`를 8001로 유지하고, `SERVER_PORT: 8001` 환경 변수를 추가하여 Spring Boot 애플리케이션이 명시적으로 8001 포트에서 시작되도록 강제했습니다.
            *   `k8s-manifests/service.yml`의 `targetPort`도 8001로 변경했습니다.
    3.  **`403 Forbidden` (Custom Header 누락)**:
        *   **문제**: 포트 불일치 문제가 해결된 후, CloudFront를 통해 `/actuator/health` 엔드포인트에 접근했을 때 `403 Forbidden` 오류가 발생했습니다.
        *   **원인**: CloudFront와 ALB 간의 `X-Origin-Verify` Custom Header를 설정했지만, 백엔드 애플리케이션에서 이 헤더의 유효성을 검사하는 로직이 없었기 때문에 ALB에 직접 접근하는 것으로 간주되어 차단되었습니다.
        *   **해결**: `OriginVerifyFilter.java`를 구현하고 `SecurityConfig.java`에 추가하여 `X-Origin-Verify` 헤더의 유효성을 검사하도록 했습니다. `/actuator/**` 경로는 헬스 체크를 위해 이 필터의 검증에서 제외했습니다.
    4.  **`500 Internal Server Error` (Actuator 누락)**:
        *   **문제**: `403 Forbidden` 문제가 해결된 후, 다시 `/actuator/health` 엔드포인트에 접근했을 때 `500 Internal Server Error`가 발생했습니다. Pod 로그에는 특별한 `ERROR` 스택 트레이스가 없었지만, ALB Target Group 헬스 체크가 실패하고 있었습니다.
        *   **원인**: `build.gradle`에 `spring-boot-starter-actuator` 의존성이 누락되어 있었고, `application-prod.yml`에 Actuator 엔드포인트 노출 설정도 되어 있지 않았기 때문입니다. 따라서 `/actuator/health` 엔드포인트가 제대로 작동하지 않았습니다.
        *   **해결**: `build.gradle`에 `spring-boot-starter-actuator` 의존성을 추가하고, `application-prod.yml`에 Actuator 엔드포인트 노출 및 상세 정보 표시 설정을 추가했습니다.
    5.  **`500 Internal Server Error` 및 `502 Bad Gateway` (Redis 연결 오류)**:
        *   **문제**: Actuator 설정 후에도 여전히 `500 Internal Server Error`와 `502 Bad Gateway`가 간헐적으로 발생했으며, Pod 로그에는 "Connection refused: localhost/127.0.0.1:6379"와 같은 Redis 연결 오류가 계속 나타났습니다. `application.yml`에 `spring.profiles.active: local` 설정이 남아있었습니다.
        *   **원인**:
            *   `application.yml`의 `spring.profiles.active: local` 설정이 배포 환경에서도 로컬 프로파일을 활성화시켜 Redis 연결을 시도하게 만들 수 있었습니다. (이 설정은 잠시 제거했지만 사용자님의 요청에 따라 로컬 환경을 위해 다시 추가되었습니다.)
            *   `Redis` 헬스 체크가 활성화된 상태에서 EKS 클러스터에 Redis 서버가 배포되어 있지 않아, 애플리케이션이 Redis에 연결하지 못하고 헬스 체크가 실패하면서 `500` 오류를 유발했습니다.
        *   **해결**: `application-prod.yml`에 `management.health.redis.enabled: false` 설정을 추가하여 Redis 헬스 체크를 비활성화했습니다. (Redis가 EKS에 배포되지 않았을 때 임시적인 해결책이며, Redis를 사용해야 한다면 EKS에 Redis를 배포해야 합니다.)

### 3.7. ✍️ `git rebase` 중 에디터 프롬프트 문제

*   **개념**: `git rebase` 명령어를 사용하여 변경 이력을 재작성하거나 브랜치를 깔끔하게 병합할 때, 충돌이 발생하여 수동으로 해결한 후 Git은 커밋 메시지를 편집하기 위해 기본 텍스트 에디터(예: Vim)를 자동으로 실행하려고 시도합니다.
*   **이유**: 터미널 환경에서 `EDITOR` 환경 변수가 설정되어 있지 않거나, 설정된 에디터가 터미널에서 실행될 수 없을 때 "Terminal is dumb, but EDITOR unset"과 같은 오류 메시지가 나타나면서 진행이 멈춥니다.
*   **트러블슈팅 및 해결**:
    1.  **에러 확인**: `error: Terminal is dumb, but EDITOR unset` 메시지가 나타나는 것을 확인합니다.
    2.  **`GIT_EDITOR` 환경 변수 설정**: PowerShell 환경에서 다음 명령어를 실행하여 `GIT_EDITOR` 환경 변수를 `true`로 설정했습니다.
        ```powershell
        $env:GIT_EDITOR="true"
        ```
        이 설정은 Git이 충돌 해결 후 커밋 메시지를 수동으로 편집하는 대신, 기본 커밋 메시지를 자동으로 사용하고 에디터 실행을 건너뛰도록 합니다.
    3.  **`git rebase --continue` 재시도**: 환경 변수 설정 후 `git rebase --continue` 명령어를 다시 실행하여 rebase 작업을 계속 진행했습니다.

---

## 💡 마무리: 현재 상태 및 다음 단계

현재까지 CloudFront 및 Route 53 설정, 백엔드 애플리케이션의 보안 및 헬스 체크 관련 설정, 그리고 Git 관리 및 CI/CD 과정에서 발생했던 다양한 트러블슈팅을 통해 서비스를 안정화하고 있습니다.

**현재 남아있는 주요 문제점은 Redis 연결 오류입니다.** `management.health.redis.enabled: false` 설정을 통해 헬스 체크는 비활성화했지만, 애플리케이션 코드 내에서 여전히 Redis 관련 빈이 로드되면서 연결을 시도하고 "Connection refused" 오류가 발생하고 있습니다.

**다음 단계에서는 이 Redis 연결 오류를 완전히 해결해야 합니다.**

*   **선택 1: Redis를 프로덕션 환경에서 사용하지 않는 경우**: `build.gradle` 파일에서 `org.springframework.boot:spring-boot-starter-data-redis`와 `org.springframework.session:spring-session-data-redis` 의존성을 완전히 제거합니다. 이렇게 하면 애플리케이션이 Redis 관련 빈을 생성하려는 시도 자체를 하지 않아 연결 오류가 발생하지 않을 것입니다.
*   **선택 2: Redis를 프로덕션 환경에서 사용하는 경우**: EKS 클러스터 내에 Redis 서버를 배포하고, 애플리케이션이 이 배포된 Redis 서버에 연결하도록 설정을 변경해야 합니다. Kubernetes `StatefulSet`과 `Service`를 사용하여 Redis를 배포하고, `application-prod.yml` 및 `deployment.yml`에서 Redis 서버의 호스트 이름과 포트를 설정해야 합니다.

어떤 선택을 하실지에 따라 다음 단계를 진행하겠습니다. 가이드가 초보자 분께 도움이 되기를 바랍니다. 궁금한 점이 있다면 언제든지 다시 질문해주세요!

