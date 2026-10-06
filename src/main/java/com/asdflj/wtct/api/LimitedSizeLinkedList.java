package com.asdflj.wtct.api;

import java.util.LinkedList;

public class LimitedSizeLinkedList<E> extends LinkedList<E> {

    private final int limit;

    public LimitedSizeLinkedList() {
        super();
        this.limit = WtctAPI.CRAFTING_HISTORY_SIZE;
    }

    public LimitedSizeLinkedList(int limit) {
        super();
        this.limit = limit;
    }

    @Override
    public boolean add(E e) {
        if (size() >= limit) {
            removeFirst();
        }
        return super.add(e);
    }

    @Override
    public void push(E e) {
        if (size() >= limit) {
            removeLast();
        }
        super.push(e);
    }
}
