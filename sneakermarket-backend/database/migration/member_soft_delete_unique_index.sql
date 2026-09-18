USE SneakerMarketDB;
GO

/* ============================================================
   Member Soft Delete Migration

   目的：
   1. 移除 account / email 原本的全表 UNIQUE Constraint
   2. 改成 Filtered Unique Index
   3. 只有 deleted_at IS NULL 的有效會員必須唯一
   4. 已刪除會員可重新使用相同 account / email 註冊
   ============================================================ */


/* ============================================================
   STEP 1
   移除 account 欄位上的 UNIQUE Constraint
   ============================================================ */

DECLARE @AccountConstraintName NVARCHAR(128);

SELECT TOP 1
    @AccountConstraintName = kc.name
FROM sys.key_constraints kc
JOIN sys.index_columns ic
    ON kc.parent_object_id = ic.object_id
    AND kc.unique_index_id = ic.index_id
JOIN sys.columns c
    ON ic.object_id = c.object_id
    AND ic.column_id = c.column_id
WHERE kc.parent_object_id = OBJECT_ID('dbo.members')
  AND kc.type = 'UQ'
  AND c.name = 'account';

IF @AccountConstraintName IS NOT NULL
BEGIN

    DECLARE @DropAccountSql NVARCHAR(MAX);

    SET @DropAccountSql =
        N'ALTER TABLE dbo.members DROP CONSTRAINT '
        + QUOTENAME(@AccountConstraintName);

    EXEC sp_executesql @DropAccountSql;

    PRINT N'已刪除 account UNIQUE Constraint：'
        + @AccountConstraintName;
END
ELSE
BEGIN
    PRINT N'account 沒有找到舊的 UNIQUE Constraint';
END;
GO


/* ============================================================
   STEP 2
   移除 email 欄位上的 UNIQUE Constraint
   ============================================================ */

DECLARE @EmailConstraintName NVARCHAR(128);

SELECT TOP 1
    @EmailConstraintName = kc.name
FROM sys.key_constraints kc
JOIN sys.index_columns ic
    ON kc.parent_object_id = ic.object_id
    AND kc.unique_index_id = ic.index_id
JOIN sys.columns c
    ON ic.object_id = c.object_id
    AND ic.column_id = c.column_id
WHERE kc.parent_object_id = OBJECT_ID('dbo.members')
  AND kc.type = 'UQ'
  AND c.name = 'email';

IF @EmailConstraintName IS NOT NULL
BEGIN

    DECLARE @DropEmailSql NVARCHAR(MAX);

    SET @DropEmailSql =
        N'ALTER TABLE dbo.members DROP CONSTRAINT '
        + QUOTENAME(@EmailConstraintName);

    EXEC sp_executesql @DropEmailSql;

    PRINT N'已刪除 email UNIQUE Constraint：'
        + @EmailConstraintName;
END
ELSE
BEGIN
    PRINT N'email 沒有找到舊的 UNIQUE Constraint';
END;
GO


/* ============================================================
   STEP 3
   如果舊 filtered index 已存在，先移除
   避免重複執行 Migration 時發生錯誤
   ============================================================ */

IF EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE object_id = OBJECT_ID('dbo.members')
      AND name = 'UX_members_account_active'
)
BEGIN

    DROP INDEX UX_members_account_active
    ON dbo.members;

END;
GO


IF EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE object_id = OBJECT_ID('dbo.members')
      AND name = 'UX_members_email_active'
)
BEGIN

    DROP INDEX UX_members_email_active
    ON dbo.members;

END;
GO


/* ============================================================
   STEP 4
   建立 Soft Delete 專用唯一 Index

   只有 deleted_at IS NULL 才要求 UNIQUE
   ============================================================ */

CREATE UNIQUE INDEX UX_members_account_active
ON dbo.members(account)
WHERE deleted_at IS NULL;
GO


CREATE UNIQUE INDEX UX_members_email_active
ON dbo.members(email)
WHERE deleted_at IS NULL;
GO


/* ============================================================
   STEP 5
   驗證目前 Index
   ============================================================ */

SELECT
    i.name AS index_name,
    i.is_unique,
    i.has_filter,
    i.filter_definition
FROM sys.indexes i
WHERE i.object_id = OBJECT_ID('dbo.members')
ORDER BY i.name;
GO


PRINT N'Member Soft Delete Unique Index Migration 完成';
GO