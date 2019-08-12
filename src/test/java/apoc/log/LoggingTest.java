package apoc.log;

import apoc.util.TestUtil;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.ClassRule;
import org.junit.Test;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.exceptions.KernelException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.internal.helpers.collection.Pair;
import org.neo4j.logging.AssertableLogProvider;
import org.neo4j.logging.AssertableLogProvider.LogMatcherBuilder;
import org.neo4j.test.rule.DbmsRule;
import org.neo4j.test.rule.ImpermanentDbmsRule;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

public class LoggingTest {

    public static AssertableLogProvider logProvider = new AssertableLogProvider();

    @ClassRule
    public static DbmsRule db = new ImpermanentDbmsRule(logProvider);

    @Test
    public void shouldWriteSafeStrings() {
        // given
        Logging logging = new Logging(Logging.LoggingType.safe, 10000L, 10);

        // when
        String stringWithWhitespaces = logging.format("Test %s ", asList(1));
        String stringWithWhitespacesAndTabs = logging.format("Test %s \t", asList(1));
        String stringWithWhitespacesAndTabsAndNewLine = logging.format("Test %s \t\nNewLine", asList(1));

        // then
        assertEquals("test_1_", stringWithWhitespaces);
        assertEquals("test_1__", stringWithWhitespacesAndTabs);
        assertEquals("test_1__\nnewline", stringWithWhitespacesAndTabsAndNewLine);
    }

    @Test
    public void shouldWriteRawStrings() {
        // given
        Logging logging = new Logging(Logging.LoggingType.raw, 10000L, 10);

        // when
        String stringWithWhitespaces = logging.format("Test %s ", asList(1));
        String stringWithWhitespacesAndTabs = logging.format("Test %s \t", asList(1));
        String stringWithWhitespacesAndTabsAndNewLine = logging.format("Test %s \t\nNewLine", asList(1));

        // then
        assertEquals("Test 1 ", stringWithWhitespaces);
        assertEquals("Test 1 \t", stringWithWhitespacesAndTabs);
        assertEquals("Test 1 \t\nNewLine", stringWithWhitespacesAndTabsAndNewLine);
    }

    @Test
    public void shouldNotLog() {
        // given
        Logging logging = new Logging(Logging.LoggingType.none, 10000L, 10);

        // when
        String stringWithWhitespaces = logging.format("Test %s ", asList(1));
        String stringWithWhitespacesAndTabs = logging.format("Test %s \t", asList(1));
        String stringWithWhitespacesAndTabsAndNewLine = logging.format("Test %s \t\nNewLine", asList(1));

        // then
        assertNull(stringWithWhitespaces);
        assertNull(stringWithWhitespacesAndTabs);
        assertNull(stringWithWhitespacesAndTabsAndNewLine);
    }

    @Test
    public void shouldSkipMessagesFor10Seconds() {
        // given
        Logging logging = new Logging(Logging.LoggingType.safe, 10000L, 10);

        List<String> all = IntStream.range(0, 10).mapToObj(i -> "test_" + i + "_")
                .collect(Collectors.toList());

        all.addAll(IntStream.range(50, 60).mapToObj(i -> "test_" + i + "_")
                .collect(Collectors.toList()));

        // when
        IntStream.range(0, 100).forEach(i -> {
            if (i < 10) {
                // when
                String msg = logging.format("Test %s ", asList(i));

                // then
                assertEquals("test_" + i + "_", msg);
                all.remove(msg);
                return;
            }
            if (i >= 10 && i < 50) {
                // when
                String msg = logging.format("Test %s ", asList(i));

                // then
                assertNull(msg);
                return;
            }
            if (i == 50) {
                try {
                    Thread.sleep(11_000L); // wait for a new time window
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (i >= 50 && i < 60) {
                // when
                String msg = logging.format("Test %s ", asList(i));

                // then
                assertEquals("test_" + i + "_", msg);
                all.remove(msg);
                return;
            }
            if (i >= 60) {
                // when
                String msg = logging.format("Test %s ", asList(i));

                // then
                assertNull(msg);
            }
        });

        // then
        assertTrue(all.isEmpty());
    }

    @Test
    public void shouldWrite20MessagesIn1Second() {
        // given
        Logging logging = new Logging(Logging.LoggingType.safe, 1000L, 20);

        List<String> all = IntStream.range(0, 20).mapToObj(i -> "test_" + i + "_")
                .collect(Collectors.toList());

        all.addAll(IntStream.range(50, 70).mapToObj(i -> "test_" + i + "_")
                .collect(Collectors.toList()));

        // when
        IntStream.range(0, 100).forEach(i -> {
            if (i < 20) {
                // when
                String msg = logging.format("Test %s ", asList(i));

                // then
                assertEquals("test_" + i + "_", msg);
                all.remove(msg);
                return;
            }
            if (i >= 20 && i < 50) {
                // when
                String msg = logging.format("Test %s ", asList(i));

                // then
                assertNull(msg); // then
                return;
            }
            if (i == 50) {
                try {
                    Thread.sleep(2_000L); // wait for a new time window
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (i >= 50 && i < 70) {
                // when
                String msg = logging.format("Test %s ", asList(i));

                // then
                assertEquals("test_" + i + "_", msg);
                all.remove(msg);
                return;
            }
            if (i >= 70) {
                // when
                String msg = logging.format("Test %s ", asList(i));

                // then
                assertNull(msg);
            }
        });

        // then
        assertTrue(all.isEmpty());
    }

    @Test
    public void shouldCallTheProcedure() throws KernelException {
        // given
        TestUtil.registerProcedure(db, Logging.class);

        // when
        db.execute("CALL apoc.log.warn('Prova %s', [1])");

        // then
        logProvider.print(System.out);
//        logProvider.assertExactly(new LogMatcherBuilder(Matchers.equalTo("org.neo4j.kernel.api.procedure.GlobalProcedures")).warn("prova_"));
    }
}
