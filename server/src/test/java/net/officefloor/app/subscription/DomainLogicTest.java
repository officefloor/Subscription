package net.officefloor.app.subscription;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.junit.Test;

import com.googlecode.objectify.Ref;

import net.officefloor.app.subscription.DomainLogic.PaidDomain;
import net.officefloor.app.subscription.store.Domain;
import net.officefloor.app.subscription.store.ObjectifyEntities;
import net.officefloor.app.subscription.store.User;
import net.officefloor.woof.mock.MockWoofResponse;
import net.officefloor.woof.mock.MockWoofServer;

/**
 * Tests obtaining domains.
 * 
 * @author Daniel Sagenschneider
 */
public class DomainLogicTest extends AbstractDomainTestCase {

	@Test
	public void noDomainsPaid() throws Exception {

		// Setup user for payments
		User user = AuthenticateLogicTest.setupUser(this.objectify, "Daniel");

		// Obtain the domains
		MockWoofResponse response = this.server.send(this.jwt.authorize(user, MockWoofServer.mockRequest("/domains")));
		response.assertJson(200, new PaidDomain[0]);
	}

	@Test
	public void getDomains() throws Exception {

		// Setup user for payments
		User user = AuthenticateLogicTest.setupUser(this.objectify, "Daniel");
		Ref<User> userRef = Ref.create(user);

		// Setup other user
		User anotherUser = AuthenticateLogicTest.setupUser(this.objectify, "Another");
		Ref<User> anotherUserRef = Ref.create(anotherUser);

		// Load the payments
		ZonedDateTime now = ZonedDateTime.now(ObjectifyEntities.ZONE);
		this.setupPayment(userRef, "officefloor.org", false, now.minus(2, ChronoUnit.YEARS));
		this.setupPayment(userRef, "officefloor.org", false, now.minus(1, ChronoUnit.YEARS));
		this.setupPayment(userRef, "officefloor.org", false, now);
		this.setupPayment(userRef, "activicy.com", false, now);
		this.setupPayment(anotherUserRef, "not.returned", false, now);
		this.setupPayment(userRef, "missing.domain", false, now);

		// Create the expire dates
		ZonedDateTime expireOfficeFloor = Instant.now().atZone(ObjectifyEntities.ZONE).plus(1, ChronoUnit.YEARS);
		ZonedDateTime expireActivicy = expireOfficeFloor.plus(2, ChronoUnit.WEEKS);
		ZonedDateTime expireNotReturned = expireOfficeFloor.plus(3, ChronoUnit.MONTHS);

		// Load the domains
		Domain domainOfficeFloor = new Domain("officefloor.org", Date.from(expireOfficeFloor.toInstant()));
		Domain domainActivicy = new Domain("activicy.com", Date.from(expireActivicy.toInstant()));
		Domain domainNotReturned = new Domain("not.returned", Date.from(expireNotReturned.toInstant()));
		this.objectify.store(domainOfficeFloor, domainActivicy, domainNotReturned);

		// Obtain the domains
		MockWoofResponse response = this.server.send(this.jwt.authorize(user, MockWoofServer.mockRequest("/domains")));
		response.assertJson(200,
				new PaidDomain[] { new PaidDomain("officefloor.org", toText(expireOfficeFloor)),
						new PaidDomain("activicy.com", toText(expireActivicy)) });
	}

}