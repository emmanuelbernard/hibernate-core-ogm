/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.hql.spi;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.internal.ast.tree.DeleteStatement;
import org.hibernate.hql.internal.ast.tree.FromElement;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.param.ParameterSpecification;
import org.hibernate.persister.collection.AbstractCollectionPersister;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.Delete;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

/**
* @author Steve Ebersole
*/
public class TableBasedDeleteHandlerImpl
		extends AbstractTableBasedBulkIdHandler
		implements MultiTableBulkIdStrategy.DeleteHandler {
	private static final Logger log = Logger.getLogger( TableBasedDeleteHandlerImpl.class );

	private final Queryable targetedPersister;

	private final String idInsertSelect;
	private final List<ParameterSpecification> idSelectParameterSpecifications;
	private final List<String> deletes;

	public TableBasedDeleteHandlerImpl(SessionFactoryImplementor factory, HqlSqlWalker walker) {
		this( factory, walker, null, null );
	}

	public TableBasedDeleteHandlerImpl(
			SessionFactoryImplementor factory,
			HqlSqlWalker walker,
			String catalog,
			String schema) {
		super( factory, walker, catalog, schema );

		DeleteStatement deleteStatement = ( DeleteStatement ) walker.getAST();
		FromElement fromElement = deleteStatement.getFromClause().getFromElement();

		this.targetedPersister = fromElement.getQueryable();
		final String bulkTargetAlias = fromElement.getTableAlias();

		final ProcessedWhereClause processedWhereClause = processWhereClause( deleteStatement.getWhereClause() );
		this.idSelectParameterSpecifications = processedWhereClause.getIdSelectParameterSpecifications();
		this.idInsertSelect = generateIdInsertSelect( targetedPersister, bulkTargetAlias, processedWhereClause );
		log.tracev( "Generated ID-INSERT-SELECT SQL (multi-table delete) : {0}", idInsertSelect );
		
		final String idSubselect = generateIdSubselect( targetedPersister );
		deletes = new ArrayList<String>();
		
		// find plural attributes defined for the entity being deleted...
		//
		// NOTE : this partially overlaps with DeleteExecutor, but it instead
		// 		uses the temp table in the idSubselect.
		for ( Type type : targetedPersister.getPropertyTypes() ) {
			if ( ! type.isCollectionType() ) {
				continue;
			}

			final CollectionType cType = (CollectionType) type;
			final AbstractCollectionPersister cPersister = (AbstractCollectionPersister)factory.getCollectionPersister( cType.getRole() );

			// if the plural attribute maps to a "collection table" we need
			// to remove the rows from that table corresponding to any
			// owners we are about to delete.  "collection table" is
			// (unfortunately) indicated in a number of ways, but here we
			// are mainly concerned with:
			//		1) many-to-many mappings
			//		2) basic collection mappings
			final boolean hasCollectionTable = cPersister.isManyToMany()
					|| !cPersister.getElementType().isAssociationType();
			if ( !hasCollectionTable ) {
				continue;
			}

			deletes.add(
					generateDelete(
							cPersister.getTableName(),
							cPersister.getKeyColumnNames(),
							idSubselect,
							"bulk delete - collection table clean up (" + cPersister.getRole() + ")"
					)
			);
		}

		String[] tableNames = targetedPersister.getConstraintOrderedTableNameClosure();
		String[][] columnNames = targetedPersister.getContraintOrderedTableKeyColumnClosure();
		for ( int i = 0; i < tableNames.length; i++ ) {
			// TODO : an optimization here would be to consider cascade deletes and not gen those delete statements;
			//      the difficulty is the ordering of the tables here vs the cascade attributes on the persisters ->
			//          the table info gotten here should really be self-contained (i.e., a class representation
			//          defining all the needed attributes), then we could then get an array of those
			deletes.add( generateDelete( tableNames[i], columnNames[i], idSubselect, "bulk delete" ) );
		}
	}
	
	private String generateDelete(String tableName, String[] columnNames, String idSubselect, String comment) {
		final Delete delete = new Delete()
				.setTableName( tableName )
				.setWhere( "(" + StringHelper.join( ", ", columnNames ) + ") IN (" + idSubselect + ")" );
		if ( factory().getSettings().isCommentsEnabled() ) {
			delete.setComment( comment );
		}
		return delete.toStatementString();
	}

	@Override
	public Queryable getTargetedQueryable() {
		return targetedPersister;
	}

	@Override
	public String[] getSqlStatements() {
		return deletes.toArray( new String[deletes.size()] );
	}

	@Override
	public int execute(SessionImplementor session, QueryParameters queryParameters) {
		prepareForUse( targetedPersister, session );
		try {
			PreparedStatement ps = null;
			int resultCount = 0;
			try {
				try {
					ps = session.getTransactionCoordinator().getJdbcCoordinator().getStatementPreparer().prepareStatement( idInsertSelect, false );
					int pos = 1;
					pos += handlePrependedParametersOnIdSelection( ps, session, pos );
					for ( ParameterSpecification parameterSpecification : idSelectParameterSpecifications ) {
						pos += parameterSpecification.bind( ps, queryParameters, session, pos );
					}
					resultCount = session.getTransactionCoordinator().getJdbcCoordinator().getResultSetReturn().executeUpdate( ps );
				}
				finally {
					if ( ps != null ) {
						session.getTransactionCoordinator().getJdbcCoordinator().release( ps );
					}
				}
			}
			catch( SQLException e ) {
				throw convert( e, "could not insert/select ids for bulk delete", idInsertSelect );
			}

			// Start performing the deletes
			for ( String delete : deletes ) {
				try {
					try {
						ps = session.getTransactionCoordinator()
								.getJdbcCoordinator()
								.getStatementPreparer()
								.prepareStatement( delete, false );
						handleAddedParametersOnDelete( ps, session );
						session.getTransactionCoordinator().getJdbcCoordinator().getResultSetReturn().executeUpdate( ps );
					}
					finally {
						if ( ps != null ) {
							session.getTransactionCoordinator().getJdbcCoordinator().release( ps );
						}
					}
				}
				catch (SQLException e) {
					throw convert( e, "error performing bulk delete", delete );
				}
			}

			return resultCount;

		}
		finally {
			releaseFromUse( targetedPersister, session );
		}
	}

	protected int handlePrependedParametersOnIdSelection(PreparedStatement ps, SessionImplementor session, int pos) throws SQLException {
		return 0;
	}

	protected void handleAddedParametersOnDelete(PreparedStatement ps, SessionImplementor session) throws SQLException {
	}
}
