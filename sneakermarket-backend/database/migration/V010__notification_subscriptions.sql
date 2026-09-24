/*
 * 通用會員關注資料。
 */
IF OBJECT_ID(
    'dbo.notification_subscriptions',
    'U'
) IS NULL
BEGIN
CREATE TABLE dbo.notification_subscriptions
(
    subscription_id BIGINT IDENTITY(1,1) NOT NULL,

    member_id BIGINT NOT NULL,

    target_type VARCHAR(20) NOT NULL,

    target_id BIGINT NOT NULL,

    scope VARCHAR(30) NOT NULL,

    created_at DATETIME2(0) NOT NULL
            CONSTRAINT DF_notification_subscriptions_created_at
            DEFAULT (SYSDATETIME()),

    CONSTRAINT PK_notification_subscriptions
        PRIMARY KEY (subscription_id),

    CONSTRAINT FK_notification_subscriptions_member
        FOREIGN KEY (member_id)
            REFERENCES dbo.members(member_id),

    CONSTRAINT CK_notification_subscriptions_target_type
        CHECK (
            target_type IN (
                            'BRAND',
                            'CATEGORY'
                )
            ),

    CONSTRAINT CK_notification_subscriptions_scope
        CHECK (
            scope IN (
                      'NEW_PRODUCT',
                      'AUCTION'
                )
            ),

    CONSTRAINT UQ_notification_subscriptions_member_target
        UNIQUE (
                member_id,
                target_type,
                target_id,
                scope
            )
);
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name =
          'IX_notification_subscriptions_target'
      AND object_id =
          OBJECT_ID(
              'dbo.notification_subscriptions'
          )
)
BEGIN
    CREATE NONCLUSTERED INDEX
        IX_notification_subscriptions_target
    ON dbo.notification_subscriptions (
        scope,
        target_type,
        target_id
    );
END;
GO

ALTER TABLE dbo.calendar_events
ALTER COLUMN title NVARCHAR(150) NOT NULL;

ALTER TABLE dbo.calendar_events
ALTER COLUMN description NVARCHAR(MAX) NULL;

ALTER TABLE dbo.calendar_events
ALTER COLUMN venue_name NVARCHAR(150) NULL;

ALTER TABLE dbo.calendar_events
ALTER COLUMN address NVARCHAR(300) NULL;