# 🌏 글로벌 Jeju 플래너 서비스 아키텍처 가이드

## 📊 서비스 개요

**타겟 사용자**: 외국인 관광객 (동아시아 97%, 미국 3%)
**제약사항**: AI Agent는 미국 리전에서만 사용 가능
**전략**: Seoul 백엔드 + CloudFront를 통한 글로벌 최적화

---

## 🏗️ 전체 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                        전 세계 사용자                              │
│  (중국, 대만, 일본, 한국, 미국, 기타)                                │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│           CloudFront (Global Edge Locations)                     │
│  • 220+ Edge Locations 전 세계 분산                               │
│  • Static Content 캐싱 (이미지, CSS, JS)                          │
│  • API Response 캐싱 (선택적)                                     │
│  • DDoS Protection (AWS Shield Standard)                         │
│  • SSL/TLS 종료                                                     │
└────────────────────┬────────────────────────────────────────────┘
                     │ HTTPS
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│              ALB (Application Load Balancer)                     │
│              Region: ap-northeast-2 (Seoul)                      │
│  • Path-based Routing                                            │
│  • Health Check                                                  │
│  • SSL Certificate (ACM)                                         │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│                  EKS Cluster (Seoul)                             │
│  ┌────────────────────────────────────────────────────────┐     │
│  │  Backend Pods (3 replicas)                              │     │
│  │  • Spring Boot Application                              │     │
│  │  • Health Check: /actuator/health                       │     │
│  │  • DynamoDB (Seoul)                                     │     │
│  └────────────────────────────────────────────────────────┘     │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼ (플래너 생성 요청만)
┌─────────────────────────────────────────────────────────────────┐
│                 AI Agent (US Region)                             │
│  • AWS Bedrock / SageMaker                                       │
│  • 플래너 생성 AI 로직                                             │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🚀 Phase 1: ALB 및 Ingress 구성 (완료)

### ✅ 완료된 작업:

1. **Ingress 생성**: `k8s-manifests/ingress.yml`
2. **Service 수정**: LoadBalancer → ClusterIP
3. **Health Check 엔드포인트**: Spring Boot Actuator 추가
4. **Security 설정**: `/actuator/health` 허용

### 배포 방법:

```bash
# 1. 새 파일들을 Git에 추가
git add k8s-manifests/ingress.yml
git add build.gradle
git add src/main/resources/application-prod.yml
git add src/main/java/com/jeju/ormicamp/common/config/SecurityConfig.java

# 2. 커밋
git commit -m "feat: Add ALB Ingress and Health Check for CloudFront integration"

# 3. develop 브랜치에 병합
git checkout develop
git merge [현재-브랜치]
git push origin develop

# 4. main에 병합 (CI/CD 자동 실행)
git checkout main
git merge develop
git push origin main
```

---

## 🛠️ Phase 2: AWS Load Balancer Controller 설치

EKS 클러스터에 AWS Load Balancer Controller를 설치해야 Ingress가 자동으로 ALB를 생성합니다.

### 2-1. IAM 정책 생성

```bash
# 1. IAM 정책 다운로드
curl -o iam-policy.json https://raw.githubusercontent.com/kubernetes-sigs/aws-load-balancer-controller/v2.7.0/docs/install/iam_policy.json

# 2. IAM 정책 생성
aws iam create-policy \
    --policy-name AWSLoadBalancerControllerIAMPolicy \
    --policy-document file://iam-policy.json \
    --region ap-northeast-2
```

### 2-2. Service Account 생성

```bash
# EKS 클러스터 이름 확인
export CLUSTER_NAME=your-eks-cluster-name
export AWS_ACCOUNT_ID=310688446727
export AWS_REGION=ap-northeast-2

# IAM Role과 Service Account 연결
eksctl create iamserviceaccount \
  --cluster=$CLUSTER_NAME \
  --namespace=kube-system \
  --name=aws-load-balancer-controller \
  --attach-policy-arn=arn:aws:iam::${AWS_ACCOUNT_ID}:policy/AWSLoadBalancerControllerIAMPolicy \
  --override-existing-serviceaccounts \
  --region $AWS_REGION \
  --approve
```

### 2-3. Load Balancer Controller 설치

```bash
# 1. Helm 추가
helm repo add eks https://aws.github.io/eks-charts
helm repo update

# 2. Controller 설치
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=$CLUSTER_NAME \
  --set serviceAccount.create=false \
  --set serviceAccount.name=aws-load-balancer-controller \
  --set region=$AWS_REGION \
  --set vpcId=$(aws eks describe-cluster --name $CLUSTER_NAME --query "cluster.resourcesVpcConfig.vpcId" --output text --region $AWS_REGION)

# 3. 설치 확인
kubectl get deployment -n kube-system aws-load-balancer-controller
```

### 2-4. Ingress 배포 및 ALB 확인

```bash
# 1. Ingress 적용
kubectl apply -f k8s-manifests/ingress.yml

# 2. ALB 생성 확인 (2-3분 소요)
kubectl get ingress backend-app-ingress

# 3. ALB DNS 이름 확인
kubectl get ingress backend-app-ingress -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'
# 출력 예: k8s-default-backenda-xxxxx-xxxxxxxxx.ap-northeast-2.elb.amazonaws.com

# 4. Health Check 테스트
curl http://[ALB-DNS]/actuator/health
```

---

## 🌐 Phase 3: CloudFront 배포

### 3-1. ACM(AWS Certificate Manager)에서 SSL 인증서 생성

**중요**: CloudFront용 인증서는 **반드시 us-east-1(버지니아)** 리전에 생성해야 합니다!

```bash
# us-east-1 리전에서 실행
aws acm request-certificate \
    --domain-name your-domain.com \
    --subject-alternative-names *.your-domain.com \
    --validation-method DNS \
    --region us-east-1

# 인증서 ARN 확인
aws acm list-certificates --region us-east-1
```

### 3-2. CloudFront Distribution 생성

#### AWS Console에서 생성:

1. **CloudFront Console** → "Create Distribution"

2. **Origin 설정**:
   ```
   Origin Domain: [ALB-DNS-Name]
   Protocol: HTTPS only
   Origin Protocol Policy: HTTPS only
   Origin SSL Protocols: TLSv1.2
   HTTP Port: 80
   HTTPS Port: 443
   Custom Headers: (나중에 보안 강화용 추가)
   ```

3. **Default Cache Behavior**:
   ```
   Viewer Protocol Policy: Redirect HTTP to HTTPS
   Allowed HTTP Methods: GET, HEAD, OPTIONS, PUT, POST, PATCH, DELETE
   Cache Policy: CachingOptimized (API는 CachingDisabled)
   Origin Request Policy: AllViewer
   Response Headers Policy: SimpleCORS
   
   Compress Objects Automatically: Yes
   ```

4. **Cache Key and Origin Requests**:
   ```
   Cache Policy: 
     - Static content (/images/*, /css/*, /js/*): CachingOptimized (TTL: 86400초)
     - API (/api/*): CachingDisabled (항상 origin 요청)
   
   Origin Request Policy: AllViewer
   ```

5. **Settings**:
   ```
   Price Class: Use All Edge Locations (전 세계)
   Alternate Domain Names (CNAMEs): your-domain.com, api.your-domain.com
   Custom SSL Certificate: [ACM 인증서 선택]
   Supported HTTP Versions: HTTP/2, HTTP/3
   Default Root Object: index.html (선택사항)
   Standard Logging: On (권장)
   IPv6: On
   ```

### 3-3. Behavior 설정 (Path Pattern별 캐싱)

CloudFront Distribution 생성 후 추가 Behavior 설정:

#### Behavior #1: API 요청 (캐싱 비활성화)
```
Path Pattern: /api/*
Cache Policy: CachingDisabled
Origin Request Policy: AllViewer
Response Headers Policy: SimpleCORS
Viewer Protocol Policy: Redirect HTTP to HTTPS
```

#### Behavior #2: Health Check
```
Path Pattern: /actuator/health*
Cache Policy: CachingDisabled
Origin Request Policy: AllViewer
Viewer Protocol Policy: HTTPS only
```

#### Behavior #3: Static Assets (최대 캐싱)
```
Path Pattern: /static/*
Cache Policy: CachingOptimized
Origin Request Policy: AllViewerExceptHostHeader
Compress Objects: Yes
TTL: 86400 (24시간)
```

### 3-4. CloudFront에서 ALB로의 보안 연결

**Origin Custom Header 추가** (ALB가 CloudFront만 허용):

CloudFront Origin 설정에 Custom Header 추가:
```
Header Name: X-Custom-Origin-Verify
Value: [랜덤-시크릿-값-생성]
```

그 후 ALB Target Group에서 Health Check에 이 헤더 검증 로직 추가:
```java
// Spring Boot에서 Filter로 검증
@Component
public class CloudFrontVerificationFilter extends OncePerRequestFilter {
    
    @Value("${cloudfront.origin.secret}")
    private String originSecret;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) {
        String originHeader = request.getHeader("X-Custom-Origin-Verify");
        
        if (originHeader == null || !originHeader.equals(originSecret)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        
        filterChain.doFilter(request, response);
    }
}
```

---

## 🔒 Phase 4: 보안 강화

### 4-1. WAF (Web Application Firewall) 설정

```bash
# CloudFront에 WAF 연결
# AWS Console → WAF & Shield → Web ACLs → Create
```

**추천 Rule Sets**:
- AWS Managed Rules - Core rule set
- AWS Managed Rules - Known bad inputs
- AWS Managed Rules - SQL injection
- Rate-based rule (요청 제한: 2000 req/5min per IP)

### 4-2. CloudFront Origin Shield 활성화

```
Origin Shield Region: ap-northeast-2 (Seoul)
```

**이점**:
- Origin 부하 감소 (캐시 히트율 증가)
- ALB로의 요청 수 감소 → 비용 절감

### 4-3. 보안 헤더 추가

CloudFront Response Headers Policy:
```
Strict-Transport-Security: max-age=63072000; includeSubdomains; preload
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Referrer-Policy: strict-origin-when-cross-origin
```

---

## 📊 Phase 5: 성능 최적화

### 5-1. 캐싱 전략

#### API 엔드포인트별 캐싱:

| 엔드포인트 | 캐싱 정책 | TTL | 이유 |
|---------|---------|-----|------|
| `/api/planner` (POST) | No Cache | 0 | 동적 생성, 사용자별 다름 |
| `/api/places` (GET) | Cache | 3600s | 정적 데이터, 자주 변경 안 됨 |
| `/api/restaurants` (GET) | Cache | 1800s | 준정적 데이터 |
| `/static/*` | Cache | 86400s | 이미지, CSS, JS |
| `/actuator/health` | No Cache | 0 | Health check |

### 5-2. Compression 활성화

CloudFront에서 자동 압축:
```
Brotli: Yes
Gzip: Yes
```

Spring Boot에서도 압축 활성화:
```yaml
# application-prod.yml
server:
  compression:
    enabled: true
    mime-types: text/html,text/xml,text/plain,text/css,application/json,application/javascript
    min-response-size: 1024
```

### 5-3. Connection Keep-Alive

```yaml
# application-prod.yml
server:
  tomcat:
    connection-timeout: 60000
    keep-alive-timeout: 60000
    max-keep-alive-requests: 100
```

---

## 🌍 Phase 6: 미국 AI Agent 연동 최적화

### 6-1. AI Agent 호출 아키텍처

```java
// Seoul Backend에서 US AI Agent 호출 최적화
@Service
public class AIPlannerService {
    
    private final WebClient usAiAgentClient;
    
    @Value("${ai.agent.us.endpoint}")
    private String usAiEndpoint; // https://ai-agent.us-east-1.amazonaws.com
    
    public AIPlannerService(WebClient.Builder builder) {
        this.usAiAgentClient = builder
            .baseUrl(usAiEndpoint)
            .defaultHeader("Content-Type", "application/json")
            // Connection Pooling 최적화
            .clientConnector(new ReactorClientHttpConnector(
                HttpClient.create()
                    .responseTimeout(Duration.ofSeconds(30))
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                    .option(ChannelOption.SO_KEEPALIVE, true)
            ))
            .build();
    }
    
    @Async
    public CompletableFuture<PlannerResponse> generatePlannerAsync(PlannerRequest request) {
        // 비동기 호출로 Seoul 백엔드가 블로킹되지 않음
        return usAiAgentClient
            .post()
            .uri("/generate-planner")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(PlannerResponse.class)
            .timeout(Duration.ofSeconds(30))
            .toFuture();
    }
}
```

### 6-2. Timeout 및 Retry 전략

```yaml
# application-prod.yml
ai:
  agent:
    us:
      endpoint: https://ai-agent.us-east-1.amazonaws.com
      timeout: 30000  # 30초
      retry:
        max-attempts: 3
        backoff: 2000  # 2초
```

### 6-3. 예상 레이턴시

```
Seoul → US AI Agent:
  - Network Latency: ~150-200ms (Seoul → Virginia)
  - AI Processing: ~2-5초
  - Total: ~2.5-5.5초

사용자 → Seoul (CloudFront 경유):
  - 한국 사용자: ~50ms
  - 일본 사용자: ~80ms
  - 중국 사용자: ~120ms
  - 미국 사용자: ~180ms

전체 플래너 생성 시간:
  - 동아시아 사용자: ~2.6-5.7초
  - 미국 사용자: ~2.9-6초
```

---

## 💰 Phase 7: 비용 최적화

### 7-1. 예상 비용 (월간, 트래픽 가정: 100만 요청/월)

| 서비스 | 비용 | 설명 |
|--------|------|------|
| CloudFront | $85-120 | Data Transfer Out + Requests |
| ALB | $25-30 | LCU 기반 |
| EKS (Worker Nodes) | $150-200 | t3.medium x 3 |
| DynamoDB | $5-20 | On-demand 또는 Provisioned |
| US AI Agent | $200-500 | 사용량에 따라 다름 |
| **Total** | **$465-870/월** | |

### 7-2. 비용 절감 전략

1. **CloudFront Cache Hit Rate 극대화**:
   - 정적 콘텐츠 캐싱 → Origin 요청 감소
   - 목표: 80%+ Cache Hit Rate

2. **Reserved Capacity (장기 사용 시)**:
   - EKS Worker Nodes: Savings Plans 적용 (최대 72% 절감)
   - DynamoDB: Reserved Capacity (최대 53% 절감)

3. **Auto Scaling**:
   ```yaml
   # EKS HPA (Horizontal Pod Autoscaler)
   apiVersion: autoscaling/v2
   kind: HorizontalPodAutoscaler
   metadata:
     name: backend-app-hpa
   spec:
     scaleTargetRef:
       apiVersion: apps/v1
       kind: Deployment
       name: backend-app-deployment
     minReplicas: 2
     maxReplicas: 10
     metrics:
     - type: Resource
       resource:
         name: cpu
         target:
           type: Utilization
           averageUtilization: 70
   ```

---

## 📈 Phase 8: 모니터링 및 알람

### 8-1. CloudWatch 대시보드 설정

**주요 메트릭**:
```
CloudFront:
  - Requests
  - BytesDownloaded
  - 4xx/5xx Error Rate
  - Cache Hit Rate

ALB:
  - TargetResponseTime
  - RequestCount
  - HTTPCode_Target_4XX_Count
  - HTTPCode_Target_5XX_Count
  - HealthyHostCount

EKS:
  - CPU Utilization
  - Memory Utilization
  - Pod Restart Count

Custom (Application):
  - AI Agent Call Latency
  - AI Agent Success Rate
  - API Response Time (per endpoint)
```

### 8-2. CloudWatch Alarms

```bash
# 1. ALB 5xx 에러율
aws cloudwatch put-metric-alarm \
    --alarm-name backend-alb-5xx-errors \
    --metric-name HTTPCode_Target_5XX_Count \
    --namespace AWS/ApplicationELB \
    --statistic Sum \
    --period 300 \
    --evaluation-periods 2 \
    --threshold 10 \
    --comparison-operator GreaterThanThreshold \
    --alarm-actions arn:aws:sns:ap-northeast-2:310688446727:backend-alerts

# 2. CloudFront Error Rate
aws cloudwatch put-metric-alarm \
    --alarm-name cloudfront-error-rate \
    --metric-name 5xxErrorRate \
    --namespace AWS/CloudFront \
    --statistic Average \
    --period 300 \
    --evaluation-periods 2 \
    --threshold 5 \
    --comparison-operator GreaterThanThreshold
```

### 8-3. Application Performance Monitoring (APM)

Spring Boot Actuator + Prometheus + Grafana:

```yaml
# prometheus-servicemonitor.yml
apiVersion: v1
kind: ServiceMonitor
metadata:
  name: backend-app-metrics
spec:
  selector:
    matchLabels:
      app: backend-app
  endpoints:
  - port: http
    path: /actuator/prometheus
    interval: 30s
```

---

## 🧪 Phase 9: 테스트

### 9-1. 로컬 테스트

```bash
# 1. Health Check
curl http://localhost:8001/actuator/health

# 2. Liveness Probe
curl http://localhost:8001/actuator/health/liveness

# 3. Readiness Probe
curl http://localhost:8001/actuator/health/readiness
```

### 9-2. ALB 테스트

```bash
# ALB DNS 확인
ALB_DNS=$(kubectl get ingress backend-app-ingress -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')

# Health Check
curl http://$ALB_DNS/actuator/health

# API 테스트
curl http://$ALB_DNS/api/test
```

### 9-3. CloudFront 테스트

```bash
# CloudFront 배포 완료 후
CLOUDFRONT_DOMAIN="your-distribution.cloudfront.net"

# Cache 테스트
curl -I https://$CLOUDFRONT_DOMAIN/api/places
# X-Cache: Hit from cloudfront (캐시 히트)
# X-Cache: Miss from cloudfront (캐시 미스)

# 성능 테스트 (여러 리전에서)
# 한국, 일본, 미국 등 다양한 위치에서 latency 측정
```

### 9-4. 부하 테스트

```bash
# Apache Benchmark
ab -n 1000 -c 100 https://$CLOUDFRONT_DOMAIN/api/test

# 또는 k6 (권장)
k6 run load-test.js
```

---

## 🚦 Phase 10: Route53 설정 (커스텀 도메인)

### 10-1. Hosted Zone 생성

```bash
aws route53 create-hosted-zone \
    --name your-domain.com \
    --caller-reference $(date +%s)
```

### 10-2. A Record 생성 (CloudFront Alias)

```bash
# CloudFront Distribution ID 확인
DISTRIBUTION_ID=$(aws cloudfront list-distributions \
    --query "DistributionList.Items[?Comment=='Jeju Planner Backend'].Id" \
    --output text)

# A Record 생성
aws route53 change-resource-record-sets \
    --hosted-zone-id Z1234567890ABC \
    --change-batch '{
      "Changes": [{
        "Action": "CREATE",
        "ResourceRecordSet": {
          "Name": "api.your-domain.com",
          "Type": "A",
          "AliasTarget": {
            "HostedZoneId": "Z2FDTNDATAQYW2",
            "DNSName": "'$CLOUDFRONT_DOMAIN'",
            "EvaluateTargetHealth": false
          }
        }
      }]
    }'
```

---

## ✅ 배포 체크리스트

### 사전 준비:
- [ ] EKS 클러스터 정상 작동 확인
- [ ] AWS Load Balancer Controller 설치
- [ ] ACM 인증서 생성 (us-east-1)
- [ ] 도메인 준비 (선택사항)

### Phase 1 - ALB/Ingress:
- [ ] Actuator 의존성 추가
- [ ] application-prod.yml 설정
- [ ] SecurityConfig에서 `/actuator/health` 허용
- [ ] Ingress.yml 생성
- [ ] Service를 ClusterIP로 변경
- [ ] GitHub에 커밋 및 main 병합
- [ ] ArgoCD 자동 배포 확인
- [ ] ALB 생성 확인
- [ ] Health Check 정상 작동 확인

### Phase 2 - CloudFront:
- [ ] CloudFront Distribution 생성
- [ ] Origin: ALB DNS 설정
- [ ] Cache Behavior 설정
- [ ] SSL 인증서 연결
- [ ] Custom Header 보안 설정
- [ ] WAF 연결
- [ ] Distribution 배포 완료 (15-20분)

### Phase 3 - 테스트:
- [ ] Health Check 테스트
- [ ] API 엔드포인트 테스트
- [ ] 캐시 동작 확인
- [ ] 여러 리전에서 latency 측정
- [ ] 부하 테스트

### Phase 4 - 모니터링:
- [ ] CloudWatch 대시보드 생성
- [ ] Alarms 설정
- [ ] SNS 알림 설정

---

## 🎯 예상 성능 개선

### CloudFront 적용 전:
```
한국 사용자 → Seoul Backend: ~10-20ms
일본 사용자 → Seoul Backend: ~40-60ms
중국 사용자 → Seoul Backend: ~80-150ms
미국 사용자 → Seoul Backend: ~180-250ms
```

### CloudFront 적용 후:
```
한국 사용자 → CloudFront Edge: ~5-10ms
일본 사용자 → CloudFront Edge: ~8-15ms
중국 사용자 → CloudFront Edge: ~20-40ms
미국 사용자 → CloudFront Edge: ~15-30ms

(캐시 미스 시 Origin까지의 시간 추가)
```

**개선율**: 50-80% latency 감소

---

## 🔄 CI/CD 파이프라인 업데이트 필요 사항

현재 CI/CD 파이프라인은 **변경 불필요**합니다:
- ✅ buildspec.yml: 그대로 유지 (ap-northeast-2)
- ✅ GitHub Actions: 그대로 유지
- ✅ ArgoCD: 그대로 작동

새로 추가된 파일만 Git에 추가하면 됩니다:
- `k8s-manifests/ingress.yml`
- `src/main/resources/application-prod.yml` (수정)
- `build.gradle` (Actuator 추가)
- `SecurityConfig.java` (Health Check 허용)

---

## 📞 문제 해결 (Troubleshooting)

### 문제 1: ALB가 생성되지 않음
```bash
# Controller 로그 확인
kubectl logs -n kube-system deployment/aws-load-balancer-controller

# Ingress 이벤트 확인
kubectl describe ingress backend-app-ingress
```

### 문제 2: Health Check 실패
```bash
# Pod 직접 접근
kubectl port-forward pod/[pod-name] 8001:8001
curl http://localhost:8001/actuator/health

# Pod 로그 확인
kubectl logs [pod-name]
```

### 문제 3: CloudFront 502 Bad Gateway
```bash
# ALB Target 상태 확인
aws elbv2 describe-target-health \
    --target-group-arn [target-group-arn] \
    --region ap-northeast-2

# CloudFront Distribution 상태
aws cloudfront get-distribution --id [distribution-id]
```

---

## 📚 참고 자료

- [AWS Load Balancer Controller 문서](https://kubernetes-sigs.github.io/aws-load-balancer-controller/)
- [CloudFront 개발자 가이드](https://docs.aws.amazon.com/cloudfront/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)

---

## 🎉 완료!

이제 전 세계 사용자가 가장 가까운 CloudFront Edge를 통해 빠르게 서비스에 접근할 수 있습니다!

**다음 작업**:
1. 변경사항을 develop → main 병합
2. AWS Load Balancer Controller 설치
3. CloudFront Distribution 생성
4. 테스트 및 모니터링 설정

