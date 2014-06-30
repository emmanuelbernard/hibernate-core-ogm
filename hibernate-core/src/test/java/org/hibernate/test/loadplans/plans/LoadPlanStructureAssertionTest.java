/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.loadplans.plans;

import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.plan.build.internal.returns.CollectionFetchableElementEntityGraph;
import org.hibernate.loader.plan.spi.BidirectionalEntityReference;
import org.hibernate.loader.plan.spi.CollectionAttributeFetch;
import org.hibernate.loader.plan.spi.EntityFetch;
import org.hibernate.loader.plan.spi.EntityReturn;
import org.hibernate.loader.plan.spi.FetchSource;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.FailureExpectedWithNewUnifiedXsd;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.test.annotations.Country;
import org.hibernate.test.annotations.cid.keymanytoone.Card;
import org.hibernate.test.annotations.cid.keymanytoone.CardField;
import org.hibernate.test.annotations.cid.keymanytoone.Key;
import org.hibernate.test.annotations.cid.keymanytoone.PrimaryKey;
import org.hibernate.test.annotations.collectionelement.Boy;
import org.hibernate.test.annotations.collectionelement.Matrix;
import org.hibernate.test.annotations.collectionelement.TestCourse;
import org.hibernate.test.loadplans.process.EncapsulatedCompositeIdResultSetProcessorTest;
import org.junit.Test;

import static junit.framework.Assert.assertNotNull;
import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

//import org.hibernate.loader.plan.spi.BidirectionalEntityFetch;

/**
 * Used to assert that "fetch graphs" between JoinWalker and LoadPlan are same.
 *
 * @author Steve Ebersole
 */
public class LoadPlanStructureAssertionTest extends BaseUnitTestCase {
	@Test
	@FailureExpectedWithNewUnifiedXsd(message = "one-to-one refers to entity-name")
	public void testJoinedOneToOne() {
		// tests the mappings defined in org.hibernate.test.onetoone.joined.JoinedSubclassOneToOneTest

		Configuration cfg = new Configuration();
		cfg.addResource( "org/hibernate/test/onetoone/joined/Person.hbm.xml" );
		SessionFactoryImplementor sf = (SessionFactoryImplementor) cfg.buildSessionFactory();

		try {
			doCompare( sf, (OuterJoinLoadable) sf.getClassMetadata( org.hibernate.test.onetoone.joined.Person.class ) );
			doCompare( sf, (OuterJoinLoadable) sf.getClassMetadata( org.hibernate.test.onetoone.joined.Entity.class ) );

			doCompare( sf, (OuterJoinLoadable) sf.getClassMetadata( org.hibernate.test.onetoone.joined.Address.class ) );
		}
		finally {
			sf.close();
		}
	}

	@Test
	@FailureExpectedWithNewUnifiedXsd(message = "formulas not yet supported in associations")
	public void testSpecialOneToOne() {
		// tests the mappings defined in org.hibernate.test.onetoone.joined.JoinedSubclassOneToOneTest

		Configuration cfg = new Configuration();
		cfg.addResource( "org/hibernate/test/onetoone/formula/Person.hbm.xml" );
		SessionFactoryImplementor sf = (SessionFactoryImplementor) cfg.buildSessionFactory();

		try {
			doCompare( sf, (OuterJoinLoadable) sf.getClassMetadata( org.hibernate.test.onetoone.formula.Person.class ) );
		}
		finally {
			sf.close();
		}
	}

	@Test
	public void testEncapsulatedCompositeIdNoFetches1() {
		// CardField is an entity with a composite identifier mapped via a @EmbeddedId class (CardFieldPK) defining
		// a @ManyToOne

		// metamodel : another indication of @Embedded/@Embeddable problem

		Configuration cfg = new Configuration();
		cfg.addAnnotatedClass( EncapsulatedCompositeIdResultSetProcessorTest.CardField.class );
		cfg.addAnnotatedClass( EncapsulatedCompositeIdResultSetProcessorTest.Card.class );
		SessionFactoryImplementor sf = (SessionFactoryImplementor) cfg.buildSessionFactory();

		try {
			doCompare( sf, (OuterJoinLoadable) sf.getClassMetadata( EncapsulatedCompositeIdResultSetProcessorTest.CardField.class ) );
			doCompare( sf, (OuterJoinLoadable) sf.getClassMetadata( EncapsulatedCompositeIdResultSetProcessorTest.Card.class ) );
		}
		finally {
			sf.close();
		}
	}

	@Test
	public void testEncapsulatedCompositeIdNoFetches2() {
		// Parent is an entity with a composite identifier mapped via a @EmbeddedId class (ParentPK) which is defined
		// using just basic types (strings, ints, etc)

		// metamodel : another indication of @Embedded/@Embeddable problem

		Configuration cfg = new Configuration();
		cfg.addAnnotatedClass( EncapsulatedCompositeIdResultSetProcessorTest.Parent.class );
		SessionFactoryImplementor sf = (SessionFactoryImplementor) cfg.buildSessionFactory();

		try {
			doCompare( sf, (OuterJoinLoadable) sf.getClassMetadata( EncapsulatedCompositeIdResultSetProcessorTest.Parent.class ) );
		}
		finally {
			sf.close();
		}
	}

	@Test
	public void testEncapsulatedCompositeIdWithFetches1() {
		Configuration cfg = new Configuration();
		cfg.addAnnotatedClass( Card.class );
		cfg.addAnnotatedClass( CardField.class );
		cfg.addAnnotatedClass( Key.class );
		cfg.addAnnotatedClass( PrimaryKey.class );

		SessionFactoryImplementor sf = (SessionFactoryImplementor) cfg.buildSessionFactory();

		try {
			final OuterJoinLoadable cardFieldPersister = (OuterJoinLoadable) sf.getClassMetadata( CardField.class );
			doCompare( sf, cardFieldPersister );

			final LoadPlan loadPlan = LoadPlanStructureAssertionHelper.INSTANCE.buildLoadPlan( sf, cardFieldPersister );
			assertEquals( LoadPlan.Disposition.ENTITY_LOADER, loadPlan.getDisposition() );
			assertEquals( 1, loadPlan.getReturns().size() );
			final EntityReturn cardFieldReturn = assertTyping( EntityReturn.class, loadPlan.getReturns().get( 0 ) );
			assertEquals( 0, cardFieldReturn.getFetches().length );

			// CardField defines a composite pk with 2 fetches : Card and Key (the id description acts as the composite)
			assertTrue( cardFieldReturn.getIdentifierDescription().hasFetches() );
			final FetchSource cardFieldIdAsFetchSource = assertTyping( FetchSource.class, cardFieldReturn.getIdentifierDescription() );
			assertEquals( 2, cardFieldIdAsFetchSource.getFetches().length );

			// First the key-many-to-one to Card...
			final EntityFetch cardFieldIdCardFetch = assertTyping(
					EntityFetch.class,
					cardFieldIdAsFetchSource.getFetches()[0]
			);
			assertFalse( cardFieldIdCardFetch.getIdentifierDescription().hasFetches() );
			// i think this one might be a mistake; i think the collection reader still needs to be registered.  Its zero
			// because the inverse of the key-many-to-one already had a registered AssociationKey and so saw the
			// CollectionFetch as a circularity (I think)
			assertEquals( 0, cardFieldIdCardFetch.getFetches().length );

			// then the Key..
			final EntityFetch cardFieldIdKeyFetch = assertTyping(
					EntityFetch.class,
					cardFieldIdAsFetchSource.getFetches()[1]
			);
			assertFalse( cardFieldIdKeyFetch.getIdentifierDescription().hasFetches() );
			assertEquals( 0, cardFieldIdKeyFetch.getFetches().length );


			// we need the readers ordered in a certain manner.  Here specifically: Fetch(Card), Fetch(Key), Return(CardField)
			//
			// additionally, we need Fetch(Card) and Fetch(Key) to be hydrated/semi-resolved before attempting to
			// resolve the EntityKey for Return(CardField)
			//
			// together those sound like argument enough to continue keeping readers for "identifier fetches" as part of
			// a special "identifier reader".  generated aliases could help here too to remove cyclic-ness from the graph.
			// but at any rate, we need to know still when this becomes circularity
		}
		finally {
			sf.close();
		}
	}

	@Test
	@FailureExpectedWithNewMetamodel
	public void testEncapsulatedCompositeIdWithFetches2() {
		Configuration cfg = new Configuration();
		cfg.addAnnotatedClass( Card.class );
		cfg.addAnnotatedClass( CardField.class );
		cfg.addAnnotatedClass( Key.class );
		cfg.addAnnotatedClass( PrimaryKey.class );

		final SessionFactoryImplementor sf = (SessionFactoryImplementor) cfg.buildSessionFactory();

		try {
			final OuterJoinLoadable cardPersister = (OuterJoinLoadable) sf.getClassMetadata( Card.class );
			doCompare( sf, cardPersister );

			final LoadPlan cardLoadPlan = LoadPlanStructureAssertionHelper.INSTANCE.buildLoadPlan( sf, cardPersister );
			assertEquals( LoadPlan.Disposition.ENTITY_LOADER, cardLoadPlan.getDisposition() );
			assertEquals( 1, cardLoadPlan.getReturns().size() );

			// Check the root EntityReturn(Card)
			final EntityReturn cardReturn = assertTyping( EntityReturn.class, cardLoadPlan.getReturns().get( 0 ) );
			assertFalse( cardReturn.getIdentifierDescription().hasFetches() );

			// Card should have one fetch, the fields collection
			assertEquals( 1, cardReturn.getFetches().length );
			final CollectionAttributeFetch fieldsFetch = assertTyping( CollectionAttributeFetch.class, cardReturn.getFetches()[0] );
			assertNotNull( fieldsFetch.getElementGraph() );

			// the Card.fields collection has entity elements of type CardField...
			final CollectionFetchableElementEntityGraph cardFieldElementGraph = assertTyping( CollectionFetchableElementEntityGraph.class, fieldsFetch.getElementGraph() );
			// CardField should have no fetches
			assertEquals( 0, cardFieldElementGraph.getFetches().length );
			// But it should have 1 key-many-to-one fetch for Key (Card is already handled)
			assertTrue( cardFieldElementGraph.getIdentifierDescription().hasFetches() );
			final FetchSource cardFieldElementGraphIdAsFetchSource = assertTyping(
					FetchSource.class,
					cardFieldElementGraph.getIdentifierDescription()
			);
			assertEquals( 1, cardFieldElementGraphIdAsFetchSource.getFetches().length );
			assertEquals( 1, cardFieldElementGraphIdAsFetchSource.getBidirectionalEntityReferences().length );

			BidirectionalEntityReference circularCardFetch = assertTyping(
					BidirectionalEntityReference.class,
					cardFieldElementGraphIdAsFetchSource.getBidirectionalEntityReferences()[0]
			);
			assertSame( circularCardFetch.getTargetEntityReference(), cardReturn );

			// the fetch above is to the other key-many-to-one for CardField.primaryKey composite: key
			EntityFetch keyFetch = assertTyping(
					EntityFetch.class,
					cardFieldElementGraphIdAsFetchSource.getFetches()[0]
			);
			assertEquals( Key.class.getName(), keyFetch.getEntityPersister().getEntityName() );
		}
		finally {
			sf.close();
		}
	}

	@Test
	@FailureExpectedWithNewMetamodel(message = "The mapping seems really off.  Ex: Many of the classes have an inverse "
			+ "<one-to-many class=\"Info\"/> w/ a <key column=\"col_plan\"/>, but the Info class has no associations.")
	public void testManyToMany() {
		Configuration cfg = new Configuration();
		cfg.addResource( "org/hibernate/test/immutable/entitywithmutablecollection/inverse/ContractVariation.hbm.xml" );
		SessionFactoryImplementor sf = (SessionFactoryImplementor) cfg.buildSessionFactory();

		try {
			doCompare( sf, (OuterJoinLoadable) sf.getClassMetadata( org.hibernate.test.immutable.entitywithmutablecollection.Contract.class ) );
		}
		finally {
			sf.close();
		}

	}

	@Test
	@FailureExpectedWithNewMetamodel
	public void testAnotherBasicCollection() {
		// metamodel : another indication of @Embedded/@Embeddable problem

		Configuration cfg = new Configuration();
		cfg.addAnnotatedClass( Boy.class );
		cfg.addAnnotatedClass( Country.class );
		cfg.addAnnotatedClass( TestCourse.class );
		cfg.addAnnotatedClass( Matrix.class );
		SessionFactoryImplementor sf = (SessionFactoryImplementor) cfg.buildSessionFactory();

		try {
			doCompare( sf, (OuterJoinLoadable) sf.getClassMetadata( Boy.class ) );
		}
		finally {
			sf.close();
		}
	}

	private void doCompare(SessionFactoryImplementor sf, OuterJoinLoadable persister) {
		LoadPlanStructureAssertionHelper.INSTANCE.performBasicComparison( sf, persister );
	}
}
