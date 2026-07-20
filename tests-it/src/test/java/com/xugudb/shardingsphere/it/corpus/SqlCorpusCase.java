package com.xugudb.shardingsphere.it.corpus;

/**
 * One triaged SQL corpus case (G-005 T1).
 */
final class SqlCorpusCase {

    enum Expect {
        PARSE,
        EXECUTE,
        BOTH
    }

    enum Status {
        PASS,
        DEFER
    }

    enum Channel {
        NATIVE,
        SS,
        SS_SHARD,
        PARSE
    }

    private final String id;
    private final String category;
    private final String sqlOrDesc;
    private final Expect expect;
    private final Status status;
    private final Channel channel;
    private final String reason;

    SqlCorpusCase(final String id, final String category, final String sqlOrDesc,
                  final Expect expect, final Status status, final Channel channel, final String reason) {
        this.id = id;
        this.category = category;
        this.sqlOrDesc = sqlOrDesc;
        this.expect = expect;
        this.status = status;
        this.channel = channel;
        this.reason = reason == null ? "" : reason;
    }

    String id() {
        return id;
    }

    String category() {
        return category;
    }

    String sqlOrDesc() {
        return sqlOrDesc;
    }

    Expect expect() {
        return expect;
    }

    Status status() {
        return status;
    }

    Channel channel() {
        return channel;
    }

    String reason() {
        return reason;
    }

    boolean isScenario() {
        return sqlOrDesc.startsWith("scenario:");
    }
}
