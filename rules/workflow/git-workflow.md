---
inclusion: always
---

# Git Workflow

## 브랜치 전략

```
main          # 항상 배포 가능한 안정 브랜치
feature/*     # 새 기능 개발 (예: feature/add-myrpg-battle-system)
fix/*         # 버그 수정 (예: fix/mycalendar-date-error)
hotfix/*      # 긴급 프로덕션 수정
chore/*       # 빌드, 설정, 의존성 업데이트 등
```

- `main` 브랜치에 직접 push 금지
- 코드 변경 작업 시작 전, 브랜치를 새로 생성할지 사용자에게 먼저 물어봅니다
  - 사용자가 브랜치 생성을 원하면: 적절한 브랜치명을 제안하고 생성 후 작업 진행
  - 사용자가 현재 브랜치에서 작업을 원하면: 브랜치 생성 없이 진행
- 브랜치를 생성한 경우, 변경은 PR을 통해 병합

## 작업 브랜치 워크플로우

코드 변경 작업 시 아래 순서를 따릅니다:

1. **브랜치 생성**: `main`에서 새 브랜치 생성
   ```bash
   git checkout main
   git pull origin main
   git checkout -b {type}/{branch-name}
   ```

2. **커밋**: 작업 완료 후 커밋
   ```bash
   git add <files>
   git commit -m "<type>(<scope>): <한글 설명>"
   ```

3. **main rebase**: push 전에 최신 main을 rebase
   ```bash
   git fetch origin
   git rebase origin/main
   ```

4. **push**: 리모트에 브랜치 push
   ```bash
   git push -u origin {type}/{branch-name}
   ```

5. **PR 생성**: GitHub에서 PR 생성 후 리뷰 → Squash and Merge

## 커밋 메시지 컨벤션

[Conventional Commits](https://www.conventionalcommits.org/) 형식 사용, **메시지는 한글로 작성**:

```
<type>(<scope>): <한글 설명>

[optional body - 한글]

[optional footer]
```

**type 목록:**
- `feat`: 새 기능
- `fix`: 버그 수정
- `docs`: 문서 변경
- `style`: 코드 포맷, 세미콜론 등 (기능 변경 없음)
- `refactor`: 리팩토링
- `test`: 테스트 추가/수정
- `chore`: 빌드, 설정, 의존성 관련
- `ci`: CI/CD 설정 변경

**scope 예시:** `myrpg`, `mycalendar`, `mystudy`, `root-pom`, `deps`, `ci`

**예시:**
```
feat(myrpg): 전투 보상 시스템 추가
fix(mycalendar): 날짜 경계값 처리 오류 수정
chore(deps): spring-boot 4.0.1로 버전 업그레이드
chore: 프로젝트 초기 설정 및 스펙 문서 추가
```

## PR 규칙

- PR 제목은 커밋 메시지 컨벤션과 동일하게 작성
- PR 설명에 변경 내용 요약, 테스트 방법 포함
- 최소 1명 리뷰 승인 후 병합
- merge 방식: Squash and Merge 권장 (히스토리 정리)

## GitHub 레포지토리 설정

- 레포지토리명: `myapps`
- Visibility: Private (기본)
- Default branch: `main`
- Branch protection: `main` 브랜치에 직접 push 금지 설정 권장
