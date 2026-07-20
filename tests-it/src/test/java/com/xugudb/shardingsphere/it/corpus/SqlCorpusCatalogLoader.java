package com.xugudb.shardingsphere.it.corpus;

import org.junit.jupiter.api.Assumptions;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Loads {@code corpus/corpus-cases.tsv} (tab-separated, header row).
 */
final class SqlCorpusCatalogLoader {

    private static final String RESOURCE = "corpus/corpus-cases.tsv";

    private SqlCorpusCatalogLoader() {
    }

    static List<SqlCorpusCase> load() throws Exception {
        List<SqlCorpusCase> cases = new ArrayList<>();
        try (InputStream in = SqlCorpusCatalogLoader.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            Assumptions.assumeTrue(in != null, RESOURCE + " missing on classpath");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String header = reader.readLine();
                Assumptions.assumeTrue(header != null && header.startsWith("id"), "bad corpus TSV header");
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    cases.add(parseLine(line));
                }
            }
        }
        return Collections.unmodifiableList(cases);
    }

    private static SqlCorpusCase parseLine(final String line) {
        String[] parts = line.split("\t", -1);
        Assumptions.assumeTrue(parts.length >= 6, "bad corpus row: " + line);
        String id = parts[0].trim();
        String category = parts[1].trim();
        String sqlOrDesc = parts[2].trim();
        SqlCorpusCase.Expect expect = SqlCorpusCase.Expect.valueOf(parts[3].trim().toUpperCase(Locale.ROOT));
        SqlCorpusCase.Status status = SqlCorpusCase.Status.valueOf(parts[4].trim().toUpperCase(Locale.ROOT));
        SqlCorpusCase.Channel channel = SqlCorpusCase.Channel.valueOf(parts[5].trim().toUpperCase(Locale.ROOT));
        String reason = parts.length > 6 ? parts[6].trim() : "";
        if (status == SqlCorpusCase.Status.DEFER) {
            Assumptions.assumeTrue(!reason.isEmpty(), id + " DEFER requires reason");
        }
        return new SqlCorpusCase(id, category, sqlOrDesc, expect, status, channel, reason);
    }
}
