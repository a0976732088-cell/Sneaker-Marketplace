/* ============================================================
   SneakerMarketDB - Microsoft SQL Server
   用途：第一次開發時建立專題資料庫
   ============================================================ */

USE master;
GO

IF DB_ID(N'SneakerMarketDB') IS NULL
BEGIN
    EXEC(N'CREATE DATABASE SneakerMarketDB');
END;
GO
