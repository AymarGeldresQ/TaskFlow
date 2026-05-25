# TaskFlow API — cURL Cheat Sheet

Base URL: `http://localhost:8080`

Set these variables before running commands:
```bash
BASE=http://localhost:8080
TOKEN=""          # fill after login/register
TEAM_ID=""
PROJECT_ID=""
TASK_ID=""
USER_ID=""
COMMENT_ID=""
LABEL_ID=""
```

---

## Auth

### Register
```bash
curl -s -X POST $BASE/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"SecurePass123!","fullName":"Alice Smith"}'
```

### Login
```bash
curl -s -X POST $BASE/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"SecurePass123!"}'
```

### Refresh token
```bash
curl -s -X POST $BASE/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}"
```

### Logout (revokes all tokens)
```bash
curl -s -X POST $BASE/api/v1/auth/logout \
  -H "Authorization: Bearer $TOKEN"
```

---

## Teams

### Create team
```bash
curl -s -X POST $BASE/api/v1/teams \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"Engineering","slug":"engineering"}'
```

### List team members
```bash
curl -s $BASE/api/v1/teams/$TEAM_ID/members \
  -H "Authorization: Bearer $TOKEN"
```

### Add member
```bash
curl -s -X POST $BASE/api/v1/teams/$TEAM_ID/members \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"userId\":\"$USER_ID\",\"role\":\"MEMBER\"}"
# role: OWNER | MEMBER | VIEWER
```

### Remove member
```bash
curl -s -X DELETE $BASE/api/v1/teams/$TEAM_ID/members/$USER_ID \
  -H "Authorization: Bearer $TOKEN"
```

---

## Projects

### Create project
```bash
curl -s -X POST $BASE/api/v1/teams/$TEAM_ID/projects \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"Backend Sprint 1","description":"First sprint"}'
```

### List projects for team
```bash
curl -s "$BASE/api/v1/teams/$TEAM_ID/projects?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

### Archive project
```bash
curl -s -X PATCH $BASE/api/v1/projects/$PROJECT_ID/archive \
  -H "Authorization: Bearer $TOKEN"
```

### Create label
```bash
curl -s -X POST $BASE/api/v1/projects/$PROJECT_ID/labels \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"bug","color":"#e11d48"}'
```

### List labels
```bash
curl -s $BASE/api/v1/projects/$PROJECT_ID/labels \
  -H "Authorization: Bearer $TOKEN"
```

---

## Tasks

### Create task
```bash
curl -s -X POST $BASE/api/v1/projects/$PROJECT_ID/tasks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"title":"Fix login bug","description":"Token expiry off by one","priority":"HIGH"}'
# priority: LOW | MEDIUM | HIGH | CRITICAL  (default: MEDIUM)
```

### List tasks (with optional status filter)
```bash
curl -s "$BASE/api/v1/projects/$PROJECT_ID/tasks?status=TODO&page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

### Update task
```bash
curl -s -X PUT $BASE/api/v1/tasks/$TASK_ID \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"title":"Updated title","description":"Updated desc","priority":"MEDIUM"}'
```

### Transition status
```bash
curl -s -X PATCH $BASE/api/v1/tasks/$TASK_ID/status \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"status":"IN_PROGRESS"}'
# Valid flow: BACKLOG → TODO → IN_PROGRESS → IN_REVIEW → DONE
# Backwards:  DONE → IN_REVIEW → IN_PROGRESS → TODO
```

### Assign task
```bash
curl -s -X PATCH $BASE/api/v1/tasks/$TASK_ID/assignee \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"userId\":\"$USER_ID\"}"
# Unassign: send {"userId": null}
```

### Add label to task
```bash
curl -s -X POST $BASE/api/v1/tasks/$TASK_ID/labels/$LABEL_ID \
  -H "Authorization: Bearer $TOKEN"
```

### Remove label from task
```bash
curl -s -X DELETE $BASE/api/v1/tasks/$TASK_ID/labels/$LABEL_ID \
  -H "Authorization: Bearer $TOKEN"
```

### Soft-delete task
```bash
curl -s -X DELETE $BASE/api/v1/tasks/$TASK_ID \
  -H "Authorization: Bearer $TOKEN"
```

---

## Comments

### Add comment
```bash
curl -s -X POST $BASE/api/v1/tasks/$TASK_ID/comments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"content":"Looks good to me, merging."}'
```

### List comments
```bash
curl -s "$BASE/api/v1/tasks/$TASK_ID/comments?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

### Edit comment
```bash
curl -s -X PATCH $BASE/api/v1/comments/$COMMENT_ID \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"content":"Updated comment text."}'
```

### Delete comment
```bash
curl -s -X DELETE $BASE/api/v1/comments/$COMMENT_ID \
  -H "Authorization: Bearer $TOKEN"
```

---

## Health & Docs

```bash
curl -s $BASE/actuator/health
curl -s $BASE/v3/api-docs        # OpenAPI JSON
open $BASE/swagger-ui/index.html  # Swagger UI (browser)
```
