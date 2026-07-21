# Code Reuse

## Good

- Copy **module/SPI structure** from mature SS dialects.
- Reuse core helpers from `org.apache.shardingsphere.*` APIs.

## Bad

- Reusing MySQL trunk fallback as product behavior.
- Copying MySQL `information_schema` SQL into XuGu loaders.
- Porting Superpowers harness plans as the execution engine (use Trellis tasks).
