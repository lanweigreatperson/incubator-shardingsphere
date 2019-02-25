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

package org.apache.shardingsphere.shardingproxy.transport.postgresql.packet.command.query.text;

import com.google.common.base.Optional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.shardingproxy.backend.communication.jdbc.connection.BackendConnection;
import org.apache.shardingsphere.shardingproxy.backend.response.BackendResponse;
import org.apache.shardingsphere.shardingproxy.backend.response.error.ErrorResponse;
import org.apache.shardingsphere.shardingproxy.backend.response.query.QueryHeader;
import org.apache.shardingsphere.shardingproxy.backend.response.query.QueryResponse;
import org.apache.shardingsphere.shardingproxy.backend.response.update.UpdateResponse;
import org.apache.shardingsphere.shardingproxy.backend.text.TextProtocolBackendHandler;
import org.apache.shardingsphere.shardingproxy.backend.text.TextProtocolBackendHandlerFactory;
import org.apache.shardingsphere.shardingproxy.runtime.GlobalRegistry;
import org.apache.shardingsphere.shardingproxy.transport.common.packet.DatabasePacket;
import org.apache.shardingsphere.shardingproxy.transport.common.packet.command.CommandResponsePackets;
import org.apache.shardingsphere.shardingproxy.transport.common.packet.command.query.DataHeaderPacket;
import org.apache.shardingsphere.shardingproxy.transport.common.packet.command.query.QueryResponsePackets;
import org.apache.shardingsphere.shardingproxy.transport.common.packet.generic.DatabaseFailurePacket;
import org.apache.shardingsphere.shardingproxy.transport.common.packet.generic.DatabaseSuccessPacket;
import org.apache.shardingsphere.shardingproxy.transport.mysql.constant.MySQLServerErrorCode;
import org.apache.shardingsphere.shardingproxy.transport.mysql.packet.generic.MySQLErrPacket;
import org.apache.shardingsphere.shardingproxy.transport.postgresql.packet.PostgreSQLPacketPayload;
import org.apache.shardingsphere.shardingproxy.transport.postgresql.packet.command.PostgreSQLCommandPacketType;
import org.apache.shardingsphere.shardingproxy.transport.postgresql.packet.command.query.PostgreSQLQueryCommandPacket;

import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;

/**
 * PostgreSQL command query packet.
 *
 * @author zhangyonglun
 */
@Slf4j
public final class PostgreSQLComQueryPacket implements PostgreSQLQueryCommandPacket {
    
    @Getter
    private final char messageType = PostgreSQLCommandPacketType.QUERY.getValue();
    
    private final String sql;
    
    private final TextProtocolBackendHandler textProtocolBackendHandler;
    
    public PostgreSQLComQueryPacket(final PostgreSQLPacketPayload payload, final BackendConnection backendConnection) {
        payload.readInt4();
        sql = payload.readStringNul();
        textProtocolBackendHandler = TextProtocolBackendHandlerFactory.newInstance(sql, backendConnection);
    }
    
    @Override
    public void write(final PostgreSQLPacketPayload payload) {
    }
    
    @Override
    public Optional<CommandResponsePackets> execute() {
        log.debug("PostgreSQLComQueryPacket received for Sharding-Proxy: {}", sql);
        if (GlobalRegistry.getInstance().isCircuitBreak()) {
            return Optional.of(new CommandResponsePackets(new MySQLErrPacket(1, MySQLServerErrorCode.ER_CIRCUIT_BREAK_MODE)));
        }
        BackendResponse backendResponse = textProtocolBackendHandler.execute();
        if (backendResponse instanceof ErrorResponse) {
            return Optional.of(new CommandResponsePackets(createDatabaseFailurePacket((ErrorResponse) backendResponse)));
        }
        if (backendResponse instanceof UpdateResponse) {
            return Optional.of(new CommandResponsePackets(createUpdatePacket((UpdateResponse) backendResponse)));
        }
        Collection<DataHeaderPacket> dataHeaderPackets = createDataHeaderPackets((QueryResponse) backendResponse);
        return Optional.<CommandResponsePackets>of(new QueryResponsePackets(dataHeaderPackets, dataHeaderPackets.size() + 2));
    }
    
    private DatabaseFailurePacket createDatabaseFailurePacket(final ErrorResponse errorResponse) {
        return new DatabaseFailurePacket(1, errorResponse.getErrorCode(), errorResponse.getSqlState(), errorResponse.getErrorMessage());
    }
    
    private DatabaseSuccessPacket createUpdatePacket(final UpdateResponse updateResponse) {
        return new DatabaseSuccessPacket(1, updateResponse.getUpdateCount(), updateResponse.getLastInsertId());
    }
    
    private Collection<DataHeaderPacket> createDataHeaderPackets(final QueryResponse queryResponse) {
        Collection<DataHeaderPacket> result = new LinkedList<>();
        int sequenceId = 1;
        for (QueryHeader each : queryResponse.getQueryHeaders()) {
            result.add(new DataHeaderPacket(
                    ++sequenceId, each.getSchema(), each.getTable(), each.getTable(), each.getColumnLabel(), each.getColumnName(), each.getColumnLength(), each.getColumnType(), each.getDecimals()));
        }
        return result;
    }
    
    @Override
    public boolean next() throws SQLException {
        return textProtocolBackendHandler.next();
    }
    
    @Override
    public DatabasePacket getQueryData() throws SQLException {
        return new PostgreSQLDataRowPacket(textProtocolBackendHandler.getQueryData().getData());
    }
    
    @Override
    public int getSequenceId() {
        return 0;
    }
}
