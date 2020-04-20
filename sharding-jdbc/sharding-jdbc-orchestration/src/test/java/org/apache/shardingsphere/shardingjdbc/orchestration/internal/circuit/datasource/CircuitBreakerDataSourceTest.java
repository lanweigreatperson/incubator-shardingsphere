/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.shardingjdbc.orchestration.internal.circuit.datasource;

import org.apache.shardingsphere.shardingjdbc.orchestration.internal.circuit.connection.CircuitBreakerConnection;
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public final class CircuitBreakerDataSourceTest {
    
    private final CircuitBreakerDataSource dataSource = new CircuitBreakerDataSource();
    
    @Test
    public void assertClose() {
        dataSource.close();
    }
    
    @Test
    public void assertGetConnection() {
        assertTrue(dataSource.getConnection() instanceof CircuitBreakerConnection);
        assertTrue(dataSource.getConnection("", "") instanceof CircuitBreakerConnection);
    }
    
    @Test
    public void assertGetLogWriter() {
        assertNull(dataSource.getLogWriter());
    }
    
    @Test
    public void assertSetLogWriter() {
        dataSource.setLogWriter(null);
        assertNull(dataSource.getLogWriter());
    }
    
    @Test
    public void assertGetParentLogger() {
        assertNull(dataSource.getParentLogger());
    }
}
