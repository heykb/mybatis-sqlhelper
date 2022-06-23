package io.github.heykb.sqlhelper.primary;

import io.github.heykb.sqlhelper.autoconfigure.SqlHelperAutoConfiguration;
import io.github.heykb.sqlhelper.autoconfigure.SqlHelperProperties;
import io.github.heykb.sqlhelper.handler.abstractor.LogicDeleteInfoHandler;
import io.github.heykb.sqlhelper.interceptor.SqlHelperPlugin;
import io.github.heykb.sqlhelper.spring.PropertyLogicDeleteInfoHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

public class AutoConfigurationTests {

    private AnnotationConfigApplicationContext context;
    @BeforeEach
    void init() {
        this.context = new AnnotationConfigApplicationContext();

    }
    @AfterEach
    void closeContext() {
        if (this.context != null) {
            this.context.close();
        }
    }

    @Test
    void testDefaultProp(){
        this.context.register(SqlHelperAutoConfiguration.class);
        this.context.refresh();
        SqlHelperProperties sqlHelperProperties = this.context.getBean(SqlHelperProperties.class);
        assertEquals(sqlHelperProperties.getProperties().getProperty(SqlHelperPlugin.enableProp),"true");
        assertEquals(sqlHelperProperties.getProperties().getProperty(SqlHelperPlugin.logicDeleteEnableProp),"false");
        assertEquals(sqlHelperProperties.getProperties().getProperty(SqlHelperPlugin.multiTenantEnableProp),"true");
        assertNull(sqlHelperProperties.getProperties().getProperty(SqlHelperPlugin.dbTypeProp));
        assertNull(sqlHelperProperties.getProperties().getProperty(SqlHelperPlugin.injectColumnInfoHandlersProp));
        assertNull(sqlHelperProperties.getProperties().getProperty(SqlHelperPlugin.columnFilterInfoHandlersProp));
        assertNull(sqlHelperProperties.getProperties().getProperty(SqlHelperPlugin.dynamicFindColumnFilterHandlerProp));
        assertNull(sqlHelperProperties.getProperties().getProperty(SqlHelperPlugin.dynamicFindInjectInfoHandlerProp));
    }

    @Test
    void testPropertyLogicDeleteInfoHandler(){
        this.context.register(SqlHelperAutoConfiguration.class);
        this.context.refresh();
        LogicDeleteInfoHandler logicDeleteInfoHandler = this.context.getBean(LogicDeleteInfoHandler.class);
        assertTrue(logicDeleteInfoHandler instanceof PropertyLogicDeleteInfoHandler);
        assertEquals(logicDeleteInfoHandler.getNotDeletedValue(),"'N'");
        assertEquals(logicDeleteInfoHandler.getDeleteSqlDemo(),"UPDATE a SET is_deleted = 'Y'");
        assertEquals(logicDeleteInfoHandler.getColumnName(),"is_deleted");
    }

    @Test
    void testSqlHelperPlugin(){
        this.context.register(SqlHelperAutoConfiguration.class);
        this.context.refresh();
        SqlHelperPlugin sqlHelperPlugin = this.context.getBean(SqlHelperPlugin.class);
        LogicDeleteInfoHandler logicDeleteInfoHandler = this.context.getBean(LogicDeleteInfoHandler.class);
        assertEquals(sqlHelperPlugin.getInjectColumnInfoHandlers().size(),1);
        assertEquals(logicDeleteInfoHandler,sqlHelperPlugin.getInjectColumnInfoHandlers().iterator().next());

    }
}
