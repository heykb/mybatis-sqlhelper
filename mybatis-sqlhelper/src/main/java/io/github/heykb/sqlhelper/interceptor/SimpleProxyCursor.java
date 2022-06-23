package io.github.heykb.sqlhelper.interceptor;


import org.apache.ibatis.cursor.Cursor;

import java.io.IOException;
import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

public class SimpleProxyCursor<T> implements Cursor<T> {

    private Cursor<T> target;

    public SimpleProxyCursor(Cursor<T> target) {
        this.target = target;
    }

    public Cursor<T> getTarget() {
        return target;
    }

    @Override
    public boolean isOpen() {
        return target.isOpen();
    }

    @Override
    public boolean isConsumed() {
        return target.isConsumed();
    }

    @Override
    public int getCurrentIndex() {
        return target.getCurrentIndex();
    }

    @Override
    public void close() throws IOException {
        target.close();
    }

    @Override
    public Iterator<T> iterator() {
        return target.iterator();
    }

}
