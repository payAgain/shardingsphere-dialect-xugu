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

package com.xugudb.shardingsphere.sql.parser.engine.xugu.visitor.statement.type;

import org.apache.shardingsphere.database.connector.core.type.DatabaseType;
import org.apache.shardingsphere.sql.parser.api.ASTNode;
import org.apache.shardingsphere.sql.parser.api.visitor.statement.type.DCLStatementVisitor;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AlterRoleContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AlterUserContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.CreateRoleContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.CreateUserContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DropRoleContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DropUserContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.GrantContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.RevokeContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.SetRoleContext;
import com.xugudb.shardingsphere.sql.parser.engine.xugu.visitor.statement.XuguStatementVisitor;
import org.apache.shardingsphere.sql.parser.statement.core.segment.generic.table.SimpleTableSegment;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.dcl.GrantStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.dcl.RevokeStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.dcl.role.AlterRoleStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.dcl.role.CreateRoleStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.dcl.role.DropRoleStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.dcl.role.SetRoleStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.dcl.user.AlterUserStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.dcl.user.CreateUserStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.dcl.user.DropUserStatement;

import java.util.Collections;

/**
 * DCL statement visitor for Xugu.
 */
public final class XuguDCLStatementVisitor extends XuguStatementVisitor implements DCLStatementVisitor {
    
    public XuguDCLStatementVisitor(final DatabaseType databaseType) {
        super(databaseType);
    }
    
    @Override
    public ASTNode visitGrant(final GrantContext ctx) {
        GrantStatement result = new GrantStatement(getDatabaseType());
        if (null != ctx.objectPrivilegeClause() && null != ctx.objectPrivilegeClause().onObjectClause().tableName()) {
            result.getTables().add((SimpleTableSegment) visit(ctx.objectPrivilegeClause().onObjectClause().tableName()));
        }
        return result;
    }
    
    @Override
    public ASTNode visitRevoke(final RevokeContext ctx) {
        RevokeStatement result = new RevokeStatement(getDatabaseType());
        if (null != ctx.objectPrivilegeClause() && null != ctx.objectPrivilegeClause().onObjectClause().tableName()) {
            result.getTables().add((SimpleTableSegment) visit(ctx.objectPrivilegeClause().onObjectClause().tableName()));
        }
        return result;
    }
    
    @Override
    public ASTNode visitCreateUser(final CreateUserContext ctx) {
        return new CreateUserStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitDropUser(final DropUserContext ctx) {
        return new DropUserStatement(getDatabaseType(), Collections.emptyList());
    }
    
    @Override
    public ASTNode visitAlterUser(final AlterUserContext ctx) {
        return new AlterUserStatement(getDatabaseType(), null);
    }
    
    @Override
    public ASTNode visitCreateRole(final CreateRoleContext ctx) {
        return new CreateRoleStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitAlterRole(final AlterRoleContext ctx) {
        return new AlterRoleStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitDropRole(final DropRoleContext ctx) {
        return new DropRoleStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitSetRole(final SetRoleContext ctx) {
        return new SetRoleStatement(getDatabaseType());
    }
}
