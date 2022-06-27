package io.github.heykb.sqlhelper;

import com.alibaba.druid.DbType;
import io.github.heykb.sqlhelper.condition.SqlTest;
import io.github.heykb.sqlhelper.handler.ColumnFilterInfoHandler;
import io.github.heykb.sqlhelper.handler.InjectColumnInfoHandler;
import io.github.heykb.sqlhelper.interceptor.SqlHelperPlugin;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class BaseUtils {

    public static List<SqlTest> parse(String resource) throws Exception {
        List<SqlTest> re = new ArrayList<>();
        try(Reader reader = Resources.getResourceAsReader(resource)){
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(reader));
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            NodeList nodes = (NodeList)xpath.evaluate("test",xpath.evaluate("/tests",document, XPathConstants.NODE), XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                Node origin = (Node) xpath.evaluate("origin",node,XPathConstants.NODE);
                Node target = (Node) xpath.evaluate("target",node,XPathConstants.NODE);
                re.add(new SqlTest() {
                    @Override
                    public String origin() {
                        return origin.getTextContent();
                    }

                    @Override
                    public String target() {
                        return target.getTextContent();
                    }

                    @Override
                    public String name() {
                        return node.getAttributes().getNamedItem("name").getNodeValue();
                    }

                    @Override
                    public DbType db() {
                        return DbType.of(node.getAttributes().getNamedItem("db").getNodeValue());
                    }

                    @Override
                    public InjectColumnInfoHandler injectColumnInfoHandler() {
                        return new InjectColumnInfoHandler() {
                            @Override
                            public String getColumnName() {
                                return node.getAttributes().getNamedItem("column").getNodeValue();
                            }

                            @Override
                            public String getValue() {
                                return node.getAttributes().getNamedItem("value").getNodeValue();
                            }

                            @Override
                            public int getInjectTypes() {
                                String type = node.getAttributes().getNamedItem("type").getNodeValue().toLowerCase();
                                int re = 0;
                                if(type.contains("condition")){
                                    re|=CONDITION;
                                }
                                if(type.contains("insert")){
                                    re|=INSERT;
                                }
                                if(type.contains("update")){
                                    re|=UPDATE;
                                }
                                return re;
                            }
                        };
                    }
                });
            }
        }
        return re;
    }

    public static SqlSessionFactory generateSqlSessionFactory(DataSource dataSource, String dbScriptResource, Class<?> MapperType, List<InjectColumnInfoHandler> injectColumnInfoHandlers, List<ColumnFilterInfoHandler> columnFilterInfoHandlers) throws Exception {
        BaseUtils.runScript(dataSource,dbScriptResource);
        Environment environment = new Environment("test", new JdbcTransactionFactory(), dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.addMapper(MapperType);
        SqlHelperPlugin sqlHelperPlugin = new SqlHelperPlugin();
        sqlHelperPlugin.setInjectColumnInfoHandlers(injectColumnInfoHandlers);
        sqlHelperPlugin.setColumnFilterInfoHandlers(columnFilterInfoHandlers);
        configuration.addInterceptor(sqlHelperPlugin);
        return new SqlSessionFactoryBuilder().build(configuration);
    }


    public static void runScript(DataSource ds, String resource) throws IOException, SQLException {
        try (Connection connection = ds.getConnection()) {
            ScriptRunner runner = new ScriptRunner(connection);
            runner.setAutoCommit(true);
            runner.setStopOnError(true);
            runner.setLogWriter(null);
            runner.setErrorLogWriter(null);
            runScript(runner, resource);
        }
    }
    public static void runScript(ScriptRunner runner, String resource) throws IOException, SQLException {
        try (Reader reader = Resources.getResourceAsReader(resource)) {
            runner.runScript(reader);
        }
    }

}
