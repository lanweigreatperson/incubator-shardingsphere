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

package org.apache.shardingsphere.core.parsing.antlr.filler.encrypt.segment.impl;

import org.apache.shardingsphere.core.metadata.table.ShardingTableMetaData;
import org.apache.shardingsphere.core.parsing.antlr.sql.segment.FromWhereSegment;
import org.apache.shardingsphere.core.parsing.antlr.sql.segment.column.ColumnSegment;
import org.apache.shardingsphere.core.parsing.antlr.sql.segment.dml.UpdateSetWhereSegment;
import org.apache.shardingsphere.core.parsing.antlr.sql.segment.expr.ExpressionSegment;
import org.apache.shardingsphere.core.parsing.parser.context.condition.Column;
import org.apache.shardingsphere.core.parsing.parser.expression.SQLExpression;
import org.apache.shardingsphere.core.parsing.parser.sql.SQLStatement;
import org.apache.shardingsphere.core.parsing.parser.sql.dml.DMLStatement;
import org.apache.shardingsphere.core.parsing.parser.token.EncryptColumnToken;
import org.apache.shardingsphere.core.rule.EncryptRule;

import java.util.Map.Entry;

/**
 * Encrypt update set where filler.
 *
 * @author duhongjun
 */
public class EncryptUpdateSetWhereFiller extends EncryptDeleteFromWhereFiller {
    
    @Override
    public void fill(final FromWhereSegment sqlSegment, final SQLStatement sqlStatement, final String sql, final EncryptRule encryptRule, final ShardingTableMetaData shardingTableMetaData) {
        super.fill(sqlSegment, sqlStatement, sql, encryptRule, shardingTableMetaData);
        UpdateSetWhereSegment updateSetWhereSegment = (UpdateSetWhereSegment) sqlSegment;
        DMLStatement dmlStatement = (DMLStatement) sqlStatement;
        String updateTable = dmlStatement.getUpdateTableAlias().values().iterator().next();
        for (Entry<ColumnSegment, ExpressionSegment> each : updateSetWhereSegment.getUpdateColumns().entrySet()) {
            fillEncryptCondition(each, updateTable, sql, encryptRule, dmlStatement);
        }
        dmlStatement.setDeleteStatement(false);
    }
    
    private void fillEncryptCondition(final Entry<ColumnSegment, ExpressionSegment> entry, final String updateTable, final String sql, final EncryptRule encryptRule, final DMLStatement dmlStatement) {
        Column column = new Column(entry.getKey().getName(), updateTable);
        SQLExpression expression = entry.getValue().convertToSQLExpression(sql).get();
        dmlStatement.getUpdateColumnValues().put(column, expression);
        if (!encryptRule.getEncryptorEngine().getShardingEncryptor(column.getTableName(), column.getName()).isPresent()) {
            return;
        }
        dmlStatement.getSQLTokens().add(new EncryptColumnToken(entry.getKey().getStartIndex(), entry.getValue().getStopIndex(), column, false));
    }
}
