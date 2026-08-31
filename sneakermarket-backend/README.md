# SneakerMarket Backend BASE

專案名稱：`sneakermarket-backend`

球鞋電子商城＋競標平台的 Spring Boot 後端共用 BASE 專案。

## 1. BASE 目前包含

- Java 17
- Spring Boot 4.0.8
- Maven Wrapper
- Spring Web MVC / REST API
- Spring Data JPA
- Microsoft SQL Server JDBC
- Spring Validation
- Spring Security（目前 BASE 暫時 permitAll，正式 JWT 由會員模組後續實作）
- Spring Mail（預留競標結束 Email）
- Spring WebSocket（預留競標即時更新延伸功能）
- Lombok
- DevTools
- Scheduler（預留競標自動結標）

## 2. Package

```text
com.project.sneakermarket
├── common
├── member
├── commerce
├── advertisement
├── calendar
├── notification
├── auction
│   ├── scheduler
│   └── email
└── admin
```

各功能模組內預留 `controller`、`service`、`repository`、`entity`、`dto`。

## 3. 建立 MSSQL 資料庫

可執行：

```text
database/schema/000_create_database.sql
```

或自行建立：

```sql
CREATE DATABASE SneakerMarketDB;
GO
```

預設本機 JDBC URL：

```text
jdbc:sqlserver://localhost:1433;databaseName=SneakerMarketDB;encrypt=true;trustServerCertificate=true
```

## 4. 設定資料庫密碼

`application-local.properties` 不會直接保存密碼。

請在 IDE Run Configuration 或作業系統環境變數設定：

```text
DB_USERNAME=sa
DB_PASSWORD=你的 MSSQL 密碼
```

`.env.example` 只是變數範例；Spring Boot 預設不會自動讀取 `.env`。

## 5. 開發階段保留的設定

### Session Cookie

```properties
server.servlet.session.cookie.name=SNEAKERMARKET_SESSION
server.servlet.session.cookie.http-only=true
```

local 環境使用：

```properties
server.servlet.session.cookie.secure=false
```

production 使用：

```properties
server.servlet.session.cookie.secure=true
```

若會員系統最後完全採 JWT 且不使用 Server Session，這些 Session Cookie 設定可再移除。

### 圖片上傳

```properties
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=20MB
```

### JPA / Hibernate

本機目前暫時保留：

```properties
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.SQLServerDialect
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.hibernate.ddl-auto=update
spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy
```

`ddl-auto=update` 只適合目前 BASE / 初期開發。等資料表定案後，建議改為 `validate`，正式 DB Schema 改由 `database/migration` SQL 統一管理。

## 6. 啟動

Windows：

```bat
mvnw.cmd spring-boot:run
```

macOS / Linux：

```bash
./mvnw spring-boot:run
```

BASE Health API：

```text
GET http://localhost:8080/api/health
```

## 7. Vue 前端

Vue / Vite 預設：

```text
http://localhost:5173
```

後端：

```text
http://localhost:8080
```

CORS BASE 已預留給前端串 REST API。

## 8. BASE 刻意尚未實作

- 正式 Member / Product / Auction Entity
- JWT 登入
- 商城 CRUD
- 競標 CRUD / 出價 / 結標
- Email 寄送 Service
- WebSocket Endpoint
- Docker / Jenkins

這些由五位組員在相同 BASE 骨架上分工開發。
