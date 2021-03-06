/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.nuxeo.ecm.core.redis.embedded;

import java.util.Random;

import redis.clients.jedis.exceptions.JedisConnectionException;

public abstract class RedisEmbeddedGuessConnectionError {

    protected final Thread ownerThread = Thread.currentThread();

    public static class NoError extends RedisEmbeddedGuessConnectionError {
        @Override
        protected void doGuessError() throws JedisConnectionException {
            return;
        }
    }

    public static class OnFirstCall extends RedisEmbeddedGuessConnectionError {

        protected boolean fired;

        @Override
        protected void doGuessError() throws JedisConnectionException {
            if (!fired) {
                fired = true;
                throw new JedisConnectionException("first call error");
            }
        }

    }

    public static class OnEveryCall extends RedisEmbeddedGuessConnectionError {

        @Override
        protected void doGuessError() throws JedisConnectionException {
            throw new JedisConnectionException("every call error");
        }

    }

    public static class OnRandomCall extends RedisEmbeddedGuessConnectionError {

        protected final Random random = new Random();

        @Override
        protected void doGuessError() throws JedisConnectionException {
            if (random.nextBoolean()) {
                throw new JedisConnectionException("random call error");
            }
        }

    }

    protected void guessError() {
        if (!onThread()) {
            return;
        }
        doGuessError();
    }

    protected abstract void doGuessError();

    protected boolean onThread() {
        return Thread.currentThread().equals(ownerThread);
    }
}
