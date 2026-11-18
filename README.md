# 게시판 REST API (Spring Boot, TDD)

게시글·댓글 CRUD REST API입니다. 10개 엔드포인트를 MockMvc 테스트로 먼저 작성한 뒤 구현했습니다(TDD).

## 기술 스택

| 항목 | 버전 |
| --- | --- |
| Java | 25 |
| Spring Boot | 4.1.2 (Web MVC, Data JPA, Validation) |
| Gradle Wrapper | 9.9.1 |
| DB | H2 (개발: 파일 DB `./db_dev`, 테스트: 메모리 DB) |

## 실행 방법

```bash
# macOS / Linux
./gradlew bootRun

# Windows
.\gradlew.bat bootRun
```

- 기본 주소: `http://localhost:8080`
- H2 콘솔: `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:./db_dev;MODE=MySQL`, 사용자 `sa`, 비밀번호 없음)
- 빌드한 jar로 실행: `java -jar build/libs/back-0.0.1-SNAPSHOT.jar`

### 샘플 데이터

개발 DB가 비어 있을 때 처음 실행하면 아래 데이터를 자동 생성합니다(`BaseInitData`).
게시글이 이미 있으면 생성하지 않으므로 같은 DB로 다시 시작해도 중복되지 않습니다.
처음 상태로 되돌리려면 앱을 끄고 `db_dev.mv.db` 파일을 삭제하세요.

| 게시글 | 댓글 수 |
| --- | --- |
| 1번 `제목 1` | 3건 (`댓글 1-1` ~ `댓글 1-3`) |
| 2번 `제목 2` | 2건 (`댓글 2-1`, `댓글 2-2`) |
| 3번 `제목 3` | 0건 |

## 테스트 실행 방법

```bash
# 전체 테스트
./gradlew test            # Windows: .\gradlew.bat test

# 전체 테스트 + 빌드
./gradlew clean build     # Windows: .\gradlew.bat clean build
```

- 테스트는 `test` 프로파일(`src/test/resources/application-test.yml`)로 H2 메모리 DB를 사용하며 개발 DB와 분리됩니다.
- 샘플 데이터는 테스트에서 생성되지 않습니다. 각 테스트가 필요한 데이터를 직접 만들고, `@Transactional`로 테스트마다 롤백되므로 실행 순서와 무관합니다.
- 테스트 클래스: `ApiV1PostControllerTest`, `ApiV1PostCommentControllerTest`, `BaseInitDataTest`

## API

### 게시글

| 메서드 | 경로 | 설명 | 요청 본문 | 성공 응답 |
| --- | --- | --- | --- | --- |
| POST | `/api/v1/posts` | 작성 | `{"title", "content"}` | 201, `RsData<PostDto>` (`201-1`) |
| GET | `/api/v1/posts` | 다건 조회(최신순) | - | 200, `PostDto[]` |
| GET | `/api/v1/posts/{id}` | 단건 조회 | - | 200, `PostDto` |
| PUT | `/api/v1/posts/{id}` | 수정 | `{"title", "content"}` | 200, `RsData<Void>` (`200-1`) |
| DELETE | `/api/v1/posts/{id}` | 삭제(소속 댓글도 삭제) | - | 200, `RsData<Void>` (`200-1`) |

### 댓글

| 메서드 | 경로 | 설명 | 요청 본문 | 성공 응답 |
| --- | --- | --- | --- | --- |
| POST | `/api/v1/posts/{postId}/comments` | 작성 | `{"content"}` | 201, `RsData<PostCommentDto>` (`201-1`) |
| GET | `/api/v1/posts/{postId}/comments` | 해당 글의 댓글 다건 조회(최신순) | - | 200, `PostCommentDto[]` |
| GET | `/api/v1/posts/{postId}/comments/{id}` | 해당 글의 댓글 단건 조회 | - | 200, `PostCommentDto` |
| PUT | `/api/v1/posts/{postId}/comments/{id}` | 수정 | `{"content"}` | 200, `RsData<Void>` (`200-1`) |
| DELETE | `/api/v1/posts/{postId}/comments/{id}` | 삭제 | - | 200, `RsData<Void>` (`200-1`) |

### 응답 형식

조회 성공은 DTO(또는 DTO 배열)를 그대로 반환합니다.

```json
{
  "id": 1,
  "createDate": "2026-11-18T14:28:08.011892",
  "modifyDate": "2026-11-18T14:28:08.011892",
  "title": "제목 1",
  "content": "내용 1"
}
```

`PostCommentDto`는 `id`, `createDate`, `modifyDate`, `postId`, `content` 필드를 가집니다.

작성·수정·삭제와 모든 실패 응답은 `RsData`입니다. `resultCode`의 앞 세 자리가 HTTP 상태 코드입니다.

```json
{
  "resultCode": "201-1",
  "msg": "4번 글이 작성되었습니다.",
  "data": { "id": 4, "createDate": "...", "modifyDate": "...", "title": "제목", "content": "내용" }
}
```

| 상황 | HTTP | resultCode |
| --- | --- | --- |
| 작성 성공 | 201 | `201-1` |
| 수정·삭제 성공 | 200 | `200-1` |
| 필수 입력 누락(빈 값), 잘못된 JSON | 400 | `400-1` |
| 없는 게시글·댓글, 다른 게시글에 속한 댓글 ID | 404 | `404-1` |

입력 검증: `title`·`content`(게시글)와 `content`(댓글)는 공백일 수 없고 2자 이상이어야 합니다.

### 사용 예 (curl)

```bash
curl -X POST localhost:8080/api/v1/posts -H "Content-Type: application/json" -d '{"title":"제목","content":"내용"}'
curl localhost:8080/api/v1/posts
curl localhost:8080/api/v1/posts/1
curl -X PUT localhost:8080/api/v1/posts/1 -H "Content-Type: application/json" -d '{"title":"수정 제목","content":"수정 내용"}'
curl -X DELETE localhost:8080/api/v1/posts/1

curl -X POST localhost:8080/api/v1/posts/2/comments -H "Content-Type: application/json" -d '{"content":"새 댓글"}'
curl localhost:8080/api/v1/posts/2/comments
curl localhost:8080/api/v1/posts/2/comments/4
curl -X PUT localhost:8080/api/v1/posts/2/comments/4 -H "Content-Type: application/json" -d '{"content":"수정 댓글"}'
curl -X DELETE localhost:8080/api/v1/posts/2/comments/4
```

## 커밋과 태그

각 기능은 테스트 작성 → 실패 확인 → 구현 → 통과 순서로 진행했고, 기능과 테스트를 한 커밋으로 묶어 번호 태그(`001`~`013`)를 붙였습니다.

```bash
git log --oneline --decorate
```
