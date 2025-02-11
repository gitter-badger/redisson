/**
 * Copyright 2014 Nikita Koksharov, Nickolay Borbit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.redisson.connection;

import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.redisson.MasterSlaveServersConfig;
import org.redisson.client.RedisConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

public class IdleConnectionWatcher {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public static class Entry {

        private final int minimumAmount;
        private final int maximumAmount;
        private final AtomicInteger freeConnectionsCounter;
        private final Collection<? extends RedisConnection> connections;

        public Entry(int minimumAmount, int maximumAmount, Collection<? extends RedisConnection> connections, AtomicInteger freeConnectionsCounter) {
            super();
            this.minimumAmount = minimumAmount;
            this.maximumAmount = maximumAmount;
            this.connections = connections;
            this.freeConnectionsCounter = freeConnectionsCounter;
        }

    };

    private final Queue<Entry> entries = new ConcurrentLinkedQueue<Entry>();

    public IdleConnectionWatcher(final ConnectionManager manager, final MasterSlaveServersConfig config) {
        manager.getGroup().scheduleWithFixedDelay(new Runnable() {

            @Override
            public void run() {
                for (Entry entry : entries) {
                    if (!validateAmount(entry)) {
                        continue;
                    }

                    for (final RedisConnection c : entry.connections) {
                        final long timeInPool = System.currentTimeMillis() - c.getLastUsageTime();
                        if (timeInPool > config.getIdleConnectionTimeout()
                                && validateAmount(entry) && entry.connections.remove(c)) {
                            ChannelFuture future = c.closeAsync();
                            future.addListener(new FutureListener<Void>() {
                                @Override
                                public void operationComplete(Future<Void> future) throws Exception {
                                    log.debug("Connection {} has been closed due to idle timeout. Not used for {} ms", c.getChannel(), timeInPool);
                                }
                            });
                        }
                    }
                }
            }

        }, config.getIdleConnectionTimeout(), config.getIdleConnectionTimeout(), TimeUnit.MILLISECONDS);
    }

    private boolean validateAmount(Entry entry) {
        return entry.maximumAmount - entry.freeConnectionsCounter.get() + entry.connections.size() >= entry.minimumAmount;
    }

    public void add(int minimumAmount, int maximumAmount, Collection<? extends RedisConnection> connections, AtomicInteger freeConnectionsCounter) {
        entries.add(new Entry(minimumAmount, maximumAmount, connections, freeConnectionsCounter));
    }

}
