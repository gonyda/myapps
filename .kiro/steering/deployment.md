# Deployment

## 배포 대상 서버

- **호스트**: 134.185.116.35 (MCP: oracle-cloud-ssh)
- **사용자**: ubuntu
- **아키텍처**: aarch64 (ARM)
- **OS**: Ubuntu 20.04 LTS
- **소스 경로**: /home/ubuntu/app/myapps (git clone)
- **실행 방식**: nohup (수동 실행/종료)

## 서버 초기 환경 설정 (최초 1회)

서버에 Java 25, Maven 3.9.9이 설치되어 있지 않으면 아래 순서로 설치합니다.

### Java 25 설치 (aarch64) — ✅ 설치 완료

- 설치 경로: `/opt/jdk-25.0.3`
- JAVA_HOME: `/opt/jdk-25.0.3`
- 버전: Oracle JDK 25.0.3+9-LTS-195

### Maven 3.9.9 설치 — ✅ 설치 완료

- 설치 경로: `/opt/apache-maven-3.9.9`
- 심볼릭 링크: `/usr/local/bin/mvn`

### DB_PASSWORD 환경변수 설정 — ✅ 설정 완료

`/etc/environment`에 설정됨.

### GitHub 소스 클론 — ✅ 완료

- 경로: `/home/ubuntu/app/myapps`
- credential 저장 설정됨 (PAT 인증, git pull 시 추가 인증 불필요)

## 배포 절차

### 1. 소스 업데이트

```bash
cd /home/ubuntu/app/myapps
git pull origin main
```

### 2. 빌드

```bash
cd /home/ubuntu/app/myapps
export JAVA_HOME=/opt/jdk-25.0.3
export PATH=$JAVA_HOME/bin:$PATH
mvn clean package -pl {modulename} -am -DskipTests
```

### 3. 기존 프로세스 종료

```bash
pkill -f "{modulename}.*\.jar" || true
```

### 4. 실행

```bash
cd /home/ubuntu/app/myapps/{modulename}
export JAVA_HOME=/opt/jdk-25.0.3
export PATH=$JAVA_HOME/bin:$PATH
export DB_PASSWORD="babjo0-mucguj-Mosjeb"
nohup java -jar target/{modulename}-1.0.0-SNAPSHOT.jar --spring.profiles.active=prod > /home/ubuntu/app/myapps/{modulename}/app.log 2>&1 &
echo $! > /home/ubuntu/app/myapps/{modulename}/app.pid
```

> **주의**: `source /etc/environment` 대신 `export DB_PASSWORD=...`를 같은 셸에서 직접 선언해야 합니다. nohup 서브셸에서는 `/etc/environment`를 source해도 환경변수가 제대로 전달되지 않을 수 있습니다.

### 5. 실행 확인

```bash
sleep 5
curl -s http://localhost:8080/actuator/health || tail -20 /home/ubuntu/app/myapps/{modulename}/app.log
```

## 포트 규칙

- **사용 가능 범위**: 8080–8099 (Oracle Cloud Security List + iptables 사전 개방 완료)
- 새 모듈 추가 시 아래 표에서 비어 있는 다음 포트를 할당하고, `application-prod.yml`에 `server.port`를 명시합니다.

| 모듈 | 포트 |
|------|------|
| mystudy | 8080 |

> 8080–8099 범위 내라면 Oracle Cloud Security List와 iptables 추가 설정 없이 바로 외부 접속 가능합니다.

## 로그 확인

```bash
tail -f /home/ubuntu/app/myapps/{modulename}/app.log
```

## 방화벽 설정 — ✅ 설정 완료

### Oracle Cloud Security List

| Source | Protocol | Destination Port Range | 비고 |
|--------|----------|------------------------|------|
| 0.0.0.0/0 | TCP | 22 | SSH |
| 0.0.0.0/0 | TCP | 8080-8099 | 앱 포트 범위 |

### iptables

8080–8099 범위가 REJECT 규칙 앞에 허용 규칙으로 등록 및 영구 저장됨.

```
5    ACCEPT     tcp  --  0.0.0.0/0            0.0.0.0/0            tcp dpts:8080:8099
```

> 8080–8099 범위 밖의 포트가 필요한 경우에만 Security List + iptables를 추가 설정합니다.
> iptables 추가 시 반드시 REJECT 규칙 앞에 삽입 (`-I INPUT {N}`)해야 합니다.

## 주의사항

- `{modulename}`은 실제 모듈명으로 대체 (예: mystudy)
- 빌드 시 `-DskipTests`로 테스트 생략 (서버에서는 테스트 불필요)
- 프로파일은 항상 `prod` 사용
- 서버 재부팅 시 수동으로 다시 실행 필요
