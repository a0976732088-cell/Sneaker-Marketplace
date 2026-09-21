/*
 * 活動地圖使用的場地與座標欄位。
 * 全部允許 NULL，避免影響既有活動。
 */

IF COL_LENGTH(
    'dbo.calendar_events',
    'venue_name'
) IS NULL
BEGIN
    ALTER TABLE dbo.calendar_events
        ADD venue_name NVARCHAR(150) NULL;
END;
GO

IF COL_LENGTH(
    'dbo.calendar_events',
    'address'
) IS NULL
BEGIN
    ALTER TABLE dbo.calendar_events
        ADD address NVARCHAR(300) NULL;
END;
GO

IF COL_LENGTH(
    'dbo.calendar_events',
    'latitude'
) IS NULL
BEGIN
    ALTER TABLE dbo.calendar_events
        ADD latitude DECIMAL(9, 6) NULL;
END;
GO

IF COL_LENGTH(
    'dbo.calendar_events',
    'longitude'
) IS NULL
BEGIN
    ALTER TABLE dbo.calendar_events
        ADD longitude DECIMAL(10, 6) NULL;
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE name = 'CK_calendar_events_latitude'
)
BEGIN
    ALTER TABLE dbo.calendar_events
        ADD CONSTRAINT CK_calendar_events_latitude
        CHECK (
            latitude IS NULL
            OR latitude BETWEEN -90 AND 90
        );
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE name = 'CK_calendar_events_longitude'
)
BEGIN
    ALTER TABLE dbo.calendar_events
        ADD CONSTRAINT CK_calendar_events_longitude
        CHECK (
            longitude IS NULL
            OR longitude BETWEEN -180 AND 180
        );
END;
GO