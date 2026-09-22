IF COL_LENGTH('dbo.calendar_events', 'photo_url') IS NULL
BEGIN
    ALTER TABLE dbo.calendar_events ADD photo_url NVARCHAR(500) NULL;
END;
GO
