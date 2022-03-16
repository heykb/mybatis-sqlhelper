package io.github.heykb.sqlhelper.interceptor;

import io.github.heykb.sqlhelper.utils.CommonUtils;
import org.apache.ibatis.cursor.Cursor;

import java.util.*;
import java.util.function.Consumer;

public class ColumnFilterCursor<T> extends SimpleProxyCursor<T> {

    private Set<String> ignoreColumns;
    private boolean isMapUnderscoreToCamelCase;


    public ColumnFilterCursor(Cursor target, Set<String> ignoreColumns, boolean isMapUnderscoreToCamelCase) {
        super(target);
        this.ignoreColumns = ignoreColumns;
        this.isMapUnderscoreToCamelCase = isMapUnderscoreToCamelCase;
    }

    @Override
    public Iterator iterator() {
        return new ColumnFilterIterator(super.iterator());
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        Objects.requireNonNull(action);
        for (T t : this) {
            action.accept(t);
        }
    }

    @Override
    public Spliterator<T> spliterator() {
        return Spliterators.spliteratorUnknownSize(iterator(), 0);
    }


    class ColumnFilterIterator implements Iterator {
        private Iterator target;

        public ColumnFilterIterator(Iterator target) {
            this.target = target;
        }

        @Override
        public Object next() {
            Object re = target.next();
            CommonUtils.filterColumns(re, ignoreColumns, isMapUnderscoreToCamelCase);
            return re;
        }

        public Iterator getTarget() {
            return target;
        }

        @Override
        public boolean hasNext() {
            return target.hasNext();
        }

    }
}
