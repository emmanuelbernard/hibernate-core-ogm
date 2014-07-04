/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.testing.junit4;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.testing.DialectCheck;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.FailureExpectedUtil;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.FailureExpectedWithNewUnifiedXsd;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.RequiresDialects;
import org.hibernate.testing.Skip;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.SkipForDialects;
import org.jboss.logging.Logger;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

/**
 * The Hibernate-specific {@link org.junit.runner.Runner} implementation which layers {@link ExtendedFrameworkMethod}
 * support on top of the standard JUnit {@link FrameworkMethod} for extra information after checking to make sure the
 * test should be run.
 *
 * @author Steve Ebersole
 */
public class CustomRunner extends BlockJUnit4ClassRunner {
	private static final Logger log = Logger.getLogger( CustomRunner.class );

	private TestClassMetadata testClassMetadata;
	private boolean beforeClassMethodFailed;

	public CustomRunner(Class<?> clazz) throws InitializationError, NoTestsRemainException {
		super( clazz );
	}

	@Override
	protected void collectInitializationErrors(List<Throwable> errors) {
		super.collectInitializationErrors( errors );
		this.testClassMetadata = new TestClassMetadata( getTestClass().getJavaClass() );
		testClassMetadata.validate( errors );
	}

	boolean beforeClassMethodFailed() {
		return beforeClassMethodFailed;
	}

	void setBeforeClassMethodFailed() {
		beforeClassMethodFailed = true;
	}

	public TestClassMetadata getTestClassMetadata() {
		return testClassMetadata;
	}

    private Boolean isAllTestsIgnored;

    private boolean isAllTestsIgnored() {
        if ( isAllTestsIgnored == null ) {
            if ( computeTestMethods().isEmpty() ) {
                isAllTestsIgnored = true;
            }
            else {
                isAllTestsIgnored = true;
                for ( FrameworkMethod method : computeTestMethods() ) {
                    Ignore ignore = method.getAnnotation( Ignore.class );
                    if ( ignore == null ) {
                        isAllTestsIgnored = false;
                        break;
                    }
                }
            }
        }
        return isAllTestsIgnored;
    }

    @Override
    protected Statement withBeforeClasses(Statement statement) {
        if ( isAllTestsIgnored() ) {
            return super.withBeforeClasses( statement );
        }
        return new BeforeClassCallbackHandler(
                this,
                super.withBeforeClasses( statement )
        );
    }

	@Override
	protected Statement withAfterClasses(Statement statement) {
        if ( isAllTestsIgnored() ) {
            return super.withAfterClasses( statement );
        }
		return new AfterClassCallbackHandler(
				this,
				super.withAfterClasses( statement )
		);
	}

	@Override
	protected Statement classBlock( RunNotifier notifier ) {
		log.info( BeforeClass.class.getSimpleName() + ": " + getName() );

		if ( FailureExpectedUtil.hasFailureExpectedMarker( getTestClass().getJavaClass().getAnnotations() ) ) {
			log.info( FailureExpected.class.getSimpleName() );
		}

		return super.classBlock( notifier );
	}

	@Override
	protected Statement methodBlock(FrameworkMethod method) {
		log.info( Test.class.getSimpleName() + ": " + method.getName() );

		if ( FailureExpectedUtil.hasFailureExpectedMarker( method.getAnnotations() ) ) {
			log.info( FailureExpected.class.getSimpleName() );
		}


		final Statement originalMethodBlock = super.methodBlock( method );
		final ExtendedFrameworkMethod extendedFrameworkMethod = (ExtendedFrameworkMethod) method;
		return new FailureExpectedHandler( this, originalMethodBlock, testClassMetadata, extendedFrameworkMethod, testInstance );
	}

	private Object testInstance;

	protected Object getTestInstance() throws Exception {
		if ( testInstance == null ) {
			testInstance = super.createTest();
		}
		return testInstance;
	}

	@Override
	protected Object createTest() throws Exception {
		return getTestInstance();
	}

	private List<FrameworkMethod> computedTestMethods;

	@Override
	protected List<FrameworkMethod> computeTestMethods() {
		if ( computedTestMethods == null ) {
			computedTestMethods = doComputation();
			sortMethods(computedTestMethods);
		}
		return computedTestMethods;
	}

	protected void sortMethods(List<FrameworkMethod> computedTestMethods) {
		if ( CollectionHelper.isEmpty( computedTestMethods ) ) {
			return;
		}
		Collections.sort( computedTestMethods, new Comparator<FrameworkMethod>() {
			@Override
			public int compare(FrameworkMethod o1, FrameworkMethod o2) {
				return o1.getName().compareTo( o2.getName() );
			}
		} );
	}

	protected List<FrameworkMethod> doComputation() {
        // Next, get all the test methods as understood by JUnit
        final List<FrameworkMethod> methods = super.computeTestMethods();

        // Now process that full list of test methods and build our custom result
        final List<FrameworkMethod> result = new ArrayList<FrameworkMethod>();
		final boolean doValidation = Boolean.getBoolean( Helper.VALIDATE_FAILURE_EXPECTED );
		int testCount = 0;

		Ignore virtualIgnore;

		for ( FrameworkMethod frameworkMethod : methods ) {
			FailureExpected failureExpected = Helper.locateAnnotation( FailureExpected.class, frameworkMethod, getTestClass() );
			// TODO: Re-think what's given to the ExtendedFrameworkMethod, rather than mocking the FailureExpected?
			if ( failureExpected == null ) {
				final FailureExpectedWithNewMetamodel failureExpectedWithNewMetamodel
						= Helper.locateAnnotation( FailureExpectedWithNewMetamodel.class, frameworkMethod, getTestClass() );
				if ( failureExpectedWithNewMetamodel != null ) {
					failureExpected = new FailureExpectedAdapter( failureExpectedWithNewMetamodel.message(),
							failureExpectedWithNewMetamodel.jiraKey() );
				}
			}
			if ( failureExpected == null ) {
				final FailureExpectedWithNewUnifiedXsd failureExpectedWithNewUnifiedXsd
						= Helper.locateAnnotation( FailureExpectedWithNewUnifiedXsd.class, frameworkMethod, getTestClass() );
				if ( failureExpectedWithNewUnifiedXsd != null ) {
					failureExpected = new FailureExpectedAdapter( failureExpectedWithNewUnifiedXsd.message(),
							failureExpectedWithNewUnifiedXsd.jiraKey() );
				}
			}
			// potentially ignore based on expected failure
			if ( failureExpected != null && !doValidation ) {
				virtualIgnore = new IgnoreImpl( Helper.extractIgnoreMessage( failureExpected, frameworkMethod ) );
			}
			else {
				virtualIgnore = convertSkipToIgnore( frameworkMethod );
			}

			testCount++;
			log.trace( "adding test " + Helper.extractTestName( frameworkMethod ) + " [#" + testCount + "]" );
			result.add( new ExtendedFrameworkMethod( frameworkMethod, virtualIgnore, failureExpected ) );
		}
		return result;
	}

	private static Dialect dialect = determineDialect();

	private static Dialect determineDialect() {
		try {
			return Dialect.getDialect();
		}
		catch( Exception e ) {
			return new Dialect() {
			};
		}
	}

	protected Ignore convertSkipToIgnore(FrameworkMethod frameworkMethod) {
		// @Skip
		Skip skip = Helper.locateAnnotation( Skip.class, frameworkMethod, getTestClass() );
		if ( skip != null ) {
			if ( isMatch( skip.condition() ) ) {
				return buildIgnore( skip );
			}
		}

		// @SkipForDialects & @SkipForDialect
		for ( SkipForDialect skipForDialectAnn : Helper.collectAnnotations(
				SkipForDialect.class, SkipForDialects.class, frameworkMethod, getTestClass()
		) ) {
			for ( Class<? extends Dialect> dialectClass : skipForDialectAnn.value() ) {
				if ( skipForDialectAnn.strictMatching() ) {
					if ( dialectClass.equals( dialect.getClass() ) ) {
						return buildIgnore( skipForDialectAnn );
					}
				}
				else {
					if ( dialectClass.isInstance( dialect ) ) {
						return buildIgnore( skipForDialectAnn );
					}
				}
			}
		}

		// @RequiresDialects & @RequiresDialect
		for ( RequiresDialect requiresDialectAnn : Helper.collectAnnotations(
				RequiresDialect.class, RequiresDialects.class, frameworkMethod, getTestClass()
		) ) {
			boolean foundMatch = false;
			for ( Class<? extends Dialect> dialectClass : requiresDialectAnn.value() ) {
				foundMatch = requiresDialectAnn.strictMatching()
						? dialectClass.equals( dialect.getClass() )
						: dialectClass.isInstance( dialect );
				if ( foundMatch ) {
					break;
				}
			}
			if ( !foundMatch ) {
				return buildIgnore( requiresDialectAnn );
			}
		}

		// @RequiresDialectFeature
		RequiresDialectFeature requiresDialectFeatureAnn = Helper.locateAnnotation( RequiresDialectFeature.class, frameworkMethod, getTestClass() );
		if ( requiresDialectFeatureAnn != null ) {
			try {
				boolean foundMatch = false;
				for ( Class<? extends DialectCheck> checkClass : requiresDialectFeatureAnn.value() ) {
					foundMatch = checkClass.newInstance().isMatch( dialect );
					if ( !foundMatch ) {
						return buildIgnore( requiresDialectFeatureAnn );
					}
				}
			}
			catch (RuntimeException e) {
				throw e;
			}
			catch (Exception e) {
				throw new RuntimeException( "Unable to instantiate DialectCheck", e );
			}
		}

		return null;
	}

	private Ignore buildIgnore(Skip skip) {
		return new IgnoreImpl( "@Skip : " + skip.message() );
	}

	private Ignore buildIgnore(SkipForDialect skip) {
		return buildIgnore( "@SkipForDialect match", skip.comment(), skip.jiraKey() );
	}

	private Ignore buildIgnore(String reason, String comment, String jiraKey) {
		StringBuilder buffer = new StringBuilder( reason );
		if ( StringHelper.isNotEmpty( comment ) ) {
			buffer.append( "; " ).append( comment );
		}

		if ( StringHelper.isNotEmpty( jiraKey ) ) {
			buffer.append( " (" ).append( jiraKey ).append( ')' );
		}

		return new IgnoreImpl( buffer.toString() );
	}

	private Ignore buildIgnore(RequiresDialect requiresDialect) {
		return buildIgnore( "@RequiresDialect non-match", requiresDialect.comment(), requiresDialect.jiraKey() );
	}

	private Ignore buildIgnore(RequiresDialectFeature requiresDialectFeature) {
		return buildIgnore( "@RequiresDialectFeature non-match", requiresDialectFeature.comment(), requiresDialectFeature.jiraKey() );
	}

	private boolean isMatch(Class<? extends Skip.Matcher> condition) {
		try {
			Skip.Matcher matcher = condition.newInstance();
			return matcher.isMatch();
		}
		catch (Exception e) {
			throw new MatcherInstantiationException( condition, e );
		}
	}

	private static class MatcherInstantiationException extends RuntimeException {
		private MatcherInstantiationException(Class<? extends Skip.Matcher> matcherClass, Throwable cause) {
			super( "Unable to instantiate specified Matcher [" + matcherClass.getName(), cause );
		}
	}

	@SuppressWarnings( {"ClassExplicitlyAnnotation"})
	public static class IgnoreImpl implements Ignore {
		private final String value;

		public IgnoreImpl(String value) {
			this.value = value;
		}

		@Override
		public String value() {
			return value;
		}

		@Override
		public Class<? extends Annotation> annotationType() {
			return Ignore.class;
		}
	}

	@SuppressWarnings("ClassExplicitlyAnnotation")
	private class FailureExpectedAdapter implements FailureExpected {
		private final String message;
		
		private final String jiraKey;

		public FailureExpectedAdapter(String message, String jiraKey) {
			this.message = message;
			this.jiraKey = jiraKey;
		}
		@Override
		public Class< ? extends Annotation > annotationType() {
			return FailureExpected.class;
		}

		@Override
		public String message() {
			return message;
		}

		@Override
		public String jiraKey() {
			return jiraKey;
		}
	}
}
