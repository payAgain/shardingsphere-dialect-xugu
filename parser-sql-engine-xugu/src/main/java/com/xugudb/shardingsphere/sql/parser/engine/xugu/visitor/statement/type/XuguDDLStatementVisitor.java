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

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Interval;
import org.apache.shardingsphere.database.connector.core.type.DatabaseType;
import org.apache.shardingsphere.sql.parser.api.ASTNode;
import org.apache.shardingsphere.sql.parser.api.visitor.statement.type.DDLStatementVisitor;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AddColumnSpecificationContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AddConstraintSpecificationContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AlterAnalyticViewContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AlterAttributeDimensionContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AlterAuditPolicyContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AlterClusterContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AlterDatabaseContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AlterDatabaseDictionaryContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AlterDatabaseLinkContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AlterDefinitionClauseContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AlterDimensionContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AlterDiskgroupContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AlterFlashbackArchiveContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AlterFunctionContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AlterHierarchyContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AlterIndexContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AlterIndexTypeContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AlterInmemoryJoinGroupContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AlterJavaContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AlterLibraryContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AlterLockdownProfileContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AlterMaterializedViewContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AlterMaterializedViewLogContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AlterMaterializedZonemapContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AlterOperatorContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AlterOutlineContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AlterPackageContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AlterPluggableDatabaseContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AlterProcedureContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AlterProfileContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AlterRollbackSegmentContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AlterSequenceContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AlterSessionContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AlterSynonymContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AlterSystemContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AlterTableContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AlterTablespaceContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AlterTriggerContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AlterTypeContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AlterViewContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AnalyzeContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AssociateStatisticsContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AuditContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AuditTraditionalContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.AuditUnifiedContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.BodyContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.CollectionVariableDeclContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.ColumnClausesContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.ColumnDefinitionContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.ColumnNameContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.ColumnOrVirtualDefinitionContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.CommentContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.ConstraintClausesContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.CreateClusterContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.CreateContextContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.CreateControlFileContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.CreateDatabaseContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.CreateDatabaseLinkContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.CreateDefinitionClauseContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.CreateDimensionContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.CreateDirectoryContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.CreateDiskgroupContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.CreateEditionContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.CreateFlashbackArchiveContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.CreateFunctionContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.CreateIndexContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.CreateInmemoryJoinGroupContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.CreateJavaContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.CreateLibraryContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.CreateLockdownProfileContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.CreateMaterializedViewContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.CreateMaterializedViewLogContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.CreateOperatorContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.CreateOutlineContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.CreatePFileContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.CreateProcedureContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.CreateProfileContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.CreateRelationalTableClauseContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.CreateRestorePointContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.CreateRollbackSegmentContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.CreateSPFileContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.CreateSequenceContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.CreateSynonymContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.CreateTableContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.CreateTablespaceContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.CreateTriggerContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.CreateTypeContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.CreateViewContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.CursorDefinitionContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.CursorForLoopStatementContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DataTypeDefinitionContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DisassociateStatisticsContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DmlStatementContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DropClusterContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DropColumnClauseContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DropColumnSpecificationContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DropConstraintClauseContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DropContextContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DropDatabaseContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DropDatabaseLinkContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DropDimensionContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DropDirectoryContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DropDiskgroupContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DropEditionContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DropFlashbackArchiveContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DropFunctionContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DropIndexContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DropIndexTypeContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DropInmemoryJoinGroupContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DropJavaContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DropLibraryContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DropLockdownProfileContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DropMaterializedViewContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DropMaterializedViewLogContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DropMaterializedZonemapContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DropOperatorContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DropOutlineContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DropPackageContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DropPluggableDatabaseContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DropProcedureContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DropProfileContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DropRestorePointContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DropRollbackSegmentContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DropSequenceContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DropSynonymContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DropTableContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DropTableSpaceContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DropTriggerContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DropTypeContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DropViewContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.DynamicSqlStmtContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.ExceptionHandlerContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.FlashbackDatabaseContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.FlashbackTableContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.FunctionContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.IndexExpressionContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.IndexExpressionsContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.IndexNameContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.IndexTypeNameContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.InlineConstraintContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.ItemDeclarationContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.ModifyColPropertiesContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.ModifyCollectionRetrievalContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.ModifyColumnSpecificationContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.ModifyConstraintClauseContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.NestedTableTypeSpecContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.NoAuditContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.ObjectBaseTypeDefContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.ObjectSubTypeDefContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.ObjectTypeDefContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.OpenForStatementContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.OperateColumnClauseContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.OutOfLineConstraintContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.OutOfLineRefConstraintContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.OwnerContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.PackageNameContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.ParameterDeclarationContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.PlsqlBlockContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.PlsqlFunctionSourceContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.PlsqlProcedureSourceContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.ProcedureCallContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.PurgeContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.RelationalPropertyContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.RenameContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.SchemaNameContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.SelectContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.SelectIntoStatementContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.SqlStatementInPlsqlContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.StatementContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.SwitchContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.SystemActionContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.TableNameContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.TruncateTableContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.TypeNameContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.VariableNameContext;
import com.xugudb.shardingsphere.sql.parser.autogen.XuguStatementParser.VarrayTypeSpecContext;
import com.xugudb.shardingsphere.sql.parser.engine.xugu.visitor.statement.XuguStatementVisitor;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dal.VariableSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.ddl.AlterDefinitionSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.ddl.CreateDefinitionSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.ddl.column.ColumnDefinitionSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.ddl.column.alter.AddColumnDefinitionSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.ddl.column.alter.DropColumnDefinitionSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.ddl.column.alter.ModifyCollectionRetrievalSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.ddl.column.alter.ModifyColumnDefinitionSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.ddl.constraint.ConstraintDefinitionSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.ddl.constraint.ConstraintSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.ddl.constraint.alter.AddConstraintDefinitionSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.ddl.constraint.alter.DropConstraintDefinitionSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.ddl.constraint.alter.ModifyConstraintDefinitionSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.ddl.index.IndexSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.ddl.index.IndexTypeSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.ddl.packages.PackageSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.ddl.routine.FunctionNameSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.ddl.tablespace.TablespaceSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.ddl.type.TypeDefinitionSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.ddl.type.TypeSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.column.ColumnSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.expr.ExpressionSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.expr.FunctionSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.generic.DataTypeSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.generic.OwnerSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.generic.table.SimpleTableSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.procedure.CursorForLoopStatementSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.procedure.ProcedureBodyEndNameSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.procedure.ProcedureCallNameSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.procedure.SQLStatementSegment;
import org.apache.shardingsphere.sql.parser.statement.core.statement.SQLStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.CommentStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.TruncateStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.database.AlterDatabaseStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.database.CreateDatabaseStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.database.DropDatabaseStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.directory.CreateDirectoryStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.directory.DropDirectoryStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.function.AlterFunctionStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.function.DropFunctionStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.index.AlterIndexStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.index.CreateIndexStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.index.DropIndexStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.operator.AlterOperatorStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.operator.CreateOperatorStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.operator.DropOperatorStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.pkg.AlterPackageStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.pkg.DropPackageStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.procedure.AlterProcedureStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.procedure.DropProcedureStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.sequence.AlterSequenceStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.sequence.CreateSequenceStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.sequence.DropSequenceStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.synonym.AlterSynonymStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.synonym.CreateSynonymStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.synonym.DropSynonymStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.table.AlterTableStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.table.CreateTableStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.table.DropTableStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.tablespace.AlterTablespaceStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.tablespace.CreateTablespaceStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.tablespace.DropTablespaceStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.trigger.AlterTriggerStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.trigger.CreateTriggerStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.trigger.DropTriggerStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.type.AlterTypeStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.view.AlterMaterializedViewStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.view.AlterViewStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.view.CreateMaterializedViewStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.view.CreateViewStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.view.DropMaterializedViewStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.view.DropViewStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.dml.SelectStatement;
import org.apache.shardingsphere.sql.parser.statement.core.value.collection.CollectionValue;
import org.apache.shardingsphere.sql.parser.statement.core.value.identifier.IdentifierValue;
import org.apache.shardingsphere.sql.parser.statement.core.value.literal.impl.StringLiteralValue;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.XuguAlterAuditPolicyStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.XuguAlterHierarchyStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.XuguAlterSessionStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.XuguAlterSystemStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.XuguAnalyzeStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.XuguAuditStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.XuguCreateNestedTableTypeStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.XuguCreateObjectTypeStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.XuguCreateSubTypeStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.XuguCreateVarrayTypeStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.XuguNoAuditStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.XuguPLSQLBlockStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.XuguPurgeStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.XuguRenameStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.XuguSwitchStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.XuguSystemActionStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.cluster.XuguAlterClusterStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.cluster.XuguCreateClusterStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.cluster.XuguDropClusterStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.context.XuguCreateContextStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.context.XuguDropContextStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.database.XuguAlterDatabaseDictionaryStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.database.XuguAlterDatabaseLinkStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.database.XuguAlterPluggableDatabaseStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.database.XuguCreateDatabaseLinkStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.database.XuguDropDatabaseLinkStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.database.XuguDropPluggableDatabaseStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.dimension.XuguAlterAttributeDimensionStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.dimension.XuguAlterDimensionStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.dimension.XuguCreateDimensionStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.dimension.XuguDropDimensionStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.diskgroup.XuguAlterDiskgroupStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.diskgroup.XuguCreateDiskgroupStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.diskgroup.XuguDropDiskgroupStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.edition.XuguCreateEditionStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.edition.XuguDropEditionStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.file.XuguCreateControlFileStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.file.XuguCreatePFileStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.file.XuguCreateSPFileStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.flashback.XuguAlterFlashbackArchiveStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.flashback.XuguCreateFlashbackArchiveStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.flashback.XuguDropFlashbackArchiveStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.flashback.XuguFlashbackDatabaseStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.flashback.XuguFlashbackTableStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.function.XuguCreateFunctionStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.index.XuguAlterIndexTypeStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.index.XuguDropIndexTypeStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.java.XuguAlterJavaStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.java.XuguCreateJavaStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.java.XuguDropJavaStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.join.XuguAlterInMemoryJoinGroupStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.join.XuguCreateInMemoryJoinGroupStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.join.XuguDropInMemoryJoinGroupStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.library.XuguAlterLibraryStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.library.XuguCreateLibraryStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.library.XuguDropLibraryStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.lockdown.XuguAlterLockdownProfileStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.lockdown.XuguCreateLockdownProfileStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.lockdown.XuguDropLockdownProfileStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.outline.XuguAlterOutlineStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.outline.XuguCreateOutlineStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.outline.XuguDropOutlineStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.procedure.XuguCreateProcedureStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.profile.XuguAlterProfileStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.profile.XuguCreateProfileStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.profile.XuguDropProfileStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.restore.XuguCreateRestorePointStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.restore.XuguDropRestorePointStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.rollback.XuguAlterRollbackSegmentStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.rollback.XuguCreateRollbackSegmentStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.rollback.XuguDropRollbackSegmentStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.statistics.XuguAssociateStatisticsStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.statistics.XuguDisassociateStatisticsStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.view.XuguAlterAnalyticViewStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.view.XuguAlterMaterializedViewLogStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.view.XuguCreateMaterializedViewLogStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.view.XuguDropMaterializedViewLogStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.zone.XuguAlterMaterializedZoneMapStatement;
import com.xugudb.shardingsphere.sql.parser.statement.xugu.ddl.zone.XuguDropMaterializedZoneMapStatement;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DDL statement visitor for Xugu.
 */
public final class XuguDDLStatementVisitor extends XuguStatementVisitor implements DDLStatementVisitor {
    
    public XuguDDLStatementVisitor(final DatabaseType databaseType) {
        super(databaseType);
    }
    
    @Override
    public ASTNode visitCreateView(final CreateViewContext ctx) {
        CreateViewStatement result = new CreateViewStatement(getDatabaseType());
        result.setReplaceView(null != ctx.REPLACE());
        XuguDMLStatementVisitor visitor = new XuguDMLStatementVisitor(getDatabaseType());
        getGlobalParameterMarkerSegments().addAll(visitor.getGlobalParameterMarkerSegments());
        getStatementParameterMarkerSegments().addAll(visitor.getStatementParameterMarkerSegments());
        result.setView((SimpleTableSegment) visit(ctx.viewName()));
        result.setSelect((SelectStatement) visitor.visit(ctx.select()));
        result.setViewDefinition(getOriginalText(ctx.select()));
        result.addParameterMarkers(getGlobalParameterMarkerSegments());
        return result;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public ASTNode visitCreateTable(final CreateTableContext ctx) {
        CreateTableStatement result = new CreateTableStatement(getDatabaseType());
        result.setTable((SimpleTableSegment) visit(ctx.tableName()));
        if (null != ctx.createDefinitionClause()) {
            CollectionValue<CreateDefinitionSegment> createDefinitions = (CollectionValue<CreateDefinitionSegment>) visit(ctx.createDefinitionClause());
            for (CreateDefinitionSegment each : createDefinitions.getValue()) {
                if (each instanceof ColumnDefinitionSegment) {
                    result.getColumnDefinitions().add((ColumnDefinitionSegment) each);
                } else if (each instanceof ConstraintDefinitionSegment) {
                    result.getConstraintDefinitions().add((ConstraintDefinitionSegment) each);
                }
            }
        }
        return result;
    }
    
    @Override
    public ASTNode visitCreateType(final CreateTypeContext ctx) {
        boolean isReplace = null != ctx.REPLACE();
        boolean isEditionable = null == ctx.NONEDITIONABLE();
        TypeSegment typeSegment = (TypeSegment) visit(ctx.plsqlTypeSource().typeName());
        if (null != ctx.plsqlTypeSource().objectSubTypeDef()) {
            ObjectSubTypeDefContext objectSubTypeDefContext = ctx.plsqlTypeSource().objectSubTypeDef();
            return new XuguCreateSubTypeStatement(getDatabaseType(), isReplace, isEditionable,
                    null == objectSubTypeDefContext.finalClause() || null == objectSubTypeDefContext.finalClause().NOT(),
                    null == objectSubTypeDefContext.instantiableClause() || null == objectSubTypeDefContext.instantiableClause().NOT(),
                    typeSegment,
                    objectSubTypeDefContext.dataTypeDefinition().stream().map(definition -> (TypeDefinitionSegment) visit(definition)).collect(Collectors.toList()));
        } else {
            return visitCreateTypeObjectBaseTypeDef(ctx.plsqlTypeSource().objectBaseTypeDef(), isReplace, isEditionable, typeSegment);
        }
    }
    
    private ASTNode visitCreateTypeObjectBaseTypeDef(final ObjectBaseTypeDefContext ctx, final boolean isReplace, final boolean isEditionable, final TypeSegment typeSegment) {
        if (null != ctx.objectTypeDef()) {
            ObjectTypeDefContext objectTypeDefContext = ctx.objectTypeDef();
            return new XuguCreateObjectTypeStatement(getDatabaseType(), isReplace, isEditionable, null == objectTypeDefContext.finalClause() || null == objectTypeDefContext.finalClause().NOT(),
                    null == objectTypeDefContext.instantiableClause() || null == objectTypeDefContext.instantiableClause().NOT(),
                    null == objectTypeDefContext.persistableClause() || null == objectTypeDefContext.persistableClause().NOT(),
                    typeSegment, objectTypeDefContext.dataTypeDefinition().stream().map(definition -> (TypeDefinitionSegment) visit(definition)).collect(Collectors.toList()));
        } else if (null != ctx.varrayTypeSpec()) {
            VarrayTypeSpecContext varrayTypeSpecContext = ctx.varrayTypeSpec();
            return new XuguCreateVarrayTypeStatement(getDatabaseType(), isReplace, isEditionable,
                    null == varrayTypeSpecContext.INTEGER_() ? -1 : Integer.parseInt(varrayTypeSpecContext.INTEGER_().getText()),
                    null != varrayTypeSpecContext.typeSpec().NULL(),
                    null == varrayTypeSpecContext.typeSpec().persistableClause() || null == varrayTypeSpecContext.typeSpec().persistableClause().NOT(),
                    typeSegment,
                    (DataTypeSegment) visit(varrayTypeSpecContext.typeSpec().dataType()));
        } else {
            NestedTableTypeSpecContext nestedTableTypeSpecContext = ctx.nestedTableTypeSpec();
            return new XuguCreateNestedTableTypeStatement(getDatabaseType(), isReplace, isEditionable,
                    null != nestedTableTypeSpecContext.typeSpec().NULL(),
                    null == nestedTableTypeSpecContext.typeSpec().persistableClause() || null == nestedTableTypeSpecContext.typeSpec().persistableClause().NOT(),
                    typeSegment,
                    (DataTypeSegment) visit(nestedTableTypeSpecContext.typeSpec().dataType()));
        }
    }
    
    @Override
    public ASTNode visitDataTypeDefinition(final DataTypeDefinitionContext ctx) {
        return new TypeDefinitionSegment(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex(), ctx.name().getText(), (DataTypeSegment) visit(ctx.dataType()));
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public ASTNode visitCreateDefinitionClause(final CreateDefinitionClauseContext ctx) {
        CollectionValue<CreateDefinitionSegment> result = new CollectionValue<>();
        if (null != ctx.createRelationalTableClause()) {
            result.combine((CollectionValue<CreateDefinitionSegment>) visit(ctx.createRelationalTableClause()));
        }
        return result;
    }
    
    @Override
    public ASTNode visitCreateRelationalTableClause(final CreateRelationalTableClauseContext ctx) {
        CollectionValue<CreateDefinitionSegment> result = new CollectionValue<>();
        if (null == ctx.relationalProperties()) {
            return result;
        }
        for (RelationalPropertyContext each : ctx.relationalProperties().relationalProperty()) {
            if (null != each.columnDefinition()) {
                result.getValue().add((ColumnDefinitionSegment) visit(each.columnDefinition()));
            }
            if (null != each.outOfLineConstraint()) {
                result.getValue().add((ConstraintDefinitionSegment) visit(each.outOfLineConstraint()));
            }
            if (null != each.outOfLineRefConstraint()) {
                result.getValue().add((ConstraintDefinitionSegment) visit(each.outOfLineRefConstraint()));
            }
        }
        return result;
    }
    
    @Override
    public ASTNode visitColumnDefinition(final ColumnDefinitionContext ctx) {
        ColumnSegment column = (ColumnSegment) visit(ctx.columnName());
        DataTypeSegment dataType = null == ctx.dataType() ? null : (DataTypeSegment) visit(ctx.dataType());
        boolean isPrimaryKey = ctx.inlineConstraint().stream().anyMatch(each -> null != each.primaryKey());
        boolean isNotNull = ctx.inlineConstraint().stream().anyMatch(each -> null != each.NOT() && null != each.NULL());
        ColumnDefinitionSegment result = new ColumnDefinitionSegment(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex(), column, dataType, isPrimaryKey, isNotNull, getText(ctx));
        if (null != ctx.REF() && null != ctx.dataType()) {
            result.setRef(true);
        }
        for (InlineConstraintContext each : ctx.inlineConstraint()) {
            if (null != each.referencesClause()) {
                result.getReferencedTables().add((SimpleTableSegment) visit(each.referencesClause().tableName()));
            }
        }
        if (null != ctx.inlineRefConstraint()) {
            result.getReferencedTables().add((SimpleTableSegment) visit(ctx.inlineRefConstraint().tableName()));
        }
        return result;
    }
    
    private String getText(final ParserRuleContext ctx) {
        return ctx.start.getInputStream().getText(new Interval(ctx.start.getStartIndex(), ctx.stop.getStopIndex()));
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public ASTNode visitOutOfLineConstraint(final OutOfLineConstraintContext ctx) {
        ConstraintDefinitionSegment result = new ConstraintDefinitionSegment(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex());
        if (null != ctx.constraintName()) {
            result.setConstraintName((ConstraintSegment) visit(ctx.constraintName()));
        }
        if (null != ctx.primaryKey()) {
            result.getPrimaryKeyColumns().addAll(((CollectionValue<ColumnSegment>) visit(ctx.columnNames())).getValue());
        }
        if (null != ctx.UNIQUE()) {
            result.getIndexColumns().addAll(((CollectionValue<ColumnSegment>) visit(ctx.columnNames())).getValue());
        }
        if (null != ctx.referencesClause()) {
            result.setReferencedTable((SimpleTableSegment) visit(ctx.referencesClause().tableName()));
        }
        return result;
    }
    
    @Override
    public ASTNode visitOutOfLineRefConstraint(final OutOfLineRefConstraintContext ctx) {
        ConstraintDefinitionSegment result = new ConstraintDefinitionSegment(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex());
        if (null != ctx.constraintName()) {
            result.setConstraintName((ConstraintSegment) visit(ctx.constraintName()));
        }
        if (null != ctx.referencesClause()) {
            result.setReferencedTable((SimpleTableSegment) visit(ctx.referencesClause().tableName()));
        }
        return result;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public ASTNode visitAlterTable(final AlterTableContext ctx) {
        AlterTableStatement result = new AlterTableStatement(getDatabaseType());
        result.setTable((SimpleTableSegment) visit(ctx.tableName()));
        if (null != ctx.alterDefinitionClause()) {
            for (AlterDefinitionSegment each : ((CollectionValue<AlterDefinitionSegment>) visit(ctx.alterDefinitionClause())).getValue()) {
                if (each instanceof AddColumnDefinitionSegment) {
                    result.getAddColumnDefinitions().add((AddColumnDefinitionSegment) each);
                } else if (each instanceof ModifyColumnDefinitionSegment) {
                    result.getModifyColumnDefinitions().add((ModifyColumnDefinitionSegment) each);
                } else if (each instanceof DropColumnDefinitionSegment) {
                    result.getDropColumnDefinitions().add((DropColumnDefinitionSegment) each);
                } else if (each instanceof AddConstraintDefinitionSegment) {
                    result.getAddConstraintDefinitions().add((AddConstraintDefinitionSegment) each);
                } else if (each instanceof ModifyConstraintDefinitionSegment) {
                    result.getModifyConstraintDefinitions().add((ModifyConstraintDefinitionSegment) each);
                } else if (each instanceof DropConstraintDefinitionSegment) {
                    result.getDropConstraintDefinitions().add((DropConstraintDefinitionSegment) each);
                } else if (each instanceof ModifyCollectionRetrievalSegment) {
                    result.setModifyCollectionRetrieval((ModifyCollectionRetrievalSegment) each);
                }
            }
        }
        return result;
    }
    
    @Override
    public ASTNode visitAlterTablespace(final AlterTablespaceContext ctx) {
        return new AlterTablespaceStatement(getDatabaseType(),
                null == ctx.tablespaceName() ? null
                        : new TablespaceSegment(
                                ctx.tablespaceName().getStart().getStartIndex(), ctx.tablespaceName().getStop().getStopIndex(), (IdentifierValue) visit(ctx.tablespaceName())),
                null == ctx.newTablespaceName() ? null
                        : new TablespaceSegment(
                                ctx.newTablespaceName().getStart().getStartIndex(), ctx.newTablespaceName().getStop().getStopIndex(), (IdentifierValue) visit(ctx.newTablespaceName())));
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public ASTNode visitAlterDefinitionClause(final AlterDefinitionClauseContext ctx) {
        CollectionValue<AlterDefinitionSegment> result = new CollectionValue<>();
        if (null != ctx.columnClauses()) {
            result.getValue().addAll(((CollectionValue<AddColumnDefinitionSegment>) visit(ctx.columnClauses())).getValue());
        }
        if (null != ctx.constraintClauses()) {
            result.getValue().addAll(((CollectionValue<AddColumnDefinitionSegment>) visit(ctx.constraintClauses())).getValue());
        }
        // TODO More alter definition parse
        return result;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public ASTNode visitColumnClauses(final ColumnClausesContext ctx) {
        CollectionValue<AlterDefinitionSegment> result = new CollectionValue<>();
        for (OperateColumnClauseContext each : ctx.operateColumnClause()) {
            if (null != each.addColumnSpecification()) {
                result.getValue().addAll(((CollectionValue<AddColumnDefinitionSegment>) visit(each.addColumnSpecification())).getValue());
            }
            if (null != each.modifyColumnSpecification()) {
                result.getValue().add((ModifyColumnDefinitionSegment) visit(each.modifyColumnSpecification()));
            }
            if (null != each.dropColumnClause()) {
                result.getValue().add((DropColumnDefinitionSegment) visit(each.dropColumnClause()));
            }
        }
        if (null != ctx.modifyCollectionRetrieval()) {
            result.getValue().add((ModifyCollectionRetrievalSegment) visit(ctx.modifyCollectionRetrieval()));
        }
        return result;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public ASTNode visitConstraintClauses(final ConstraintClausesContext ctx) {
        // TODO Support rename constraint
        CollectionValue<AlterDefinitionSegment> result = new CollectionValue<>();
        if (null != ctx.addConstraintSpecification()) {
            result.combine((CollectionValue<AlterDefinitionSegment>) visit(ctx.addConstraintSpecification()));
        }
        if (null != ctx.modifyConstraintClause()) {
            result.getValue().add((AlterDefinitionSegment) visit(ctx.modifyConstraintClause()));
        }
        for (DropConstraintClauseContext each : ctx.dropConstraintClause()) {
            if (null != each.constraintName()) {
                result.getValue().add((AlterDefinitionSegment) visit(each));
            }
        }
        return result;
    }
    
    @Override
    public ASTNode visitModifyCollectionRetrieval(final ModifyCollectionRetrievalContext ctx) {
        return new ModifyCollectionRetrievalSegment(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex(), (SimpleTableSegment) visit(ctx.tableName()));
    }
    
    @Override
    public ASTNode visitAddColumnSpecification(final AddColumnSpecificationContext ctx) {
        CollectionValue<AddColumnDefinitionSegment> result = new CollectionValue<>();
        for (ColumnOrVirtualDefinitionContext each : ctx.columnOrVirtualDefinitions().columnOrVirtualDefinition()) {
            if (null != each.columnDefinition()) {
                AddColumnDefinitionSegment addColumnDefinition = new AddColumnDefinitionSegment(
                        each.columnDefinition().getStart().getStartIndex(), each.columnDefinition().getStop().getStopIndex(),
                        Collections.singletonList((ColumnDefinitionSegment) visit(each.columnDefinition())));
                result.getValue().add(addColumnDefinition);
            }
        }
        return result;
    }
    
    @Override
    public ASTNode visitModifyColumnSpecification(final ModifyColumnSpecificationContext ctx) {
        // TODO handle no columnDefinition and multiple columnDefinitions
        ColumnDefinitionSegment columnDefinition = null;
        for (ModifyColPropertiesContext each : ctx.modifyColProperties()) {
            columnDefinition = (ColumnDefinitionSegment) visit(each);
        }
        return new ModifyColumnDefinitionSegment(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex(), columnDefinition);
    }
    
    @Override
    public ASTNode visitDropColumnClause(final DropColumnClauseContext ctx) {
        if (null != ctx.dropColumnSpecification()) {
            return visit(ctx.dropColumnSpecification());
        }
        Collection<ColumnSegment> columns = new LinkedList<>();
        if (null != ctx.columnOrColumnList().columnName()) {
            columns.add((ColumnSegment) visit(ctx.columnOrColumnList().columnName()));
        } else {
            for (ColumnNameContext each : ctx.columnOrColumnList().columnNames().columnName()) {
                columns.add((ColumnSegment) visit(each));
            }
        }
        return new DropColumnDefinitionSegment(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex(), columns);
    }
    
    @Override
    public ASTNode visitModifyColProperties(final ModifyColPropertiesContext ctx) {
        ColumnSegment column = (ColumnSegment) visit(ctx.columnName());
        DataTypeSegment dataType = null == ctx.dataType() ? null : (DataTypeSegment) visit(ctx.dataType());
        // TODO visit pk and reference table
        return new ColumnDefinitionSegment(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex(), column, dataType, false, false, getText(ctx));
    }
    
    @Override
    public ASTNode visitDropColumnSpecification(final DropColumnSpecificationContext ctx) {
        Collection<ColumnSegment> columns = new LinkedList<>();
        if (null != ctx.columnOrColumnList().columnName()) {
            columns.add((ColumnSegment) visit(ctx.columnOrColumnList().columnName()));
        } else {
            for (ColumnNameContext each : ctx.columnOrColumnList().columnNames().columnName()) {
                columns.add((ColumnSegment) visit(each));
            }
        }
        return new DropColumnDefinitionSegment(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex(), columns);
    }
    
    @Override
    public ASTNode visitAddConstraintSpecification(final AddConstraintSpecificationContext ctx) {
        CollectionValue<AddConstraintDefinitionSegment> result = new CollectionValue<>();
        for (OutOfLineConstraintContext each : ctx.outOfLineConstraint()) {
            result.getValue().add(new AddConstraintDefinitionSegment(each.getStart().getStartIndex(), each.getStop().getStopIndex(), (ConstraintDefinitionSegment) visit(each)));
        }
        if (null != ctx.outOfLineRefConstraint()) {
            result.getValue().add(new AddConstraintDefinitionSegment(ctx.outOfLineRefConstraint().getStart().getStartIndex(), ctx.outOfLineRefConstraint().getStop().getStopIndex(),
                    (ConstraintDefinitionSegment) visit(ctx.outOfLineRefConstraint())));
        }
        return result;
    }
    
    @Override
    public ASTNode visitModifyConstraintClause(final ModifyConstraintClauseContext ctx) {
        if (null != ctx.constraintOption().constraintWithName()) {
            return new ModifyConstraintDefinitionSegment(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex(),
                    (ConstraintSegment) visit(ctx.constraintOption().constraintWithName().constraintName()));
        } else {
            return new ModifyConstraintDefinitionSegment(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex(), null);
        }
    }
    
    @Override
    public ASTNode visitDropConstraintClause(final DropConstraintClauseContext ctx) {
        return new DropConstraintDefinitionSegment(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex(), (ConstraintSegment) visit(ctx.constraintName()));
    }
    
    @Override
    public ASTNode visitDropContext(final DropContextContext ctx) {
        return new XuguDropContextStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitDropTable(final DropTableContext ctx) {
        DropTableStatement result = new DropTableStatement(getDatabaseType());
        result.getTables().add((SimpleTableSegment) visit(ctx.tableName()));
        return result;
    }
    
    @Override
    public ASTNode visitDropDatabaseLink(final DropDatabaseLinkContext ctx) {
        return new XuguDropDatabaseLinkStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitAlterDatabaseLink(final AlterDatabaseLinkContext ctx) {
        return new XuguAlterDatabaseLinkStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitAlterDatabaseDictionary(final AlterDatabaseDictionaryContext ctx) {
        return new XuguAlterDatabaseDictionaryStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitAlterView(final AlterViewContext ctx) {
        AlterViewStatement result = new AlterViewStatement(getDatabaseType());
        result.setView((SimpleTableSegment) visit(ctx.viewName()));
        result.setConstraintDefinition((ConstraintDefinitionSegment) getAlterViewConstraintDefinition(ctx));
        return result;
    }
    
    private ASTNode getAlterViewConstraintDefinition(final AlterViewContext ctx) {
        ConstraintDefinitionSegment result = null;
        if (null != ctx.outOfLineConstraint()) {
            result = (ConstraintDefinitionSegment) visit(ctx.outOfLineConstraint());
        } else if (null != ctx.constraintName()) {
            result = new ConstraintDefinitionSegment(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex());
            result.setConstraintName((ConstraintSegment) visit(ctx.constraintName()));
        }
        return result;
    }
    
    @Override
    public ASTNode visitDropPackage(final DropPackageContext ctx) {
        return new DropPackageStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitAlterPackage(final AlterPackageContext ctx) {
        return new AlterPackageStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitCreateSynonym(final CreateSynonymContext ctx) {
        return new CreateSynonymStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitDropSynonym(final DropSynonymContext ctx) {
        return new DropSynonymStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitCreateDirectory(final CreateDirectoryContext ctx) {
        return new CreateDirectoryStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitDropView(final DropViewContext ctx) {
        DropViewStatement result = new DropViewStatement(getDatabaseType());
        result.getViews().add((SimpleTableSegment) visit(ctx.viewName()));
        return result;
    }
    
    @Override
    public ASTNode visitCreateEdition(final CreateEditionContext ctx) {
        return new XuguCreateEditionStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitDropTrigger(final DropTriggerContext ctx) {
        return new DropTriggerStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitCreateTrigger(final CreateTriggerContext ctx) {
        return new CreateTriggerStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitAlterTrigger(final AlterTriggerContext ctx) {
        return new AlterTriggerStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitTruncateTable(final TruncateTableContext ctx) {
        return new TruncateStatement(getDatabaseType(), Collections.singleton((SimpleTableSegment) visit(ctx.tableName())));
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public ASTNode visitCreateIndex(final CreateIndexContext ctx) {
        CreateIndexStatement result = new CreateIndexStatement(getDatabaseType());
        if (null != ctx.createIndexDefinitionClause().tableIndexClause()) {
            result.setTable((SimpleTableSegment) visit(ctx.createIndexDefinitionClause().tableIndexClause().tableName()));
            result.getColumns().addAll(((CollectionValue) visit(ctx.createIndexDefinitionClause().tableIndexClause().indexExpressions())).getValue());
        }
        result.setIndex((IndexSegment) visit(ctx.indexName()));
        if (null != ctx.createIndexSpecification() && null != ctx.createIndexSpecification().UNIQUE()) {
            result.getIndex().setUniqueKey(true);
        }
        return result;
    }
    
    @Override
    public ASTNode visitIndexExpressions(final IndexExpressionsContext ctx) {
        CollectionValue<ColumnSegment> result = new CollectionValue<>();
        for (IndexExpressionContext each : ctx.indexExpression()) {
            ASTNode astNode = visit(each);
            if (astNode instanceof ColumnSegment) {
                result.getValue().add((ColumnSegment) astNode);
            }
            if (astNode instanceof FunctionSegment) {
                ((FunctionSegment) astNode).getParameters().forEach(parameter -> {
                    if (parameter instanceof ColumnSegment) {
                        result.getValue().add((ColumnSegment) parameter);
                    }
                });
            }
        }
        return result;
    }
    
    @Override
    public ASTNode visitIndexExpression(final IndexExpressionContext ctx) {
        return null == ctx.expr() ? visit(ctx.columnName()) : visit(ctx.expr());
    }
    
    @Override
    public ASTNode visitAlterIndex(final AlterIndexContext ctx) {
        AlterIndexStatement result = new AlterIndexStatement(getDatabaseType());
        result.setIndex((IndexSegment) visit(ctx.indexName()));
        return result;
    }
    
    @Override
    public ASTNode visitDropIndex(final DropIndexContext ctx) {
        DropIndexStatement result = new DropIndexStatement(getDatabaseType());
        result.getIndexes().add((IndexSegment) visit(ctx.indexName()));
        return result;
    }
    
    @Override
    public ASTNode visitAlterSynonym(final AlterSynonymContext ctx) {
        return new AlterSynonymStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitAlterSession(final AlterSessionContext ctx) {
        return new XuguAlterSessionStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitAlterDatabase(final AlterDatabaseContext ctx) {
        return new AlterDatabaseStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitAlterSystem(final AlterSystemContext ctx) {
        return new XuguAlterSystemStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitAnalyze(final AnalyzeContext ctx) {
        return new XuguAnalyzeStatement(getDatabaseType(), null == ctx.tableName() ? null : (SimpleTableSegment) visit(ctx.tableName()),
                null == ctx.indexName() ? null : (IndexSegment) visit(ctx.indexName()));
    }
    
    @Override
    public ASTNode visitAssociateStatistics(final AssociateStatisticsContext ctx) {
        XuguAssociateStatisticsStatement result = new XuguAssociateStatisticsStatement(getDatabaseType());
        if (null != ctx.columnAssociation()) {
            for (TableNameContext each : ctx.columnAssociation().tableName()) {
                result.getTables().add((SimpleTableSegment) visit(each));
            }
            for (ColumnNameContext each : ctx.columnAssociation().columnName()) {
                result.getColumns().add((ColumnSegment) visit(each));
            }
        }
        if (null != ctx.functionAssociation()) {
            for (IndexNameContext each : ctx.functionAssociation().indexName()) {
                result.getIndexes().add((IndexSegment) visit(each));
            }
            for (FunctionContext each : ctx.functionAssociation().function()) {
                result.getFunctions().add((FunctionSegment) visit(each));
            }
            for (PackageNameContext each : ctx.functionAssociation().packageName()) {
                result.getPackages().add((PackageSegment) visit(each));
            }
            for (TypeNameContext each : ctx.functionAssociation().typeName()) {
                result.getTypes().add((TypeSegment) visit(each));
            }
            for (IndexTypeNameContext each : ctx.functionAssociation().indexTypeName()) {
                result.getIndexTypes().add((IndexTypeSegment) visit(each));
            }
        }
        return result;
    }
    
    @Override
    public ASTNode visitDisassociateStatistics(final DisassociateStatisticsContext ctx) {
        XuguDisassociateStatisticsStatement result = new XuguDisassociateStatisticsStatement(getDatabaseType());
        if (null != ctx.tableName()) {
            for (TableNameContext each : ctx.tableName()) {
                result.getTables().add((SimpleTableSegment) visit(each));
            }
            for (ColumnNameContext each : ctx.columnName()) {
                result.getColumns().add((ColumnSegment) visit(each));
            }
        }
        if (null != ctx.indexName()) {
            for (IndexNameContext each : ctx.indexName()) {
                result.getIndexes().add((IndexSegment) visit(each));
            }
        }
        if (null != ctx.function()) {
            for (FunctionContext each : ctx.function()) {
                result.getFunctions().add((FunctionSegment) visit(each));
            }
        }
        if (null != ctx.packageName()) {
            for (PackageNameContext each : ctx.packageName()) {
                result.getPackages().add((PackageSegment) visit(each));
            }
        }
        if (null != ctx.typeName()) {
            for (TypeNameContext each : ctx.typeName()) {
                result.getTypes().add((TypeSegment) visit(each));
            }
        }
        if (null != ctx.indexTypeName()) {
            for (IndexTypeNameContext each : ctx.indexTypeName()) {
                result.getIndexTypes().add((IndexTypeSegment) visit(each));
            }
        }
        return result;
    }
    
    @Override
    public ASTNode visitAudit(final AuditContext ctx) {
        return null == ctx.auditTraditional() ? visit(ctx.auditUnified()) : visit(ctx.auditTraditional());
    }
    
    @Override
    public ASTNode visitAuditTraditional(final AuditTraditionalContext ctx) {
        return new XuguAuditStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitAuditUnified(final AuditUnifiedContext ctx) {
        return new XuguAuditStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitNoAudit(final NoAuditContext ctx) {
        return new XuguNoAuditStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitComment(final CommentContext ctx) {
        CommentStatement result = new CommentStatement(getDatabaseType());
        if (null != ctx.tableName()) {
            result.setTable((SimpleTableSegment) visit(ctx.tableName()));
        }
        if (null != ctx.columnName()) {
            result.setColumn((ColumnSegment) visit(ctx.columnName()));
        }
        if (null != ctx.indexTypeName()) {
            result.setIndexType((IndexTypeSegment) visit(ctx.indexTypeName()));
        }
        result.setComment(new IdentifierValue(ctx.STRING_().getText()));
        return result;
    }
    
    @Override
    public ASTNode visitFlashbackDatabase(final FlashbackDatabaseContext ctx) {
        return new XuguFlashbackDatabaseStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitFlashbackTable(final FlashbackTableContext ctx) {
        return new XuguFlashbackTableStatement(getDatabaseType(), (SimpleTableSegment) visit(ctx.tableName()),
                null == ctx.renameToTable() ? null : (SimpleTableSegment) visit(ctx.renameToTable().tableName()));
    }
    
    @Override
    public ASTNode visitPurge(final PurgeContext ctx) {
        return new XuguPurgeStatement(getDatabaseType(), null == ctx.tableName() ? null : (SimpleTableSegment) visit(ctx.tableName()),
                null == ctx.indexName() ? null : (IndexSegment) visit(ctx.indexName()));
    }
    
    @Override
    public ASTNode visitRename(final RenameContext ctx) {
        return new XuguRenameStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitCreateDatabase(final CreateDatabaseContext ctx) {
        return new CreateDatabaseStatement(getDatabaseType(), null, false);
    }
    
    @Override
    public ASTNode visitCreateDatabaseLink(final CreateDatabaseLinkContext ctx) {
        return new XuguCreateDatabaseLinkStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitCreateDimension(final CreateDimensionContext ctx) {
        return new XuguCreateDimensionStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitAlterDimension(final AlterDimensionContext ctx) {
        return new XuguAlterDimensionStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitDropDimension(final DropDimensionContext ctx) {
        return new XuguDropDimensionStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitDropDirectory(final DropDirectoryContext ctx) {
        return new DropDirectoryStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitCreateFunction(final CreateFunctionContext ctx) {
        return visitCreateFunction0(ctx);
    }
    
    private ASTNode visitCreateFunction0(final CreateFunctionContext ctx) {
        if (null != ctx.plsqlFunctionSource().declareSection()) {
            visit(ctx.plsqlFunctionSource().declareSection());
        }
        if (null != ctx.plsqlFunctionSource().body()) {
            visit(ctx.plsqlFunctionSource().body());
        }
        getSqlStatementsInPlsql().sort(Comparator.comparingInt(SQLStatementSegment::getStartIndex));
        getProcedureCallNames().sort(Comparator.comparingInt(ProcedureCallNameSegment::getStartIndex));
        getDynamicSqlStatementExpressions().sort(Comparator.comparingInt(ExpressionSegment::getStartIndex));
        XuguCreateFunctionStatement result = new XuguCreateFunctionStatement(getDatabaseType(), getSqlStatementsInPlsql(), getProcedureCallNames());
        result.setFunctionName(visitFunctionName(ctx.plsqlFunctionSource()));
        result.getDynamicSqlStatementExpressions().addAll(getDynamicSqlStatementExpressions());
        return result;
    }
    
    private FunctionNameSegment visitFunctionName(final PlsqlFunctionSourceContext ctx) {
        OwnerContext schema = ctx.function().owner();
        IdentifierValue functionName = (IdentifierValue) visit(ctx.function().name().identifier());
        if (null == schema) {
            return new FunctionNameSegment(ctx.function().name().start.getStartIndex(), ctx.function().name().stop.getStopIndex(), functionName);
        }
        OwnerSegment owner = new OwnerSegment(schema.start.getStartIndex(), schema.stop.getStopIndex(), (IdentifierValue) visit(schema.identifier()));
        FunctionNameSegment result = new FunctionNameSegment(ctx.function().start.getStartIndex(), ctx.function().stop.getStopIndex(), functionName);
        result.setOwner(owner);
        return result;
    }
    
    @Override
    public ASTNode visitDropEdition(final DropEditionContext ctx) {
        return new XuguDropEditionStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitDropOutline(final DropOutlineContext ctx) {
        return new XuguDropOutlineStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitAlterOutline(final AlterOutlineContext ctx) {
        return new XuguAlterOutlineStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitAlterAnalyticView(final AlterAnalyticViewContext ctx) {
        return new XuguAlterAnalyticViewStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitAlterAttributeDimension(final AlterAttributeDimensionContext ctx) {
        return new XuguAlterAttributeDimensionStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitCreateSequence(final CreateSequenceContext ctx) {
        return new CreateSequenceStatement(getDatabaseType(), ctx.sequenceName().getText());
    }
    
    @Override
    public ASTNode visitAlterSequence(final AlterSequenceContext ctx) {
        return new AlterSequenceStatement(getDatabaseType(), null);
    }
    
    @Override
    public ASTNode visitCreateContext(final CreateContextContext ctx) {
        return new XuguCreateContextStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitCreateSPFile(final CreateSPFileContext ctx) {
        return new XuguCreateSPFileStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitCreatePFile(final CreatePFileContext ctx) {
        return new XuguCreatePFileStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitCreateControlFile(final CreateControlFileContext ctx) {
        return new XuguCreateControlFileStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitCreateFlashbackArchive(final CreateFlashbackArchiveContext ctx) {
        return new XuguCreateFlashbackArchiveStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitAlterFlashbackArchive(final AlterFlashbackArchiveContext ctx) {
        return new XuguAlterFlashbackArchiveStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitDropFlashbackArchive(final DropFlashbackArchiveContext ctx) {
        return new XuguDropFlashbackArchiveStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitCreateDiskgroup(final CreateDiskgroupContext ctx) {
        return new XuguCreateDiskgroupStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitDropDiskgroup(final DropDiskgroupContext ctx) {
        return new XuguDropDiskgroupStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitCreateRollbackSegment(final CreateRollbackSegmentContext ctx) {
        return new XuguCreateRollbackSegmentStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitDropRollbackSegment(final DropRollbackSegmentContext ctx) {
        return new XuguDropRollbackSegmentStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitDropTableSpace(final DropTableSpaceContext ctx) {
        return new DropTablespaceStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitCreateLockdownProfile(final CreateLockdownProfileContext ctx) {
        return new XuguCreateLockdownProfileStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitDropLockdownProfile(final DropLockdownProfileContext ctx) {
        return new XuguDropLockdownProfileStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitCreateInmemoryJoinGroup(final CreateInmemoryJoinGroupContext ctx) {
        return new XuguCreateInMemoryJoinGroupStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitAlterInmemoryJoinGroup(final AlterInmemoryJoinGroupContext ctx) {
        return new XuguAlterInMemoryJoinGroupStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitDropInmemoryJoinGroup(final DropInmemoryJoinGroupContext ctx) {
        return new XuguDropInMemoryJoinGroupStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitCreateRestorePoint(final CreateRestorePointContext ctx) {
        return new XuguCreateRestorePointStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitDropRestorePoint(final DropRestorePointContext ctx) {
        return new XuguDropRestorePointStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitAlterOperator(final AlterOperatorContext ctx) {
        return new AlterOperatorStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitAlterProfile(final AlterProfileContext ctx) {
        return new XuguAlterProfileStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitAlterRollbackSegment(final AlterRollbackSegmentContext ctx) {
        return new XuguAlterRollbackSegmentStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitDropOperator(final DropOperatorContext ctx) {
        return new DropOperatorStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitDropSequence(final DropSequenceContext ctx) {
        return new DropSequenceStatement(getDatabaseType(), Collections.emptyList());
    }
    
    @Override
    public ASTNode visitAlterLibrary(final AlterLibraryContext ctx) {
        return new XuguAlterLibraryStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitDropType(final DropTypeContext ctx) {
        return new DropPackageStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitAlterMaterializedZonemap(final AlterMaterializedZonemapContext ctx) {
        return new XuguAlterMaterializedZoneMapStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitAlterJava(final AlterJavaContext ctx) {
        return new XuguAlterJavaStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitAlterAuditPolicy(final AlterAuditPolicyContext ctx) {
        return new XuguAlterAuditPolicyStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitAlterCluster(final AlterClusterContext ctx) {
        return new XuguAlterClusterStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitAlterDiskgroup(final AlterDiskgroupContext ctx) {
        return new XuguAlterDiskgroupStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitAlterIndexType(final AlterIndexTypeContext ctx) {
        return new XuguAlterIndexTypeStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitAlterMaterializedView(final AlterMaterializedViewContext ctx) {
        return new AlterMaterializedViewStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitAlterMaterializedViewLog(final AlterMaterializedViewLogContext ctx) {
        return new XuguAlterMaterializedViewLogStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitAlterFunction(final AlterFunctionContext ctx) {
        return new AlterFunctionStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitAlterHierarchy(final AlterHierarchyContext ctx) {
        return new XuguAlterHierarchyStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitAlterLockdownProfile(final AlterLockdownProfileContext ctx) {
        return new XuguAlterLockdownProfileStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitAlterPluggableDatabase(final AlterPluggableDatabaseContext ctx) {
        return new XuguAlterPluggableDatabaseStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitCreateProcedure(final CreateProcedureContext ctx) {
        return visitCreateProcedure0(ctx);
    }
    
    private ASTNode visitCreateProcedure0(final CreateProcedureContext ctx) {
        if (null != ctx.plsqlProcedureSource().parameterDeclaration()) {
            for (ParameterDeclarationContext each : ctx.plsqlProcedureSource().parameterDeclaration()) {
                visit(each);
            }
        }
        if (null != ctx.plsqlProcedureSource().declareSection()) {
            visit(ctx.plsqlProcedureSource().declareSection());
        }
        if (null != ctx.plsqlProcedureSource().body()) {
            visit(ctx.plsqlProcedureSource().body());
        }
        getSqlStatementsInPlsql().sort(Comparator.comparingInt(SQLStatementSegment::getStartIndex));
        getProcedureCallNames().sort(Comparator.comparingInt(ProcedureCallNameSegment::getStartIndex));
        getDynamicSqlStatementExpressions().sort(Comparator.comparingInt(ExpressionSegment::getStartIndex));
        XuguCreateProcedureStatement result = new XuguCreateProcedureStatement(getDatabaseType());
        result.getProcedureCallNames().addAll(getProcedureCallNames());
        result.getProcedureBodyEndNameSegments().addAll(getProcedureBodyEndNameSegments());
        result.getDynamicSqlStatementExpressions().addAll(getDynamicSqlStatementExpressions());
        result.setProcedureName(visitProcedureName(ctx.plsqlProcedureSource()));
        result.getSqlStatements().addAll(getSqlStatementsInPlsql());
        result.getVariableNames().addAll(getVariableNames());
        getSqlStatementsInPlsql().forEach(each -> each.getSqlStatement().getVariableNames().addAll(getVariableNames()));
        result.getCursorForLoopStatements().addAll(getCursorForLoopStatementSegments());
        return result;
    }
    
    @Override
    public ASTNode visitParameterDeclaration(final ParameterDeclarationContext ctx) {
        if (null != ctx.parameterName()) {
            IdentifierValue paramName = (IdentifierValue) visit(ctx.parameterName().identifier());
            getVariableNames().add(paramName.getValue().toLowerCase());
            return new VariableSegment(ctx.start.getStartIndex(), ctx.stop.getStopIndex(), paramName.getValue());
        }
        return super.visitParameterDeclaration(ctx);
    }
    
    @Override
    public ASTNode visitItemDeclaration(final ItemDeclarationContext ctx) {
        CollectionValue<VariableSegment> result = new CollectionValue<>();
        if (null != ctx.collectionVariableDecl() && null != ctx.collectionVariableDecl().variableName()) {
            for (VariableNameContext each : ctx.collectionVariableDecl().variableName()) {
                getVariableSegment(each).ifPresent(optional -> result.getValue().add(optional));
            }
        }
        if (null != ctx.constantDeclaration() && null != ctx.constantDeclaration().variableName()) {
            getVariableSegment(ctx.constantDeclaration().variableName()).ifPresent(optional -> result.getValue().add(optional));
        }
        if (null != ctx.cursorVariableDeclaration() && null != ctx.cursorVariableDeclaration().variableName()) {
            getVariableSegment(ctx.cursorVariableDeclaration().variableName()).ifPresent(optional -> result.getValue().add(optional));
        }
        if (null != ctx.exceptionDeclaration() && null != ctx.exceptionDeclaration().variableName()) {
            getVariableSegment(ctx.exceptionDeclaration().variableName()).ifPresent(optional -> result.getValue().add(optional));
        }
        if (null != ctx.recordVariableDeclaration() && null != ctx.recordVariableDeclaration().variableName()) {
            getVariableSegment(ctx.recordVariableDeclaration().variableName()).ifPresent(optional -> result.getValue().add(optional));
        }
        if (null != ctx.variableDeclaration() && null != ctx.variableDeclaration().variableName()) {
            getVariableSegment(ctx.variableDeclaration().variableName()).ifPresent(optional -> result.getValue().add(optional));
        }
        return result;
    }
    
    private Optional<VariableSegment> getVariableSegment(final VariableNameContext variableNameContext) {
        if (null == variableNameContext) {
            return Optional.empty();
        }
        if (null != variableNameContext.identifier()) {
            String variableName = ((IdentifierValue) visitIdentifier(variableNameContext.identifier())).getValue().toLowerCase();
            getVariableNames().add(variableName);
            return Optional.of(new VariableSegment(variableNameContext.start.getStartIndex(), variableNameContext.stop.getStopIndex(), variableName));
        }
        if (null != variableNameContext.stringLiterals()) {
            String variableName = variableNameContext.stringLiterals().STRING_().getText().toLowerCase();
            getVariableNames().add(variableName);
            return Optional.of(new VariableSegment(variableNameContext.start.getStartIndex(), variableNameContext.stop.getStopIndex(), variableName));
        }
        return Optional.empty();
    }
    
    @Override
    public ASTNode visitCollectionVariableDecl(final CollectionVariableDeclContext ctx) {
        if (null == ctx.variableName()) {
            return super.visitCollectionVariableDecl(ctx);
        }
        CollectionValue<VariableSegment> result = new CollectionValue<>();
        for (VariableNameContext each : ctx.variableName()) {
            getVariableSegment(each).ifPresent(optional -> result.getValue().add(optional));
        }
        return result;
    }
    
    private FunctionNameSegment visitProcedureName(final PlsqlProcedureSourceContext ctx) {
        SchemaNameContext schemaName = ctx.schemaName();
        IdentifierValue procedureName = (IdentifierValue) visit(ctx.procedureName().identifier());
        if (null == schemaName) {
            return new FunctionNameSegment(ctx.procedureName().start.getStartIndex(), ctx.procedureName().stop.getStopIndex(), procedureName);
        }
        OwnerSegment owner = new OwnerSegment(schemaName.start.getStartIndex(), schemaName.stop.getStopIndex(), (IdentifierValue) visit(schemaName.identifier()));
        FunctionNameSegment result = new FunctionNameSegment(schemaName.start.getStartIndex(), ctx.procedureName().stop.getStopIndex(), procedureName);
        result.setOwner(owner);
        return result;
    }
    
    @Override
    public ASTNode visitCursorDefinition(final CursorDefinitionContext ctx) {
        SQLStatement sqlStatement = visitSelect0(ctx.select());
        getCursorStatements().put(null != ctx.variableName().identifier()
                ? new IdentifierValue(ctx.variableName().getText()).getValue()
                : new StringLiteralValue(ctx.variableName().getText()).getValue(), sqlStatement);
        return defaultResult();
    }
    
    @Override
    public ASTNode visitBody(final BodyContext ctx) {
        for (StatementContext each : ctx.statement()) {
            visit(each);
        }
        for (ExceptionHandlerContext eachExceptionHandler : ctx.exceptionHandler()) {
            for (StatementContext each : eachExceptionHandler.statement()) {
                visit(each);
            }
        }
        if (null != ctx.identifier()) {
            getProcedureBodyEndNameSegments().add(
                    new ProcedureBodyEndNameSegment(ctx.identifier().getStart().getStartIndex(), ctx.identifier().getStop().getStopIndex(), new IdentifierValue(ctx.identifier().getText())));
        }
        return defaultResult();
    }
    
    @Override
    public ASTNode visitProcedureCall(final ProcedureCallContext ctx) {
        int startIndex = ctx.procedureName().start.getStartIndex();
        PackageSegment packageSegment = null;
        if (null != ctx.packageName()) {
            startIndex = ctx.packageName().start.getStartIndex();
            packageSegment = (PackageSegment) visit(ctx.packageName());
        }
        ProcedureCallNameSegment result = new ProcedureCallNameSegment(startIndex, ctx.procedureName().stop.getStopIndex(), (IdentifierValue) visit(ctx.procedureName().identifier()));
        result.setPackageSegment(packageSegment);
        getProcedureCallNames().add(result);
        return defaultResult();
    }
    
    @Override
    public ASTNode visitCursorForLoopStatement(final CursorForLoopStatementContext ctx) {
        SQLStatement relatedCursorStatement;
        String cursorName = null;
        if (null != ctx.select()) {
            relatedCursorStatement = visitSelect0(ctx.select());
        } else {
            cursorName = null == ctx.cursor().variableName().identifier()
                    ? new StringLiteralValue(ctx.cursor().getText()).getValue()
                    : new IdentifierValue(ctx.cursor().getText()).getValue();
            relatedCursorStatement = getCursorStatements().get(cursorName);
        }
        increaseCursorForLoopLevel();
        for (StatementContext each : ctx.statement()) {
            visit(each);
        }
        Set<SQLStatement> sqlStatements = getTempCursorForLoopStatements().remove(getCursorForLoopLevel());
        CursorForLoopStatementSegment cursorForLoopStatementSegment = new CursorForLoopStatementSegment(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex(),
                new IdentifierValue(ctx.record().getText()).getValue(), cursorName, relatedCursorStatement, null == sqlStatements ? Collections.emptyList() : sqlStatements);
        getCursorForLoopStatementSegments().add(cursorForLoopStatementSegment);
        decreaseCursorForLoopLevel();
        return defaultResult();
    }
    
    @Override
    public ASTNode visitOpenForStatement(final OpenForStatementContext ctx) {
        if (null != ctx.select()) {
            visitSelect0(ctx.select());
        }
        // TODO handle SQL in dynamicString
        return defaultResult();
    }
    
    private SQLStatement visitSelect0(final SelectContext select) {
        XuguStatementVisitor visitor = createXuguDMLStatementVisitor();
        SQLStatement result = (SQLStatement) visitor.visitSelect(select);
        getSqlStatementsInPlsql().add(new SQLStatementSegment(select.start.getStartIndex(), select.stop.getStopIndex(), result));
        addToTempCursorForLoopStatements(result);
        return result;
    }
    
    private void addToTempCursorForLoopStatements(final SQLStatement sqlStatement) {
        if (0 == getCursorForLoopLevel()) {
            return;
        }
        for (int i = 1; i <= getCursorForLoopLevel(); i++) {
            getTempCursorForLoopStatements().computeIfAbsent(i, key -> new LinkedHashSet<>()).add(sqlStatement);
        }
    }
    
    @Override
    public ASTNode visitSqlStatementInPlsql(final SqlStatementInPlsqlContext ctx) {
        if (null != ctx.commit()) {
            XuguStatementVisitor visitor = createXuguTCLStatementVisitor();
            SQLStatement result = (SQLStatement) visitor.visitCommit(ctx.commit());
            getSqlStatementsInPlsql().add(new SQLStatementSegment(ctx.commit().start.getStartIndex(), ctx.commit().stop.getStopIndex(), result));
            addToTempCursorForLoopStatements(result);
        }
        // TODO visit collection_method_call
        if (null != ctx.delete()) {
            XuguStatementVisitor visitor = createXuguDMLStatementVisitor();
            SQLStatement result = (SQLStatement) visitor.visitDelete(ctx.delete());
            getSqlStatementsInPlsql().add(new SQLStatementSegment(ctx.delete().start.getStartIndex(), ctx.delete().stop.getStopIndex(), result));
            addToTempCursorForLoopStatements(result);
        }
        if (null != ctx.insert()) {
            XuguStatementVisitor visitor = createXuguDMLStatementVisitor();
            SQLStatement result = (SQLStatement) visitor.visitInsert(ctx.insert());
            getSqlStatementsInPlsql().add(new SQLStatementSegment(ctx.insert().start.getStartIndex(), ctx.insert().stop.getStopIndex(), result));
            addToTempCursorForLoopStatements(result);
        }
        if (null != ctx.lock()) {
            XuguStatementVisitor visitor = createXuguDMLStatementVisitor();
            SQLStatement result = (SQLStatement) visitor.visitLock(ctx.lock());
            getSqlStatementsInPlsql().add(new SQLStatementSegment(ctx.lock().start.getStartIndex(), ctx.lock().stop.getStopIndex(), result));
            addToTempCursorForLoopStatements(result);
        }
        if (null != ctx.merge()) {
            XuguStatementVisitor visitor = createXuguDMLStatementVisitor();
            SQLStatement result = (SQLStatement) visitor.visitMerge(ctx.merge());
            getSqlStatementsInPlsql().add(new SQLStatementSegment(ctx.merge().start.getStartIndex(), ctx.merge().stop.getStopIndex(), result));
            addToTempCursorForLoopStatements(result);
        }
        if (null != ctx.rollback()) {
            XuguStatementVisitor visitor = createXuguTCLStatementVisitor();
            SQLStatement result = (SQLStatement) visitor.visitRollback(ctx.rollback());
            getSqlStatementsInPlsql().add(new SQLStatementSegment(ctx.rollback().start.getStartIndex(), ctx.rollback().stop.getStopIndex(), result));
            addToTempCursorForLoopStatements(result);
        }
        if (null != ctx.savepoint()) {
            XuguStatementVisitor visitor = createXuguTCLStatementVisitor();
            SQLStatement result = (SQLStatement) visitor.visitSavepoint(ctx.savepoint());
            getSqlStatementsInPlsql().add(new SQLStatementSegment(ctx.savepoint().start.getStartIndex(), ctx.savepoint().stop.getStopIndex(), result));
            addToTempCursorForLoopStatements(result);
        }
        if (null != ctx.setTransaction()) {
            XuguStatementVisitor visitor = createXuguTCLStatementVisitor();
            SQLStatement result = (SQLStatement) visitor.visitSetTransaction(ctx.setTransaction());
            getSqlStatementsInPlsql().add(new SQLStatementSegment(ctx.setTransaction().start.getStartIndex(), ctx.setTransaction().stop.getStopIndex(), result));
            addToTempCursorForLoopStatements(result);
        }
        if (null != ctx.update()) {
            XuguStatementVisitor visitor = createXuguDMLStatementVisitor();
            SQLStatement result = (SQLStatement) visitor.visitUpdate(ctx.update());
            getSqlStatementsInPlsql().add(new SQLStatementSegment(ctx.update().start.getStartIndex(), ctx.update().stop.getStopIndex(), result));
            addToTempCursorForLoopStatements(result);
        }
        return defaultResult();
    }
    
    private XuguStatementVisitor createXuguTCLStatementVisitor() {
        XuguStatementVisitor result = new XuguTCLStatementVisitor(getDatabaseType());
        result.getVariableNames().addAll(getVariableNames());
        return result;
    }
    
    private XuguStatementVisitor createXuguDMLStatementVisitor() {
        XuguStatementVisitor result = new XuguDMLStatementVisitor(getDatabaseType());
        result.getVariableNames().addAll(getVariableNames());
        return result;
    }
    
    @Override
    public ASTNode visitDmlStatement(final DmlStatementContext ctx) {
        if (null != ctx.insert()) {
            XuguStatementVisitor visitor = createXuguDMLStatementVisitor();
            SQLStatement result = (SQLStatement) visitor.visitInsert(ctx.insert());
            getSqlStatementsInPlsql().add(new SQLStatementSegment(ctx.insert().start.getStartIndex(), ctx.insert().stop.getStopIndex(), result));
            addToTempCursorForLoopStatements(result);
        }
        if (null != ctx.update()) {
            XuguStatementVisitor visitor = createXuguDMLStatementVisitor();
            SQLStatement result = (SQLStatement) visitor.visitUpdate(ctx.update());
            getSqlStatementsInPlsql().add(new SQLStatementSegment(ctx.update().start.getStartIndex(), ctx.update().stop.getStopIndex(), result));
            addToTempCursorForLoopStatements(result);
        }
        if (null != ctx.delete()) {
            XuguStatementVisitor visitor = createXuguDMLStatementVisitor();
            SQLStatement result = (SQLStatement) visitor.visitDelete(ctx.delete());
            getSqlStatementsInPlsql().add(new SQLStatementSegment(ctx.delete().start.getStartIndex(), ctx.delete().stop.getStopIndex(), result));
            addToTempCursorForLoopStatements(result);
        }
        if (null != ctx.merge()) {
            XuguStatementVisitor visitor = createXuguDMLStatementVisitor();
            SQLStatement result = (SQLStatement) visitor.visitMerge(ctx.merge());
            getSqlStatementsInPlsql().add(new SQLStatementSegment(ctx.merge().start.getStartIndex(), ctx.merge().stop.getStopIndex(), result));
            addToTempCursorForLoopStatements(result);
        }
        // TODO Handling dynamicSqlStmt if we can
        return defaultResult();
    }
    
    @Override
    public ASTNode visitSelectIntoStatement(final SelectIntoStatementContext ctx) {
        // TODO Visit intoClause
        XuguStatementVisitor visitor = createXuguDMLStatementVisitor();
        SelectStatement result = (SelectStatement) visitor.visitSelectIntoStatement(ctx);
        getSqlStatementsInPlsql().add(new SQLStatementSegment(ctx.start.getStartIndex(), ctx.stop.getStopIndex(), result));
        addToTempCursorForLoopStatements(result);
        return result;
    }
    
    @Override
    public ASTNode visitDynamicSqlStmt(final DynamicSqlStmtContext ctx) {
        ExpressionSegment result = (ExpressionSegment) visit(ctx.expression().expr());
        getDynamicSqlStatementExpressions().add(result);
        return result;
    }
    
    @Override
    public ASTNode visitPlsqlBlock(final PlsqlBlockContext ctx) {
        if (null != ctx.body() && null != ctx.body().statement()) {
            ctx.body().statement().forEach(this::visit);
        }
        return new XuguPLSQLBlockStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitAlterProcedure(final AlterProcedureContext ctx) {
        return new AlterProcedureStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitDropProcedure(final DropProcedureContext ctx) {
        return new DropProcedureStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitDropIndexType(final DropIndexTypeContext ctx) {
        return new XuguDropIndexTypeStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitDropProfile(final DropProfileContext ctx) {
        return new XuguDropProfileStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitDropPluggableDatabase(final DropPluggableDatabaseContext ctx) {
        return new XuguDropPluggableDatabaseStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitDropJava(final DropJavaContext ctx) {
        return new XuguDropJavaStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitDropFunction(final DropFunctionContext ctx) {
        return new DropFunctionStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitDropLibrary(final DropLibraryContext ctx) {
        return new XuguDropLibraryStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitDropCluster(final DropClusterContext ctx) {
        return new XuguDropClusterStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitDropMaterializedView(final DropMaterializedViewContext ctx) {
        return new DropMaterializedViewStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitDropMaterializedViewLog(final DropMaterializedViewLogContext ctx) {
        return new XuguDropMaterializedViewLogStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitDropMaterializedZonemap(final DropMaterializedZonemapContext ctx) {
        return new XuguDropMaterializedZoneMapStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitCreateTablespace(final CreateTablespaceContext ctx) {
        return new CreateTablespaceStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitCreateMaterializedView(final CreateMaterializedViewContext ctx) {
        return new CreateMaterializedViewStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitCreateMaterializedViewLog(final CreateMaterializedViewLogContext ctx) {
        return new XuguCreateMaterializedViewLogStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitCreateCluster(final CreateClusterContext ctx) {
        return new XuguCreateClusterStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitSystemAction(final SystemActionContext ctx) {
        return new XuguSystemActionStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitAlterType(final AlterTypeContext ctx) {
        return new AlterTypeStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitCreateJava(final CreateJavaContext ctx) {
        return new XuguCreateJavaStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitCreateLibrary(final CreateLibraryContext ctx) {
        return new XuguCreateLibraryStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitSwitch(final SwitchContext ctx) {
        return new XuguSwitchStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitCreateProfile(final CreateProfileContext ctx) {
        return new XuguCreateProfileStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitDropDatabase(final DropDatabaseContext ctx) {
        return new DropDatabaseStatement(getDatabaseType(), null, false);
    }
    
    @Override
    public ASTNode visitCreateOperator(final CreateOperatorContext ctx) {
        return new CreateOperatorStatement(getDatabaseType());
    }
    
    @Override
    public ASTNode visitCreateOutline(final CreateOutlineContext ctx) {
        return new XuguCreateOutlineStatement(getDatabaseType());
    }
}
