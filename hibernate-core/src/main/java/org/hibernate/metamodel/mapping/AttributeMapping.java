/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.sql.results.graph.DatabaseSnapshotContributor;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.generator.Generator;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.MutabilityPlanExposer;

/**
 * Describes an attribute at the mapping model level.
 *
 * @author Steve Ebersole
 */
public interface AttributeMapping
		extends OwnedValuedModelPart, Fetchable, DatabaseSnapshotContributor, PropertyBasedMapping, MutabilityPlanExposer {
	/**
	 * The name of the mapped attribute
	 */
	String getAttributeName();

	@Override
	default String getPartName() {
		return getAttributeName();
	}

	/**
	 * The attribute's position within the container's state array
	 */
	int getStateArrayPosition();

	/**
	 * Access to AttributeMetadata
	 */
	AttributeMetadata getAttributeMetadata();

	/**
	 * The managed type that declares this attribute
	 */
	ManagedMappingType getDeclaringType();

	/**
	 * The getter/setter access to this attribute
	 */
	PropertyAccess getPropertyAccess();

	/**
	 * Convenient access to getting the value for this attribute from the declarer
	 */
	default Object getValue(Object container) {
		return getPropertyAccess().getGetter().get( container );
	}

	/**
	 * Convenient access to setting the value for this attribute on the declarer
	 */
	default void setValue(Object container, Object value) {
		getPropertyAccess().getSetter().set( container, value );
	}

	/**
	 * The value generation strategy to use for this attribute.
	 *
	 * @apiNote Only relevant for non-id attributes
	 */
	Generator getGenerator();

	@Override
	default EntityMappingType findContainingEntityMapping() {
		return getDeclaringType().findContainingEntityMapping();
	}

	@Override
	default MutabilityPlan<?> getExposedMutabilityPlan() {
		return getAttributeMetadata().getMutabilityPlan();
	}

	default int compare(Object value1, Object value2) {
		//noinspection unchecked,rawtypes
		return ( (JavaType) getJavaType() ).getComparator().compare( value1, value2 );
	}

	@Override //Overrides multiple interfaces!
	default AttributeMapping asAttributeMapping() {
		return this;
	}

	/**
	 * A utility method to avoid casting explicitly to PluralAttributeMapping
	 *
	 * @return PluralAttributeMapping if this is an instance of PluralAttributeMapping otherwise {@code null}
	 */
	default PluralAttributeMapping asPluralAttributeMapping() {
		return null;
	}

	default boolean isPluralAttributeMapping() {
		return false;
	}

	/**
	 * A utility method to avoid casting explicitly to EmbeddedAttributeMapping
	 *
	 * @return EmbeddedAttributeMapping if this is an instance of EmbeddedAttributeMapping otherwise {@code null}
	 */
	default EmbeddedAttributeMapping asEmbeddedAttributeMapping(){
		return null;
	}

	default boolean isEmbeddedAttributeMapping(){
		return false;
	}

}
