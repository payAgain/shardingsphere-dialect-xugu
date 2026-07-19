package com.xugudb.shardingsphere.it;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PaginationProbeIT {
    
    private Properties loadProps() throws Exception {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(Paths.get("src/test/resources/it-xugu.properties"))) {
            props.load(in);
        }
        return props;
    }
    
    @Test
    void probeLimitAndRownum() throws Exception {
        Properties props = loadProps();
        Class.forName(props.getProperty("jdbc.driver"));
        boolean limitOk = false;
        boolean rownumOk = false;
        try (Connection conn = DriverManager.getConnection(
                props.getProperty("jdbc.url"), props.getProperty("jdbc.user"), props.getProperty("jdbc.password"));
             Statement st = conn.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT 1 AS ID FROM DUAL LIMIT 1")) {
                limitOk = rs.next();
            } catch (Exception ignored) {
                limitOk = false;
            }
            try (ResultSet rs = st.executeQuery("SELECT * FROM (SELECT 1 AS ID FROM DUAL) T WHERE ROWNUM <= 1")) {
                rownumOk = rs.next();
            } catch (Exception ignored) {
                rownumOk = false;
            }
        } catch (Exception ex) {
            Assumptions.assumeTrue(false, "XuGu IT host unreachable: " + ex.getMessage());
        }
        System.out.println("LIMIT_OK=" + limitOk + " ROWNUM_OK=" + rownumOk);
        assertTrue(limitOk || rownumOk, "neither LIMIT nor ROWNUM works under compatiblemode=NONE");
    }
}
