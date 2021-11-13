package io.github.heykb.sqlhelper.handler.abstractor;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import io.github.heykb.sqlhelper.handler.ConditionInjectInfo;
import io.github.heykb.sqlhelper.helper.Configuration;
import io.github.heykb.sqlhelper.utils.CommonUtils;

/**
 * 复杂条件注入
 *
 * @author heykb
 */
public abstract class BinaryConditionInjectInfoHandler implements ConditionInjectInfo {


    /**
     * Gets left condition inject info.
     *
     * @return the left condition inject info
     */
    abstract public ConditionInjectInfo getLeftConditionInjectInfo();

    /**
     * Gets right condition inject info.
     *
     * @return the right condition inject info
     */
    abstract public ConditionInjectInfo getRightConditionInjectInfo();

    @Override
    public String op() {
        return "and";
    }

    @Override
    public SQLExpr toConditionSQLExpr(String tableAlias,DbType dbType, Configuration configuration){
        SQLExpr left = null;
        SQLExpr right = null;
        if(getLeftConditionInjectInfo()!=null){
            left = getLeftConditionInjectInfo().toConditionSQLExpr(tableAlias,dbType,configuration);
        }
        if(getRightConditionInjectInfo()!=null){
            right = getRightConditionInjectInfo().toConditionSQLExpr(tableAlias,dbType,configuration);
        }
        if(left != null && right !=null){
            return new SQLBinaryOpExpr(left, right, CommonUtils.convert(op()));
        }else if(left == null){
            return right;
        }else if(right == null){
            return left;
        }
        return null;
    }
}
