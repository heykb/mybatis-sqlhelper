package io.github.heykb.sqlhelper.test;

import com.alibaba.druid.sql.SQLUtils;
import io.github.heykb.sqlhelper.handler.InjectColumnInfoHandler;
import io.github.heykb.sqlhelper.helper.SqlStatementEditor;
import org.junit.After;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;

public class CompareSqlTest {

    static FileWriter fileWriter;

    static Stream<Arguments> parse() throws Exception {
        return BaseUtils.parse("io/github/heykb/sqlhelper/test/testSql.xml").stream()
                .map(item-> Arguments.of(Named.of(item.name(),item)));
    }

    @BeforeAll
    static void start() throws IOException {
        fileWriter = new FileWriter("../sql-demo.md");
    }

    @AfterAll
    static void end() throws IOException {
        fileWriter.close();
    }

    @ParameterizedTest
    @MethodSource("parse")
    void test(SqlTest sqlTest) throws IOException {
        InjectColumnInfoHandler injectColumnInfoHandler = sqlTest.injectColumnInfoHandler();
        SqlStatementEditor sqlStatementEditor =
                new SqlStatementEditor.Builder(sqlTest.origin(), sqlTest.db())
                        .injectColumnInfoHandlers(Arrays.asList(injectColumnInfoHandler))
                        .build();
        SqlStatementEditor.Result result = sqlStatementEditor.processing();
        System.out.println(sqlTest.target().trim());
        StringBuilder sb = new StringBuilder("```sql\n");
        String title = String.format("-- [%s] [%s] [columnName=%s] [op=\"%s\"] [value=%s]",sqlTest.db(),sqlTest.name(),injectColumnInfoHandler.getColumnName(),injectColumnInfoHandler.op(),injectColumnInfoHandler.getValue());
        sb.append(title).append("\n");
        sb.append(SQLUtils.parseSingleStatement(sqlTest.origin(),sqlTest.db()).toString()).append("\n");
        if(result == null){
            System.out.println("不会修改");
            Assertions.assertTrue(sqlTest.target().trim().equals("null"));
        }else{
            String resultStr = "\n-- ⇊\n\n"+result.getSql();
            sb.append(resultStr).append("\n");
            String exceptResult = SQLUtils.parseSingleStatement(sqlTest.target(),sqlTest.db()).toString();
            Assertions.assertEquals(result.getSql(),exceptResult);
        }
        sb.append("```\n");
        fileWriter.write(sb.toString());
    }
}
