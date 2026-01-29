# Docker exec format error 해결 가이드

## 문제 상황
```
exec /usr/bin/java: exec format error
```

## 원인 분석
1. **아키텍처 호환성 문제**: Docker 이미지가 EC2 인스턴스의 아키텍처와 맞지 않음
2. **불완전한 Dockerfile**: ENTRYPOINT/CMD 누락
3. **잘못된 베이스 이미지**: ARM64에서 빌드된 이미지를 x86_64에서 실행

## 해결 방법

### 1. 즉시 해결 (EC2에서 실행)
```bash
# 현재 실행 중인 컨테이너 중지
docker stop spring-backend
docker rm spring-backend

# AMD64 전용 이미지로 다시 빌드
docker build --platform linux/amd64 -t spring-backend-local .

# 로컬 이미지로 컨테이너 실행
docker run -d --name spring-backend \
  --network ipoten-net \
  -p 8080:8080 \
  spring-backend-local
```

### 2. 장기 해결책
```bash
# 멀티 아키텍처 이미지 빌드 및 푸시
./build-and-deploy.sh
```

### 3. Docker Compose 수정
docker-compose.yml에서 이미지를 로컬 빌드로 변경:
```yaml
server:
  build: .  # 또는 image: spring-backend-local
  # image: ghcr.io/imcoder0000/spring-backend:latest  # 주석 처리
```

## 확인 방법
```bash
# 아키텍처 확인
docker image inspect spring-backend-local | grep Architecture

# 컨테이너 로그 확인
docker logs spring-backend
```
