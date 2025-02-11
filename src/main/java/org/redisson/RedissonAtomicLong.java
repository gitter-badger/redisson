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
package org.redisson;

import java.util.Collections;

import org.redisson.client.codec.StringCodec;
import org.redisson.client.protocol.RedisCommands;
import org.redisson.core.RAtomicLong;

import io.netty.util.concurrent.Future;

/**
 * Distributed alternative to the {@link java.util.concurrent.atomic.AtomicLong}
 *
 * @author Nikita Koksharov
 *
 */
public class RedissonAtomicLong extends RedissonExpirable implements RAtomicLong {

    protected RedissonAtomicLong(CommandExecutor commandExecutor, String name) {
        super(commandExecutor, name);
    }

    @Override
    public long addAndGet(long delta) {
        return get(addAndGetAsync(delta));
    }

    @Override
    public Future<Long> addAndGetAsync(long delta) {
        return commandExecutor.writeAsync(getName(), StringCodec.INSTANCE, RedisCommands.INCRBY, getName(), delta);
    }

    @Override
    public boolean compareAndSet(long expect, long update) {
        return get(compareAndSetAsync(expect, update));
    }

    @Override
    public Future<Boolean> compareAndSetAsync(long expect, long update) {
        return commandExecutor.evalWriteAsync(getName(), StringCodec.INSTANCE, RedisCommands.EVAL_BOOLEAN,
                "if redis.call('get', KEYS[1]) == ARGV[1] then "
                     + "redis.call('set', KEYS[1], ARGV[2]); "
                     + "return true "
                   + "else "
                     + "return false end",
                Collections.<Object>singletonList(getName()), expect, update);
    }

    @Override
    public long decrementAndGet() {
        return get(decrementAndGetAsync());
    }

    @Override
    public Future<Long> decrementAndGetAsync() {
        return commandExecutor.writeAsync(getName(), StringCodec.INSTANCE, RedisCommands.DECR, getName());
    }

    @Override
    public long get() {
        return addAndGet(0);
    }

    @Override
    public Future<Long> getAsync() {
        return addAndGetAsync(0);
    }

    @Override
    public long getAndAdd(long delta) {
        return get(getAndAddAsync(delta));
    }

    @Override
    public Future<Long> getAndAddAsync(long delta) {
        return commandExecutor.evalWriteAsync(getName(),
                StringCodec.INSTANCE, RedisCommands.EVAL_LONG,
                "local v = redis.call('get', KEYS[1]) or 0; "
                + "redis.call('set', KEYS[1], v + ARGV[1]); "
                + "return tonumber(v)",
                Collections.<Object>singletonList(getName()), delta);
    }


    @Override
    public long getAndSet(long newValue) {
        return get(getAndSetAsync(newValue));
    }

    @Override
    public Future<Long> getAndSetAsync(long newValue) {
        return commandExecutor.evalWriteAsync(getName(),
                StringCodec.INSTANCE, RedisCommands.EVAL_LONG,
                "local v = redis.call('get', KEYS[1]) or 0; redis.call('set', KEYS[1], ARGV[1]); return tonumber(v)",
                Collections.<Object>singletonList(getName()), newValue);
    }

    @Override
    public long incrementAndGet() {
        return get(incrementAndGetAsync());
    }

    @Override
    public Future<Long> incrementAndGetAsync() {
        return commandExecutor.writeAsync(getName(), StringCodec.INSTANCE, RedisCommands.INCR, getName());
    }

    @Override
    public long getAndIncrement() {
        return getAndAdd(1);
    }

    @Override
    public Future<Long> getAndIncrementAsync() {
        return getAndAddAsync(1);
    }

    @Override
    public long getAndDecrement() {
        return getAndAdd(-1);
    }

    @Override
    public Future<Long> getAndDecrementAsync() {
        return getAndAddAsync(-1);
    }

    @Override
    public void set(long newValue) {
        get(setAsync(newValue));
    }

    @Override
    public Future<Void> setAsync(long newValue) {
        return commandExecutor.writeAsync(getName(), StringCodec.INSTANCE, RedisCommands.SET, getName(), newValue);
    }

    public String toString() {
        return Long.toString(get());
    }

}
