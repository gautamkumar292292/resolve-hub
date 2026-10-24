package com.qsp.resolvehub.service.impl;

import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReentrantLock;

@Component
public class AssignmentLock {

    private final ReentrantLock lock = new ReentrantLock();

    public void lock() {
        lock.lock();
    }

    public void unlock() {
        lock.unlock();
    }
}