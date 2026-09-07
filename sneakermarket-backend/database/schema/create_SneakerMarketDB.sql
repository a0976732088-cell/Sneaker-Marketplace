USE SneakerMarketDB;
GO

CREATE TABLE dbo.advertisements (
                                    ad_id BIGINT IDENTITY(1,1) PRIMARY KEY,
                                    created_by INT NOT NULL,

                                    ad_name NVARCHAR(120) NOT NULL,
                                    image_url NVARCHAR(500) NOT NULL,
                                    ad_description NVARCHAR(1000) NULL,

                                    ad_type NVARCHAR(30) NOT NULL,
                                    ad_placement NVARCHAR(30) NOT NULL,

                                    start_at DATETIME2(0) NULL,
                                    end_at DATETIME2(0) NULL,

                                    target_type NVARCHAR(30) NOT NULL,
                                    target_id BIGINT NULL,
                                    target_url NVARCHAR(500) NULL,

                                    sort_order INT NOT NULL DEFAULT (0),
                                    ad_status NVARCHAR(20) NOT NULL,

                                    created_at DATETIME2(0) NOT NULL,
                                    updated_at DATETIME2(0) NOT NULL,

                                    FOREIGN KEY (created_by)
                                        REFERENCES dbo.members(member_id)
);
GO
CREATE TABLE dbo.advertisement_stats (
                                         stat_id BIGINT IDENTITY(1,1) PRIMARY KEY,
                                         ad_id BIGINT NOT NULL,

                                         stat_date DATE NOT NULL,
                                         click_count BIGINT NOT NULL DEFAULT (0),
                                         view_count BIGINT NOT NULL DEFAULT (0),

                                         updated_at DATETIME2(0) NOT NULL,

                                         FOREIGN KEY (ad_id)
                                             REFERENCES dbo.advertisements(ad_id)
);
GO