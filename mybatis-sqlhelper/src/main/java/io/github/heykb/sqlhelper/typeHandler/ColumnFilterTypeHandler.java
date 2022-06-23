package io.github.heykb.sqlhelper.typeHandler;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * @author heykb
 */
public class ColumnFilterTypeHandler implements TypeHandler {

    private TypeHandler typeHandler;
    private List<Integer> removeIndex;

    public ColumnFilterTypeHandler(TypeHandler typeHandler, List<Integer> removeIndex) {
        this.typeHandler = typeHandler;
        this.removeIndex = removeIndex;
        Collections.sort(removeIndex,Integer::compareTo);
    }

   /*
   1 2 3 4 5 6
   1    3  4  6
   2 5
    */
    @Override
    public void setParameter(PreparedStatement ps, int i, Object parameter, JdbcType jdbcType) throws SQLException {
        int j = 0;
        for(Integer item:removeIndex){
            if(i < item){
                break;
            }else if(i==item){
                return;
            }else if(i > item){
                ++j;
            }
        }
        typeHandler.setParameter(ps,i-j,parameter,jdbcType);
    }

    @Override
    public Object getResult(ResultSet rs, String columnName) throws SQLException {
        return typeHandler.getResult(rs,columnName);
    }

    @Override
    public Object getResult(ResultSet rs, int columnIndex) throws SQLException {
        return typeHandler.getResult(rs,columnIndex);
    }

    @Override
    public Object getResult(CallableStatement cs, int columnIndex) throws SQLException {
        return typeHandler.getResult(cs,columnIndex);
    }
}
