package io.github.heykb.sqlhelper.helper;

import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.visitor.SchemaStatVisitor;

import java.util.HashMap;
import java.util.Map;

public class MySchemaStatVisitor extends SchemaStatVisitor {
    private Map<String,String> aliasTableMap = new HashMap<>();
    @Override
    public void endVisit(SQLExprTableSource x) {
        super.endVisit(x);
        String alias = x.computeAlias();
        if(alias!=null){
            aliasTableMap.put(alias,x.getTableName());
        }

    }
    public Map<String, String> getAliasTableMap() {
        return aliasTableMap;
    }
}
