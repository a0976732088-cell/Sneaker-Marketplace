# Database scripts

本專題使用 Microsoft SQL Server（MSSQL）／T-SQL。

- `schema/`：初始 CREATE TABLE 腳本。
- `migration/`：後續 ALTER TABLE／新增索引等變更腳本。
- `seed/`：每位組員本機開發使用的測試資料。

規則：已合併的 migration 不回頭修改；需要變更時新增下一支 SQL。
