/*
 * 會員 Google Calendar OAuth 連線。
 * Token 由後端加密後才存入資料庫。
 */
IF OBJECT_ID(
    'dbo.google_calendar_connections',
    'U'
) IS NULL
BEGIN
CREATE TABLE dbo.google_calendar_connections
(
    connection_id BIGINT IDENTITY(1,1) NOT NULL,

    member_id BIGINT NOT NULL,

    access_token_encrypted NVARCHAR(MAX) NOT NULL,

    refresh_token_encrypted NVARCHAR(MAX) NOT NULL,

    access_token_expires_at DATETIME2(0) NOT NULL,

    granted_scope NVARCHAR(1000) NULL,

    created_at DATETIME2(0) NOT NULL
            CONSTRAINT DF_google_calendar_connections_created
            DEFAULT (SYSDATETIME()),

    updated_at DATETIME2(0) NOT NULL
            CONSTRAINT DF_google_calendar_connections_updated
            DEFAULT (SYSDATETIME()),

    CONSTRAINT PK_google_calendar_connections
        PRIMARY KEY (connection_id),

    CONSTRAINT UQ_google_calendar_connections_member
        UNIQUE (member_id),

    CONSTRAINT FK_google_calendar_connections_member
        FOREIGN KEY (member_id)
            REFERENCES dbo.members(member_id)
);
END;
GO

/*
 * 站內活動和 Google Calendar Event 的對應關係。
 */
IF OBJECT_ID(
    'dbo.google_calendar_event_links',
    'U'
) IS NULL
BEGIN
CREATE TABLE dbo.google_calendar_event_links
(
    link_id BIGINT IDENTITY(1,1) NOT NULL,

    member_id BIGINT NOT NULL,

    event_id BIGINT NOT NULL,

    google_event_id NVARCHAR(255) NOT NULL,

    sync_status VARCHAR(20) NOT NULL
        CONSTRAINT DF_google_calendar_event_links_status
        DEFAULT ('SYNCED'),

    last_error NVARCHAR(500) NULL,

    last_synced_at DATETIME2(0) NOT NULL
            CONSTRAINT DF_google_calendar_event_links_synced
            DEFAULT (SYSDATETIME()),

    created_at DATETIME2(0) NOT NULL
            CONSTRAINT DF_google_calendar_event_links_created
            DEFAULT (SYSDATETIME()),

    updated_at DATETIME2(0) NOT NULL
            CONSTRAINT DF_google_calendar_event_links_updated
            DEFAULT (SYSDATETIME()),

    CONSTRAINT PK_google_calendar_event_links
        PRIMARY KEY (link_id),

    CONSTRAINT UQ_google_calendar_event_links_member_event
        UNIQUE (member_id, event_id),

    CONSTRAINT FK_google_calendar_event_links_member
        FOREIGN KEY (member_id)
            REFERENCES dbo.members(member_id),

    CONSTRAINT FK_google_calendar_event_links_event
        FOREIGN KEY (event_id)
            REFERENCES dbo.calendar_events(event_id)
);
END;
GO