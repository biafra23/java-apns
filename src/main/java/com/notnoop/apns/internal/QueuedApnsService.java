/*
 * Copyright 2009, Mahmood Ali.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *   * Neither the name of Mahmood Ali. nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.notnoop.apns.internal;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.notnoop.apns.ApnsNotification;
import com.notnoop.apns.ApnsService;
import com.notnoop.exceptions.NetworkIOException;

public class QueuedApnsService extends AbstractApnsService {

    private ApnsService service;
    private BlockingQueue<ApnsNotification> queue;
    private volatile boolean started = false;

    public QueuedApnsService(ApnsService service) {
        super(null);
        this.service = service;
        this.queue = new LinkedBlockingQueue<ApnsNotification>();
        this.thread = null;
    }

    @Override
    public void push(ApnsNotification msg) {
        if (!started)
            throw new IllegalStateException("service hasn't be started or was closed");
        queue.add(msg);
    }

    private Thread thread;
    private volatile boolean shouldContinue;

    public void start() {
        started = true;
        service.start();
        if (thread != null)
            stop();
        shouldContinue = true;
        thread = new Thread() {
            public void run() {
                while (shouldContinue) {
                    try {
                        ApnsNotification msg = queue.take();
                        service.push(msg);
                    } catch (InterruptedException e) {
                    }
                }
            }
        };
        thread.start();
    }

    public void stop() {
        started = false;
        shouldContinue = false;
        thread.interrupt();
        service.stop();
    }

    @Override
    public Map<String, Date> getInactiveDevices() throws NetworkIOException {
        return service.getInactiveDevices();
    }

    public void testConnection() throws NetworkIOException {
        service.testConnection();
    }

}
