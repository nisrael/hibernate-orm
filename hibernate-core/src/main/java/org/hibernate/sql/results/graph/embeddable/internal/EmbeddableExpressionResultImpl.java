/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.embeddable.internal;

import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.results.graph.AbstractFetchParent;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.embeddable.EmbeddableInitializer;
import org.hibernate.sql.results.graph.embeddable.EmbeddableResult;
import org.hibernate.sql.results.graph.embeddable.EmbeddableResultGraphNode;
import org.hibernate.sql.results.graph.internal.ImmutableFetchList;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class EmbeddableExpressionResultImpl<T> extends AbstractFetchParent implements EmbeddableResultGraphNode, DomainResult<T>, EmbeddableResult<T> {
	private final String resultVariable;
	private final boolean containsAnyNonScalars;

	public EmbeddableExpressionResultImpl(
			NavigablePath navigablePath,
			EmbeddableValuedModelPart modelPart,
			SqlTuple sqlExpression,
			String resultVariable,
			DomainResultCreationState creationState) {
		super( modelPart.getEmbeddableTypeDescriptor(), navigablePath );
		this.resultVariable = resultVariable;

		final ImmutableFetchList.Builder fetches = new ImmutableFetchList.Builder( modelPart );
		final EmbeddableMappingType mappingType = modelPart.getEmbeddableTypeDescriptor();
		final int numberOfAttributeMappings = mappingType.getNumberOfAttributeMappings();
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		final TypeConfiguration typeConfiguration = sqlAstCreationState.getCreationContext()
				.getSessionFactory()
				.getTypeConfiguration();
		final SqlExpressionResolver sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();
		for ( int i = 0; i < numberOfAttributeMappings; i++ ) {
			final BasicAttributeMapping attribute = (BasicAttributeMapping) mappingType.getAttributeMapping( i );
			final SqlSelection sqlSelection = sqlExpressionResolver.resolveSqlSelection(
					sqlExpression.getExpressions().get( i ),
					attribute.getJavaType(),
					this,
					typeConfiguration
			);
			fetches.add(
					new BasicFetch<>(
							sqlSelection.getValuesArrayPosition(),
							this,
							resolveNavigablePath( attribute ),
							attribute,
							FetchTiming.IMMEDIATE,
							creationState
					)
			);
		}

		resetFetches( fetches.build() );
		this.containsAnyNonScalars = determineIfContainedAnyScalars( getFetches() );
	}

	private static boolean determineIfContainedAnyScalars(ImmutableFetchList fetches) {
		for ( Fetch fetch : fetches ) {
			if ( fetch.containsAnyNonScalarResults() ) {
				return true;
			}
		}

		return false;
	}

	@Override
	public String getResultVariable() {
		return resultVariable;
	}

	@Override
	public boolean containsAnyNonScalarResults() {
		return containsAnyNonScalars;
	}

	@Override
	public EmbeddableMappingType getFetchContainer() {
		return (EmbeddableMappingType) super.getFetchContainer();
	}

	@Override
	public JavaType<?> getResultJavaType() {
		return getReferencedMappingType().getJavaType();
	}

	@Override
	public EmbeddableMappingType getReferencedMappingType() {
		return getFetchContainer();
	}

	@Override
	public EmbeddableValuedModelPart getReferencedMappingContainer() {
		return getFetchContainer().getEmbeddedValueMapping();
	}

	@Override
	public DomainResultAssembler<T> createResultAssembler(
			FetchParentAccess parentAccess,
			AssemblerCreationState creationState) {
		final EmbeddableInitializer initializer = creationState.resolveInitializer(
				getNavigablePath(),
				getReferencedModePart(),
				() -> new EmbeddableResultInitializer(
						this,
						parentAccess,
						creationState
				)
		).asEmbeddableInitializer();

		assert initializer != null;

		//noinspection unchecked
		return new EmbeddableAssembler( initializer );
	}
}
