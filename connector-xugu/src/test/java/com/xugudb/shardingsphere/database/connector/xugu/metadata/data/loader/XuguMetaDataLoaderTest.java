package com.xugudb.shardingsphere.database.connector.xugu.metadata.data.loader;

import org.apache.shardingsphere.database.connector.core.metadata.data.loader.DialectMetaDataLoader;
import org.apache.shardingsphere.database.connector.core.metadata.data.loader.MetaDataLoaderMaterial;
import org.apache.shardingsphere.database.connector.core.metadata.data.model.ColumnMetaData;
import org.apache.shardingsphere.database.connector.core.metadata.data.model.IndexMetaData;
import org.apache.shardingsphere.database.connector.core.metadata.data.model.SchemaMetaData;
import org.apache.shardingsphere.database.connector.core.metadata.data.model.TableMetaData;
import org.apache.shardingsphere.database.connector.core.metadata.database.datatype.DataTypeRegistry;
import org.apache.shardingsphere.database.connector.core.metadata.database.enums.TableType;
import org.apache.shardingsphere.database.connector.core.spi.DatabaseTypedSPILoader;
import org.apache.shardingsphere.database.connector.core.type.DatabaseType;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class XuguMetaDataLoaderTest {
    
    private static final String TABLE_META_DATA_SQL =
            "SELECT s.schema_name, t.TABLE_NAME, c.COL_NAME, c.NOT_NULL, c.TYPE_NAME, c.COL_NO, c.IS_HIDE ,c.IS_SERIAL, c.COLLATOR FROM ALL_COLUMNS AS c "
                    + "join ALL_TABLES AS t on t.table_id = c.table_id "
                    + "JOIN ALL_SCHEMAS AS s on t.schema_id = s.schema_id "
                    + "WHERE s.schema_name = ? AND t.TABLE_NAME IN ('tbl') ORDER BY COL_NO";
    
    private static final String VIEW_META_DATA_SQL =
            "SELECT s.schema_name, v.VIEW_NAME, c.COL_NAME, null as NOT_NULL, c.TYPE_NAME, c.COL_NO, null as IS_HIDE ,null as IS_SERIAL, null as COLLATOR FROM ALL_VIEW_COLUMNS AS c "
                    + "join ALL_VIEWS AS v on v.VIEW_ID = c.VIEW_ID "
                    + "JOIN ALL_SCHEMAS AS s on v.schema_id = s.schema_id "
                    + "WHERE s.schema_name = ? AND v.VIEW_NAME IN ('tbl') ORDER BY COL_NO";
    
    private static final String VIEW_NAME_SQL =
            "SELECT v.VIEW_NAME FROM ALL_VIEWS AS v JOIN ALL_SCHEMAS as s on v.schema_id = s.schema_id WHERE s.schema_name = ? AND v.VIEW_NAME IN ('tbl')";
    
    private static final String INDEX_META_DATA_SQL =
            "SELECT s.schema_name,t.TABLE_NAME,i.INDEX_NAME,IS_UNIQUE FROM ALL_INDEXES AS i "
                    + "JOIN ALL_TABLES AS t ON i.table_id = t.table_id "
                    + "JOIN ALL_SCHEMAS AS s on t.schema_id = s.schema_id "
                    + "WHERE s.schema_name = ? AND t.TABLE_NAME IN ('tbl')";
    
    private static final String PRIMARY_KEY_META_DATA_SQL =
            "SELECT s.schema_name, t.table_name, cs.define FROM ALL_CONSTRAINTS AS cs "
                    + "JOIN all_tables AS t ON cs.table_id = t.table_id "
                    + "JOIN ALL_SCHEMAS AS s ON t.schema_id = s.schema_id "
                    + "WHERE cs.CONS_TYPE = 'p' AND s.schema_name = 'TEST' AND t.TABLE_NAME IN ('tbl')";
    
    private static final String INDEX_COLUMN_META_DATA_SQL =
            "SELECT i.KEYS FROM ALL_INDEXES AS i "
                    + "JOIN ALL_TABLES AS t ON i.table_id = t.table_id "
                    + "JOIN ALL_SCHEMAS AS s on t.schema_id = s.schema_id "
                    + "WHERE s.schema_name = ? AND t.TABLE_NAME = ? AND i.INDEX_NAME = ?";
    
    private final DatabaseType databaseType = TypedSPILoader.getService(DatabaseType.class, "XuGu");
    
    private final DialectMetaDataLoader dialectMetaDataLoader = DatabaseTypedSPILoader.getService(DialectMetaDataLoader.class, databaseType);
    
    @SuppressWarnings({"JDBCResourceOpenedButNotSafelyClosed", "resource"})
    @Test
    void assertLoadWithPrimaryKeyAndIndex() throws SQLException {
        DataSource dataSource = mockDataSource();
        ResultSet tableMetaDataResultSet = mockTableMetaDataResultSet();
        ResultSet indexMetaDataResultSet = mockIndexMetaDataResultSet();
        ResultSet indexColumnMetaDataResultSet = mockIndexColumnMetaDataResultSet();
        ResultSet primaryKeysResultSet = mockPrimaryKeysMetaDataResultSet();
        ResultSet emptyResultSet = mock(ResultSet.class);
        when(emptyResultSet.next()).thenReturn(false);
        when(dataSource.getConnection().getSchema()).thenReturn("TEST");
        when(dataSource.getConnection().prepareStatement(VIEW_NAME_SQL).executeQuery()).thenReturn(emptyResultSet);
        when(dataSource.getConnection().prepareStatement(TABLE_META_DATA_SQL).executeQuery()).thenReturn(tableMetaDataResultSet);
        when(dataSource.getConnection().prepareStatement(VIEW_META_DATA_SQL).executeQuery()).thenReturn(emptyResultSet);
        when(dataSource.getConnection().prepareStatement(PRIMARY_KEY_META_DATA_SQL).executeQuery()).thenReturn(primaryKeysResultSet);
        when(dataSource.getConnection().prepareStatement(INDEX_META_DATA_SQL).executeQuery()).thenReturn(indexMetaDataResultSet);
        when(dataSource.getConnection().prepareStatement(INDEX_COLUMN_META_DATA_SQL).executeQuery()).thenReturn(indexColumnMetaDataResultSet);
        TableMetaData actualTableMetaData = assertAndGetSingleTableMetaData(loadMetaData(dataSource));
        assertThat(actualTableMetaData.getName(), is("tbl"));
        assertThat(actualTableMetaData.getType(), is(TableType.TABLE));
        assertThat(actualTableMetaData.getIndexes().size(), is(1));
        List<IndexMetaData> actualIndexes = new ArrayList<>(actualTableMetaData.getIndexes());
        assertIndexMetaData(actualIndexes.get(0), "id", true, Collections.singletonList("id"));
        List<ColumnMetaData> columnMetaDataList = new ArrayList<>(actualTableMetaData.getColumns());
        assertThat(columnMetaDataList.size(), is(3));
        assertColumnMetaData(columnMetaDataList.get(0), new ColumnMetaData("id", Types.INTEGER, true, true, true, true, false, false));
        assertColumnMetaData(columnMetaDataList.get(1), new ColumnMetaData("name", Types.VARCHAR, false, false, false, false, false, true));
        assertColumnMetaData(columnMetaDataList.get(2), new ColumnMetaData("creation_time", Types.TIMESTAMP, false, false, false, true, false, true));
    }
    
    @SuppressWarnings({"JDBCResourceOpenedButNotSafelyClosed", "resource"})
    @Test
    void assertLoadWithoutPrimaryKey() throws SQLException {
        DataSource dataSource = mockDataSource();
        ResultSet tableMetaDataResultSet = mockTableMetaDataResultSet();
        ResultSet emptyResultSet = mock(ResultSet.class);
        when(emptyResultSet.next()).thenReturn(false);
        when(dataSource.getConnection().getSchema()).thenReturn("TEST");
        when(dataSource.getConnection().prepareStatement(VIEW_NAME_SQL).executeQuery()).thenReturn(emptyResultSet);
        when(dataSource.getConnection().prepareStatement(TABLE_META_DATA_SQL).executeQuery()).thenReturn(tableMetaDataResultSet);
        when(dataSource.getConnection().prepareStatement(VIEW_META_DATA_SQL).executeQuery()).thenReturn(emptyResultSet);
        when(dataSource.getConnection().prepareStatement(PRIMARY_KEY_META_DATA_SQL).executeQuery()).thenReturn(emptyResultSet);
        when(dataSource.getConnection().prepareStatement(INDEX_META_DATA_SQL).executeQuery()).thenReturn(emptyResultSet);
        TableMetaData actualTableMetaData = assertAndGetSingleTableMetaData(loadMetaData(dataSource));
        assertThat(actualTableMetaData.getIndexes().size(), is(0));
        List<ColumnMetaData> columnMetaDataList = new ArrayList<>(actualTableMetaData.getColumns());
        assertColumnMetaData(columnMetaDataList.get(0), new ColumnMetaData("id", Types.INTEGER, false, true, true, true, false, false));
        assertColumnMetaData(columnMetaDataList.get(1), new ColumnMetaData("name", Types.VARCHAR, false, false, false, false, false, true));
        assertColumnMetaData(columnMetaDataList.get(2), new ColumnMetaData("creation_time", Types.TIMESTAMP, false, false, false, true, false, true));
    }
    
    @SuppressWarnings("JDBCResourceOpenedButNotSafelyClosed")
    private DataSource mockDataSource() throws SQLException {
        DataSource result = mock(DataSource.class, RETURNS_DEEP_STUBS);
        ResultSet typeInfoResultSet = mockTypeInfoResultSet();
        when(result.getConnection().getMetaData().getTypeInfo()).thenReturn(typeInfoResultSet);
        return result;
    }
    
    private ResultSet mockTypeInfoResultSet() throws SQLException {
        ResultSet result = mock(ResultSet.class);
        when(result.next()).thenReturn(true, true, true, false);
        when(result.getString("TYPE_NAME")).thenReturn("int", "varchar", "TIMESTAMP");
        when(result.getInt("DATA_TYPE")).thenReturn(Types.INTEGER, Types.VARCHAR, Types.TIMESTAMP);
        return result;
    }
    
    private ResultSet mockTableMetaDataResultSet() throws SQLException {
        ResultSet result = mock(ResultSet.class);
        when(result.next()).thenReturn(true, true, true, false);
        when(result.getString("TABLE_NAME")).thenReturn("tbl");
        when(result.getString("COL_NAME")).thenReturn("id", "name", "creation_time");
        when(result.getString("TYPE_NAME")).thenReturn("int", "varchar", "TIMESTAMP(6)");
        when(result.getBoolean("IS_HIDE")).thenReturn(false, true, false);
        when(result.getBoolean("IS_SERIAL")).thenReturn(true, false, false);
        when(result.getString("COLLATOR")).thenReturn("BINARY_CS", "BINARY_CI", "BINARY_CI");
        when(result.getBoolean("NOT_NULL")).thenReturn(true, false, false);
        return result;
    }
    
    private ResultSet mockIndexMetaDataResultSet() throws SQLException {
        ResultSet result = mock(ResultSet.class);
        when(result.next()).thenReturn(true, false);
        when(result.getString("INDEX_NAME")).thenReturn("id");
        when(result.getString("TABLE_NAME")).thenReturn("tbl");
        when(result.getBoolean("IS_UNIQUE")).thenReturn(true);
        return result;
    }
    
    private ResultSet mockIndexColumnMetaDataResultSet() throws SQLException {
        ResultSet result = mock(ResultSet.class);
        when(result.next()).thenReturn(true, false);
        when(result.getString("KEYS")).thenReturn("id");
        return result;
    }
    
    private ResultSet mockPrimaryKeysMetaDataResultSet() throws SQLException {
        ResultSet result = mock(ResultSet.class);
        when(result.next()).thenReturn(true, false);
        when(result.getString("table_name")).thenReturn("tbl");
        when(result.getString("define")).thenReturn("id");
        return result;
    }
    
    private Collection<SchemaMetaData> loadMetaData(final DataSource dataSource) throws SQLException {
        DataTypeRegistry.load(dataSource, "XuGu");
        return dialectMetaDataLoader.load(new MetaDataLoaderMaterial(Collections.singleton("tbl"), "foo_ds", dataSource, databaseType, "sharding_db"));
    }
    
    private TableMetaData assertAndGetSingleTableMetaData(final Collection<SchemaMetaData> schemaMetaDataList) {
        assertThat(schemaMetaDataList.size(), is(1));
        SchemaMetaData actualSchemaMetaData = schemaMetaDataList.iterator().next();
        assertThat(actualSchemaMetaData.getName(), is("sharding_db"));
        assertThat(actualSchemaMetaData.getTables().size(), is(1));
        return actualSchemaMetaData.getTables().iterator().next();
    }
    
    private void assertColumnMetaData(final ColumnMetaData actual, final ColumnMetaData expected) {
        assertThat(actual.getName(), is(expected.getName()));
        assertThat(actual.getDataType(), is(expected.getDataType()));
        assertThat(actual.isPrimaryKey(), is(expected.isPrimaryKey()));
        assertThat(actual.isGenerated(), is(expected.isGenerated()));
        assertThat(actual.isCaseSensitive(), is(expected.isCaseSensitive()));
        assertThat(actual.isVisible(), is(expected.isVisible()));
        assertThat(actual.isUnsigned(), is(expected.isUnsigned()));
        assertThat(actual.isNullable(), is(expected.isNullable()));
    }
    
    private void assertIndexMetaData(final IndexMetaData actual, final String expectedName, final boolean expectedUnique, final Collection<String> expectedColumns) {
        assertThat(actual.getName(), is(expectedName));
        assertThat(actual.getColumns(), is(expectedColumns));
        assertThat(actual.isUnique(), is(expectedUnique));
    }
}
