/*
 * One-time manual compatibility upgrade for an existing SneakerMarketDB.
 * This is an explicitly-invoked, non-Flyway script. Stop the application and
 * its schedulers before execution, and confirm the target database/backup.
 *
 * Hibernate ddl-auto=update does not replace SQL Server CHECK constraints
 * when Java enum values are added. This script changes only CHECK constraints
 * that actually reference these columns:
 *   dbo.auction_orders.order_status
 *   dbo.auction_email_logs.email_type
 *
 * No table is rebuilt and no business row is inserted, updated, or deleted.
 * Existing NULL payment_due_at values are intentionally left unchanged.
 */

USE [SneakerMarketDB];

SET NOCOUNT ON;
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;

BEGIN TRY
    BEGIN TRANSACTION;

    IF DB_NAME() <> N'SneakerMarketDB'
        THROW 51000, 'This upgrade must run only against SneakerMarketDB.', 1;

    DECLARE @applicationLockResult int;
    EXEC @applicationLockResult = sys.sp_getapplock
        @Resource = N'SneakerMarketDB:auction-order-payment-deadline-schema-upgrade',
        @LockMode = N'Exclusive',
        @LockOwner = N'Transaction',
        @LockTimeout = 15000;

    IF @applicationLockResult < 0
        THROW 51001, 'Could not obtain the schema-upgrade application lock.', 1;

    DECLARE @auctionOrdersId int = OBJECT_ID(N'dbo.auction_orders', N'U');
    DECLARE @auctionEmailLogsId int = OBJECT_ID(N'dbo.auction_email_logs', N'U');
    DECLARE @orderStatusColumnId int;
    DECLARE @emailTypeColumnId int;
    DECLARE @auctionOrdersBefore bigint;
    DECLARE @auctionEmailLogsBefore bigint;
    DECLARE @dropSql nvarchar(max);

    IF @auctionOrdersId IS NULL
        THROW 51002, 'Required table dbo.auction_orders does not exist.', 1;

    IF @auctionEmailLogsId IS NULL
        THROW 51003, 'Required table dbo.auction_email_logs does not exist.', 1;

    IF HAS_PERMS_BY_NAME(N'dbo.auction_orders', N'OBJECT', N'VIEW DEFINITION') <> 1
       OR HAS_PERMS_BY_NAME(N'dbo.auction_email_logs', N'OBJECT', N'VIEW DEFINITION') <> 1
        THROW 51004, 'VIEW DEFINITION permission is required for both target tables.', 1;

    SELECT @orderStatusColumnId = column_id
    FROM sys.columns
    WHERE object_id = @auctionOrdersId
      AND name = N'order_status';

    SELECT @emailTypeColumnId = column_id
    FROM sys.columns
    WHERE object_id = @auctionEmailLogsId
      AND name = N'email_type';

    IF @orderStatusColumnId IS NULL
        THROW 51005, 'Required column dbo.auction_orders.order_status does not exist.', 1;

    IF @emailTypeColumnId IS NULL
        THROW 51006, 'Required column dbo.auction_email_logs.email_type does not exist.', 1;

    /* Keep both tables stable through validation, DDL, and row-count checks. */
    SELECT @auctionOrdersBefore = COUNT_BIG(*)
    FROM dbo.auction_orders WITH (TABLOCKX, HOLDLOCK);

    SELECT @auctionEmailLogsBefore = COUNT_BIG(*)
    FROM dbo.auction_email_logs WITH (TABLOCKX, HOLDLOCK);

    /* Binary collation enforces exact Java enum spelling during preflight. */
    IF EXISTS (
        SELECT 1
        FROM dbo.auction_orders
        WHERE order_status IS NULL
           OR order_status COLLATE Latin1_General_100_BIN2 NOT IN (
                N'PENDING_PAYMENT', N'PAID', N'SHIPPED',
                N'COMPLETED', N'CANCELLED', N'ABANDONED'
           )
    )
        THROW 51007, 'Unsupported auction order status found; no schema changes were applied.', 1;

    IF EXISTS (
        SELECT 1
        FROM dbo.auction_email_logs
        WHERE email_type IS NULL
           OR email_type COLLATE Latin1_General_100_BIN2 NOT IN (
                N'SOLD_SELLER', N'UNSOLD_SELLER', N'WON', N'LOST',
                N'ORDER_ABANDONED_BUYER', N'ORDER_ABANDONED_SELLER'
           )
    )
        THROW 51008, 'Unsupported auction email type found; no schema changes were applied.', 1;

    /*
     * Resolve constraints by column metadata/dependencies, never by generated
     * name patterns or definition LIKE expressions.
     */
    DECLARE @orderStatusChecks TABLE (
        object_id int PRIMARY KEY,
        constraint_name sysname NOT NULL,
        is_system_named bit NOT NULL,
        constraint_definition nvarchar(max) NOT NULL
    );

    INSERT INTO @orderStatusChecks (
        object_id, constraint_name, is_system_named, constraint_definition
    )
    SELECT cc.object_id, cc.name, cc.is_system_named, cc.definition
    FROM sys.check_constraints AS cc
    WHERE cc.parent_object_id = @auctionOrdersId
      AND (
          cc.parent_column_id = @orderStatusColumnId
          OR EXISTS (
              SELECT 1
              FROM sys.sql_expression_dependencies AS sed
              WHERE sed.referencing_id = cc.object_id
                AND sed.referencing_class = 1
                AND sed.referenced_class = 1
                AND sed.referenced_id = @auctionOrdersId
                AND sed.referenced_minor_id = @orderStatusColumnId
          )
      );

    IF EXISTS (
        SELECT 1
        FROM @orderStatusChecks
        WHERE constraint_name <> N'CK_auction_orders_order_status'
          AND (
              is_system_named = 0
              OR REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
                    UPPER(constraint_definition), N'[', N''), N']', N''),
                    N'(', N''), N')', N''), N' ', N'') <>
                 N'ORDER_STATUS=''CANCELLED''ORORDER_STATUS=''COMPLETED''ORORDER_STATUS=''SHIPPED''ORORDER_STATUS=''PAID''ORORDER_STATUS=''PENDING_PAYMENT'''
          )
    )
        THROW 51009, 'An unrecognized non-canonical order-status CHECK requires manual review.', 1;

    IF EXISTS (
        SELECT 1
        FROM @orderStatusChecks AS candidate
        JOIN sys.sql_expression_dependencies AS sed
          ON sed.referencing_id = candidate.object_id
        WHERE sed.referenced_id = @auctionOrdersId
          AND sed.referenced_minor_id > 0
          AND sed.referenced_minor_id <> @orderStatusColumnId
    )
        THROW 51010, 'A multi-column order-status CHECK requires manual review.', 1;

    SET @dropSql = N'';
    SELECT @dropSql +=
        N'ALTER TABLE [dbo].[auction_orders] DROP CONSTRAINT '
        + QUOTENAME(constraint_name) + N';'
    FROM @orderStatusChecks;

    IF @dropSql <> N''
        EXEC sys.sp_executesql @dropSql;

    IF EXISTS (
        SELECT 1
        FROM sys.check_constraints
        WHERE parent_object_id = @auctionOrdersId
          AND name = N'CK_auction_orders_order_status'
    )
        THROW 51011, 'Canonical order-status constraint remains after targeted cleanup.', 1;

    ALTER TABLE dbo.auction_orders WITH CHECK
        ADD CONSTRAINT CK_auction_orders_order_status CHECK (
            order_status IN (
                N'PENDING_PAYMENT', N'PAID', N'SHIPPED',
                N'COMPLETED', N'CANCELLED', N'ABANDONED'
            )
        );

    ALTER TABLE dbo.auction_orders
        CHECK CONSTRAINT CK_auction_orders_order_status;

    DECLARE @emailTypeChecks TABLE (
        object_id int PRIMARY KEY,
        constraint_name sysname NOT NULL,
        is_system_named bit NOT NULL,
        constraint_definition nvarchar(max) NOT NULL
    );

    INSERT INTO @emailTypeChecks (
        object_id, constraint_name, is_system_named, constraint_definition
    )
    SELECT cc.object_id, cc.name, cc.is_system_named, cc.definition
    FROM sys.check_constraints AS cc
    WHERE cc.parent_object_id = @auctionEmailLogsId
      AND (
          cc.parent_column_id = @emailTypeColumnId
          OR EXISTS (
              SELECT 1
              FROM sys.sql_expression_dependencies AS sed
              WHERE sed.referencing_id = cc.object_id
                AND sed.referencing_class = 1
                AND sed.referenced_class = 1
                AND sed.referenced_id = @auctionEmailLogsId
                AND sed.referenced_minor_id = @emailTypeColumnId
          )
      );

    IF EXISTS (
        SELECT 1
        FROM @emailTypeChecks
        WHERE constraint_name <> N'CK_auction_email_logs_type'
    )
        THROW 51012, 'An unrecognized non-canonical email-type CHECK requires manual review.', 1;

    IF EXISTS (
        SELECT 1
        FROM @emailTypeChecks AS candidate
        JOIN sys.sql_expression_dependencies AS sed
          ON sed.referencing_id = candidate.object_id
        WHERE sed.referenced_id = @auctionEmailLogsId
          AND sed.referenced_minor_id > 0
          AND sed.referenced_minor_id <> @emailTypeColumnId
    )
        THROW 51013, 'A multi-column email-type CHECK requires manual review.', 1;

    SET @dropSql = N'';
    SELECT @dropSql +=
        N'ALTER TABLE [dbo].[auction_email_logs] DROP CONSTRAINT '
        + QUOTENAME(constraint_name) + N';'
    FROM @emailTypeChecks;

    IF @dropSql <> N''
        EXEC sys.sp_executesql @dropSql;

    IF EXISTS (
        SELECT 1
        FROM sys.check_constraints
        WHERE parent_object_id = @auctionEmailLogsId
          AND name = N'CK_auction_email_logs_type'
    )
        THROW 51014, 'Canonical email-type constraint remains after targeted cleanup.', 1;

    ALTER TABLE dbo.auction_email_logs WITH CHECK
        ADD CONSTRAINT CK_auction_email_logs_type CHECK (
            email_type IN (
                N'SOLD_SELLER', N'UNSOLD_SELLER', N'WON', N'LOST',
                N'ORDER_ABANDONED_BUYER', N'ORDER_ABANDONED_SELLER'
            )
        );

    ALTER TABLE dbo.auction_email_logs
        CHECK CONSTRAINT CK_auction_email_logs_type;

    IF (
        SELECT COUNT(*)
        FROM sys.check_constraints AS cc
        WHERE cc.parent_object_id = @auctionOrdersId
          AND (
              cc.parent_column_id = @orderStatusColumnId
              OR EXISTS (
                  SELECT 1
                  FROM sys.sql_expression_dependencies AS sed
                  WHERE sed.referencing_id = cc.object_id
                    AND sed.referenced_id = @auctionOrdersId
                    AND sed.referenced_minor_id = @orderStatusColumnId
              )
          )
    ) <> 1
        THROW 51015, 'Order-status cleanup did not produce exactly one CHECK.', 1;

    IF NOT EXISTS (
        SELECT 1
        FROM sys.check_constraints
        WHERE parent_object_id = @auctionOrdersId
          AND name = N'CK_auction_orders_order_status'
          AND is_disabled = 0
          AND is_not_trusted = 0
    )
        THROW 51016, 'Canonical order-status CHECK is not enabled and trusted.', 1;

    IF (
        SELECT COUNT(*)
        FROM sys.check_constraints AS cc
        WHERE cc.parent_object_id = @auctionEmailLogsId
          AND (
              cc.parent_column_id = @emailTypeColumnId
              OR EXISTS (
                  SELECT 1
                  FROM sys.sql_expression_dependencies AS sed
                  WHERE sed.referencing_id = cc.object_id
                    AND sed.referenced_id = @auctionEmailLogsId
                    AND sed.referenced_minor_id = @emailTypeColumnId
              )
          )
    ) <> 1
        THROW 51017, 'Email-type cleanup did not produce exactly one CHECK.', 1;

    IF NOT EXISTS (
        SELECT 1
        FROM sys.check_constraints
        WHERE parent_object_id = @auctionEmailLogsId
          AND name = N'CK_auction_email_logs_type'
          AND is_disabled = 0
          AND is_not_trusted = 0
    )
        THROW 51018, 'Canonical email-type CHECK is not enabled and trusted.', 1;

    IF @auctionOrdersBefore <> (SELECT COUNT_BIG(*) FROM dbo.auction_orders)
        THROW 51019, 'dbo.auction_orders row count changed unexpectedly.', 1;

    IF @auctionEmailLogsBefore <> (SELECT COUNT_BIG(*) FROM dbo.auction_email_logs)
        THROW 51020, 'dbo.auction_email_logs row count changed unexpectedly.', 1;

    COMMIT TRANSACTION;

    SELECT
        @auctionOrdersBefore AS auction_orders_before,
        (SELECT COUNT_BIG(*) FROM dbo.auction_orders) AS auction_orders_after,
        @auctionEmailLogsBefore AS auction_email_logs_before,
        (SELECT COUNT_BIG(*) FROM dbo.auction_email_logs) AS auction_email_logs_after;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;

    THROW;
END CATCH;
