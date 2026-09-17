-- 既有 SQL Server 資料庫：補上 Email 重試欄位與狀態
-- 可重複執行；新資料庫仍以 JPA Entity 為 Schema 來源。
SET XACT_ABORT ON;

BEGIN TRY
BEGIN TRANSACTION;

    IF OBJECT_ID(N'dbo.notification_email_jobs', N'U') IS NULL
        THROW 51000, 'notification_email_jobs table does not exist.', 1;

    IF COL_LENGTH(N'dbo.notification_email_jobs', N'next_retry_at') IS NULL
ALTER TABLE dbo.notification_email_jobs
    ADD next_retry_at datetime2 NULL;

DECLARE @checkCount int;
    DECLARE @checkName sysname;
    DECLARE @checkDefinition nvarchar(max);

SELECT
    @checkCount = COUNT(*),
    @checkName = MAX(name),
    @checkDefinition = MAX(definition)
FROM sys.check_constraints
WHERE parent_object_id = OBJECT_ID(N'dbo.notification_email_jobs')
  AND definition LIKE N'%status%';

IF @checkCount > 1
        THROW 51001, 'Multiple status CHECK constraints need manual review.', 1;

    IF @checkCount = 1
       AND @checkDefinition NOT LIKE N'%RETRY_WAIT%'
BEGIN
        DECLARE @dropSql nvarchar(max) =
            N'ALTER TABLE dbo.notification_email_jobs DROP CONSTRAINT '
            + QUOTENAME(@checkName);

EXEC sys.sp_executesql @dropSql;
END;

    IF @checkCount = 0
       OR @checkDefinition NOT LIKE N'%RETRY_WAIT%'
BEGIN
ALTER TABLE dbo.notification_email_jobs WITH CHECK
    ADD CONSTRAINT CK_notification_email_jobs_status
    CHECK ([status] IN ('PENDING', 'RETRY_WAIT', 'SENT', 'FAILED'));
END;

COMMIT TRANSACTION;
END TRY
BEGIN CATCH
IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;