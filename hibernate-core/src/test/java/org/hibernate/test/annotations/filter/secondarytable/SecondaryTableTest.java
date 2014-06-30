package org.hibernate.test.annotations.filter.secondarytable;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

public class SecondaryTableTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {User.class};
	}

	@Override
	protected void prepareTest() throws Exception {
		openSession();
		insertUser("q@s.com", 21, false, "a1", "b");
		insertUser("r@s.com", 22, false, "a2", "b");
		insertUser("s@s.com", 23, true, "a3", "b");
		insertUser("t@s.com", 24, false, "a4", "b");
		session.flush();
	}
	
	@Test
	public void testFilter(){
		Assert.assertEquals(Long.valueOf(4), session.createQuery("select count(u) from User u").uniqueResult());
		session.enableFilter("ageFilter").setParameter("age", 24);
		Assert.assertEquals(Long.valueOf(2), session.createQuery("select count(u) from User u").uniqueResult());
	}
	
	private void insertUser(String emailAddress, int age, boolean lockedOut, String username, String password){
		User user = new User();
		user.setEmailAddress(emailAddress);
		user.setAge(age);
		user.setLockedOut(lockedOut);
		user.setUsername(username);
		user.setPassword(password);
		session.persist(user);
	}

}
