package com.saicharan.ems;

import com.saicharan.ems.db.ConnectionFactory;

import java.util.UUID;

/** Creates a fresh, isolated in-memory H2 database (MySQL mode) for each test. */
final class TestDatabase {

    private TestDatabase() {
    }

    static ConnectionFactory fresh() {
        String url = "jdbc:h2:mem:test_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        ConnectionFactory factory = new ConnectionFactory(url, "sa", "");
        factory.initSchema();
        return factory;
    }
}
