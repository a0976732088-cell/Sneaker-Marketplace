# 老師 Day7 專案參考與 BASE 調整

本 BASE 直接參考 `springbootTeacherDay7.zip` 的專案習慣建立。

> 老師 Day7 原始專案為 Spring Boot 3.5.16；SneakerMarket BASE 已升級為 Spring Boot 4.0.8。

## 保留

- Java 17
- Spring Boot 3.5.16（老師原始專案版本；SneakerMarket BASE 使用 4.0.8）
- Maven / Maven Wrapper
- Spring Web MVC / REST API（Boot 4 使用 `spring-boot-starter-webmvc`）
- Spring Data JPA
- Microsoft SQL Server JDBC
- Lombok
- DevTools
- Controller → Service → Repository → Entity 分層概念
- `GenerationType.IDENTITY` 對 MSSQL 自動編號的方向

## 不複製

- Students / Messages / Users / Cart / GoodPhoto 等課堂練習程式
- Thymeleaf template 與 Bootstrap 靜態檔，因 SneakerMarket 前端是獨立專案
- Eclipse `.project`、`.classpath`、`.settings` 等 IDE 個人檔案
- `target/` 編譯產物
- 寫死在 `application.properties` 的 sa 密碼

## SneakerMarket 額外 BASE

- `com.project.sneakermarket` package
- 會員、商城、廣告、行事曆、通知、競標、管理員模組邊界
- `/api/**` REST API 基礎
- Vue/Vite localhost:5173 CORS
- `ApiResponse<T>` 共用回應
- `GlobalExceptionHandler`
- Spring Security BASE（目前 permitAll，會員組實作 JWT 後調整）
- Spring Mail（競標結標 Email）依賴
- Spring WebSocket（競標延伸）依賴
- `@EnableScheduling`，供競標結標與提醒排程後續使用
- MSSQL schema / migration / seed 目錄
