# Parser

## Modules

- `parser-sql-statement-xugu`
- `parser-sql-engine-xugu`

## Rules

- Grammar targets XuGu **NONE** mode only.
- Facade + visitors must register via SPI for `"XuGu"`.
- Do not depend on `shardingsphere-parser-sql-engine-mysql` as a fallback path for this product.
