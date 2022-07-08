package io.github.heykb.sqlhelper.test;

import com.alibaba.druid.sql.SQLUtils;
import io.github.heykb.sqlhelper.helper.SqlStatementEditor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.stream.Stream;

public class CompareSqlTest {

    static Stream<Arguments> parse() throws Exception {
        return BaseUtils.parse("io/github/heykb/sqlhelper/test/testSql.xml").stream()
                .map(item-> Arguments.of(Named.of(item.name(),item)));
    }

    @ParameterizedTest
    @MethodSource("parse")
    void test(SqlTest sqlTest){
        SqlStatementEditor sqlStatementEditor =
                new SqlStatementEditor.Builder(sqlTest.origin(), sqlTest.db())
                        .injectColumnInfoHandlers(Arrays.asList(sqlTest.injectColumnInfoHandler()))
                        .build();
        SqlStatementEditor.Result result = sqlStatementEditor.processing();
        if(result == null){
            Assertions.assertTrue(sqlTest.target().trim().equals("null"));
        }else{
            System.out.println(result.getSql());
            String exceptResult = SQLUtils.parseSingleStatement(sqlTest.target(),sqlTest.db()).toString();
            Assertions.assertEquals(result.getSql(),exceptResult);
        }
    }
}
