package com.jeppeman.highlite;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import com.jeppeman.highlite.test.table.TestDatabase;
import com.jeppeman.highlite.test.table.TestNonSerializable;
import com.jeppeman.highlite.test.table.TestSerializable;
import com.jeppeman.highlite.test.table.TestTable;
import com.jeppeman.highlite.test.table.TestTable2;
import com.jeppeman.highlite.test.table.TestTable3;
import com.jeppeman.highlite.test.table.TestTable4;
import com.jeppeman.highlite.test.table.TestTable5;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class SQLiteOperatorTest {

    private Context getContext() {
        return RuntimeEnvironment.application;
    }

    @Test(expected = RuntimeException.class)
    public void testInvalidClassPassed() {
        SQLiteOperator.from(getContext(), ArrayList.class).getList();
    }

    @After
    public void finishComponentTesting() throws ClassNotFoundException {
        resetSingleton(Class.forName(TestDatabase.class.getCanonicalName() + "_OpenHelper"),
                "sInstance");
    }

    private void resetSingleton(Class clazz, String fieldName) {
        Field instance;
        try {
            instance = clazz.getDeclaredField(fieldName);
            instance.setAccessible(true);
            instance.set(null, null);
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    private SQLiteOpenHelper getHelperInstance() throws ClassNotFoundException,
            NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return (SQLiteOpenHelper) Class.forName(TestDatabase.class.getCanonicalName()
                + "_OpenHelper").getMethod("getInstance", Context.class).invoke(null, getContext());
    }

    @Test
    public void testSaveAndGetSingleById() throws Exception {
        SQLiteOperator<TestTable> operator = SQLiteOperator.from(getContext(), TestTable.class);
        TestTable table = operator.getSingle(1).executeBlocking();
        assertNull(table);
        operator.save(new TestTable()).executeBlocking();
        table = operator.getSingle(1).executeBlocking();
        assertNotNull(table);
        assertEquals(1, table.id);
    }

    @Test
    public void testSaveAndGetSingleByRawQuery() throws Exception {
        SQLiteOperator<TestTable> operator = SQLiteOperator.from(getContext(), TestTable.class);
        TestTable table = operator
                .getSingle()
                .withRawQuery("SELECT * FROM testTable WHERE id = ?", 1)
                .executeBlocking();
        assertNull(table);
        TestTable newTable = new TestTable();
        newTable.testSerializable = new TestSerializable("test");
        newTable.testString = "123";
        newTable.testBoolean = true;
        newTable.testList = Arrays.asList("1", "2", "3");
        operator.save(newTable).executeBlocking();
        table = operator
                .getSingle()
                .withRawQuery("SELECT * FROM testTable WHERE id = ?", 1)
                .executeBlocking();
        assertNotNull(table);
        assertEquals(table.id, 1);
        assertEquals(table.testString, "123");
        assertEquals(table.testSerializable.testField, "test");
        assertEquals(Arrays.toString(newTable.testList.toArray()),
                Arrays.toString(new String[]{"1", "2", "3"}));
    }

    @Test
    public void testSaveAndGetSingleByQueryBuilder() throws Exception {
        SQLiteOperator<TestTable> operator = SQLiteOperator.from(getContext(), TestTable.class);
        TestTable table = operator
                .getSingle()
                .withQuery(SQLiteQuery.builder().where("`id` = ?", 1).build())
                .executeBlocking();
        assertNull(table);
        TestTable newTable = new TestTable();
        newTable.testSerializable = new TestSerializable("test");
        newTable.testString = "123";
        newTable.testBoolean = true;
        newTable.testList = Arrays.asList("1", "2", "3");
        operator.save(newTable).executeBlocking();
        table = operator
                .getSingle()
                .withQuery(SQLiteQuery.builder().where("`id` = ?", 1).build())
                .executeBlocking();
        assertNotNull(table);
        assertEquals(table.id, 1);
        assertEquals(table.testString, "123");
        assertEquals(table.testSerializable.testField, "test");
        assertEquals(Arrays.toString(newTable.testList.toArray()),
                Arrays.toString(new String[]{"1", "2", "3"}));
    }

    @Test
    public void testSaveAndGetListByRawQueryBlocking() throws Exception {
        SQLiteOperator<TestTable> operator = SQLiteOperator.from(getContext(), TestTable.class);
        List<TestTable> list = operator
                .getList()
                .withRawQuery("SELECT * FROM testTable")
                .executeBlocking();
        assertNotNull(list);
        assertEquals(0, list.size());
        TestTable newTable = new TestTable();
        newTable.testSerializable = new TestSerializable("test");
        newTable.testString = "123";
        newTable.testBoolean = true;
        newTable.testList = Arrays.asList("1", "2", "3");
        operator.save(newTable).executeBlocking();
        list = operator
                .getList()
                .withRawQuery("SELECT * FROM testTable")
                .executeBlocking();
        assertEquals(1, list.size());
        newTable.id = 0;
        operator.save(newTable).executeBlocking();
        list = operator
                .getList()
                .withRawQuery("SELECT * FROM testTable")
                .executeBlocking();
        assertEquals(2, list.size());
    }

    @Test
    public void testSaveAndGetListByQueryBuilderBlocking() throws Exception {
        SQLiteOperator<TestTable> operator = SQLiteOperator.from(getContext(), TestTable.class);
        List<TestTable> list = operator
                .getList()
                .withQuery(SQLiteQuery.builder().where("`testFieldName` = ?", "123").build())
                .executeBlocking();
        assertNotNull(list);
        assertEquals(0, list.size());
        TestTable newTable = new TestTable();
        newTable.testSerializable = new TestSerializable("test");
        newTable.testString = "123";
        newTable.testBoolean = true;
        newTable.testList = Arrays.asList("1", "2", "3");
        operator.save(newTable).executeBlocking();
        list = operator
                .getList()
                .withQuery(SQLiteQuery.builder().where("`testFieldName` = ?", "123").build())
                .executeBlocking();
        assertEquals(1, list.size());
        newTable.testString = "1234";
        operator.save(newTable).executeBlocking();
        list = operator
                .getList()
                .withQuery(SQLiteQuery.builder().where("`testFieldName` = ?", "123").build())
                .executeBlocking();
        assertEquals(0, list.size());
    }

    @Test
    public void testSaveAndGetFullListBlocking() throws Exception {
        SQLiteOperator<TestTable> operator = SQLiteOperator.from(getContext(), TestTable.class);
        List<TestTable> list = operator.getList().executeBlocking();
        assertNotNull(list);
        assertEquals(0, list.size());
        TestTable newTable = new TestTable();
        newTable.testSerializable = new TestSerializable("test");
        newTable.testString = "123";
        newTable.testBoolean = true;
        newTable.testList = Arrays.asList("1", "2", "3");
        operator.save(newTable).executeBlocking();
        list = operator.getList().executeBlocking();
        assertEquals(1, list.size());
        newTable.id = 0;
        operator.save(newTable).executeBlocking();
        list = operator.getList().executeBlocking();
        assertEquals(2, list.size());
    }

    @Test(expected = SQLiteException.class)
    public void testAutoCreateTableDisabled() throws Exception {
        SQLiteOperator.from(getContext(), TestTable3.class)
                .save(new TestTable3())
                .executeBlocking();
    }

    @Test
    public void testPrimaryKeyAsString() throws Exception {
        SQLiteOperator<TestTable5> operator = SQLiteOperator.from(getContext(), TestTable5.class);
        operator.save(new TestTable5("test")).executeBlocking();
        assertNotNull(operator.getSingle("test").executeBlocking());
    }

    @Test(expected = RuntimeException.class)
    public void testSaveWithNonSerializableFields() throws Exception {
        final TestTable2 table2 = new TestTable2();
        table2.nonSerializable = new TestNonSerializable();
        SQLiteOperator.from(getContext(), TestTable2.class).save(table2).executeBlocking();
    }

    @Test
    public void testSave() throws Exception {
        SQLiteOperator<TestTable> operator = SQLiteOperator.from(getContext(), TestTable.class);
        final TestTable table = new TestTable();
        table.testString = "123";
        table.testList = Arrays.asList("1", "2", "3");
        table.testBoolean = true;
        table.testSerializable = new TestSerializable("test");
        assertEquals(0, table.id);
        operator.save(table).executeBlocking();
        assertEquals(1, table.id);
        table.id = 0;
        operator.save(table).executeBlocking();
        assertEquals(2, table.id);
        operator.save(table).executeBlocking();
        assertEquals(2, table.id);
    }

    @Test
    public void testUpdateAndGetSingleById() throws Exception {
        SQLiteOperator<TestTable> operator = SQLiteOperator.from(getContext(), TestTable.class);
        final TestTable table = new TestTable();
        table.testString = "123";
        table.testList = Arrays.asList("1", "2", "3");
        table.testBoolean = true;
        table.testSerializable = new TestSerializable("test");
        operator.save(table).executeBlocking();
        table.testString = "testString";
        table.testBoolean = false;
        assertEquals(1, operator.save(table).executeBlocking());
        TestTable fetched = operator.getSingle(1).executeBlocking();
        assertNotNull(fetched);
        assertEquals(fetched.testString, table.testString);
        assertEquals(fetched.testBoolean, table.testBoolean);
    }

    @Test
    public void testDeleteAndGetSingleById() throws Exception {
        SQLiteOperator<TestTable> operator = SQLiteOperator.from(getContext(), TestTable.class);
        TestTable table = new TestTable();
        operator.save(table).executeBlocking();
        table = operator.getSingle(1).executeBlocking();
        assertNotNull(table);
        assertEquals(1, operator.delete(table).executeBlocking());
        table = operator.getSingle(1).executeBlocking();
        assertNull(table);
    }

    @Test(expected = SQLiteConstraintException.class)
    public void testFailingForeignKeyConstraint() {
        SQLiteOperator<TestTable4> operator = SQLiteOperator.from(getContext(), TestTable4.class);
        operator.save(new TestTable4()).executeBlocking();
    }

    @Test(expected = SQLiteConstraintException.class)
    public void testFailingUniqueConstraint() {
        SQLiteOperator<TestTable4> operator = SQLiteOperator.from(getContext(), TestTable4.class);
        TestTable4 t1 = new TestTable4();
        t1.uniqueField = "notUnique";
        operator.save(t1).executeBlocking();
        TestTable4 t2 = new TestTable4();
        t2.uniqueField = "notUnique";
        operator.save(t2).executeBlocking();
    }

    @Test
    public void testRelationship() {
        SQLiteOperator<TestTable> operator = SQLiteOperator.from(getContext(), TestTable.class);
        TestTable t1 = new TestTable();
        operator.save(t1).executeBlocking();
        SQLiteOperator<TestTable4> operator2 = SQLiteOperator.from(getContext(), TestTable4.class);
        t1 = operator.getSingle(1).executeBlocking();
        assertNotNull(t1);
        assertEquals(0, t1.table4Relation.size());
        TestTable4 related1 = new TestTable4();
        related1.foreignKey = t1.id;
        TestTable4 related2 = new TestTable4();
        related2.foreignKey = t1.id;
        operator2.save(related1, related2).executeBlocking();
        t1 = operator.getSingle(1).executeBlocking();
        assertNotNull(t1);
        assertEquals(2, t1.table4Relation.size());
    }

    @Test
    public void testRespectedForeignKeyConstraintAndCascade() {
        SQLiteOperator<TestTable> operator = SQLiteOperator.from(getContext(), TestTable.class);
        TestTable testTable = new TestTable();
        operator.save(testTable).executeBlocking();
        SQLiteOperator<TestTable4> operator2 = SQLiteOperator.from(getContext(), TestTable4.class);
        TestTable4 testTable4 = new TestTable4();
        testTable4.foreignKey = 1;
        operator2.save(testTable4).executeBlocking();
        assertNotNull(operator2.getSingle(1).executeBlocking());
        operator.delete(testTable).executeBlocking();
        assertNull(operator.getSingle(1).executeBlocking());
        assertNull(operator2.getSingle(1).executeBlocking());
    }

    @Test
    public void testOnUpgradeWithAddAndDeleteColumnAndValuePersistence() throws Exception {
        SQLiteOperator<TestTable> operator = SQLiteOperator.from(getContext(), TestTable.class);
        operator.save(new TestTable()).executeBlocking();
        Cursor testTableCursor = getHelperInstance()
                .getReadableDatabase()
                .rawQuery("PRAGMA table_info(testTable)", null);
        final List<String> testTableCols = new ArrayList<>();

        if (testTableCursor.moveToFirst()) {
            do {
                testTableCols.add(testTableCursor.getString(1));
            } while (testTableCursor.moveToNext());
        }
        testTableCursor.close();

        final String upgradeAddColName = "upgradeAddTester",
                upgradeDeleteColName = "upgradeDeleteTester";

        assertTrue(!testTableCols.contains(upgradeAddColName));
        assertTrue(testTableCols.contains(upgradeDeleteColName));

        getHelperInstance().onUpgrade(getHelperInstance().getWritableDatabase(), 1, 2);
        testTableCursor = getHelperInstance()
                .getReadableDatabase()
                .rawQuery("PRAGMA table_info(testTable)", null);
        testTableCols.clear();

        if (testTableCursor.moveToFirst()) {
            do {
                testTableCols.add(testTableCursor.getString(1));
            } while (testTableCursor.moveToNext());
        }
        testTableCursor.close();

        assertTrue(testTableCols.contains(upgradeAddColName));
        assertTrue(!testTableCols.contains(upgradeDeleteColName));
        assertNotNull(operator.getSingle(1).executeBlocking());
    }
}