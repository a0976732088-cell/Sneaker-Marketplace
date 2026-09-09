# SneakerMarket Git 協作規範

## 1. Branch

```text
main
└── develop
    ├── feature/member
    ├── feature/commerce
    ├── feature/advertisement
    ├── feature/calendar-notification
    └── feature/auction
```

### main

只保留已整合、可展示的穩定版本。

### develop

五個模組進行整合測試的共同分支。

### feature/\*

各模組正式開發分支。開發前從最新 `develop` 建立或同步。

## 2. 開發流程

1. `fetch` / `pull` 最新 `develop`。
2. 切換到自己的 `feature/*`。
3. 開發並於本機測試。
4. Commit。
5. Push 自己的 feature branch。
6. 建立 Pull Request：`feature/*` → `develop`。
7. 整合測試通過後再合併。
8. 階段版本穩定後：`develop` → `main`。

## 3. Commit Message

建議格式：

```text
<type>(<scope>): <description>
```

常用 type：

- `feat`：新增功能
- `fix`：修正錯誤
- `refactor`：重構但不改功能
- `docs`：文件
- `style`：CSS / 格式調整
- `test`：測試
- `chore`：套件、設定、工具

例如：

```text
feat(auction): add auction detail API
fix(cart): prevent quantity exceeding stock
docs: update API contract
chore(frontend): update dependencies
```

## 4. 共用檔案

下列檔案較容易造成多人衝突，修改前應先通知：

### Frontend

- `package.json`
- `package-lock.json`
- `vite.config.js`
- `src/router/index.js`
- 共用 Layout / Navbar / 全域 CSS

### Backend

- `pom.xml`
- `application*.properties`
- Security / CORS 共用設定
- `common/**`

### Database / Docs

- 共用資料表 Schema
- API Contract
- 資料表責任表

## 5. Database Change

資料表正式進入多人開發後：

- 不任意刪除、改名共用欄位。
- local 開發環境以 Hibernate Entity Mapping 與 ddl-auto=update 維護 Schema。
- 新增或修改資料表結構時，必須同步 Java Entity 與資料庫文件。
- 高風險變更（刪除／改名／型別／PK／FK）先由團隊確認。
- 未來若改採 migration/validate 管理，再統一使用 migration SQL。
