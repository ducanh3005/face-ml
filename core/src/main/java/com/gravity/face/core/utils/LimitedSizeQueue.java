package com.gravity.face.core.utils;

import java.util.LinkedList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class LimitedSizeQueue<E> {

    private final int capacity;
    private final LinkedList<E> queue;
    private final ReadWriteLock lock;

    public LimitedSizeQueue(int capacity) {
        this.capacity = capacity;
        this.queue = new LinkedList<>();
        this.lock = new ReentrantReadWriteLock();
    }

    public boolean add(E e) {
        lock.writeLock().lock();
        try {
            if (this.queue.size() >= capacity) {
                this.queue.removeFirst();
            }
            return this.queue.add(e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public E poll() {
        lock.readLock().lock();
        try {
            return this.queue.poll();
        } finally {
            lock.readLock().unlock();
        }
    }
}
