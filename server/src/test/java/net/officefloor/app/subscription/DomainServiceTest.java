package net.officefloor.app.subscription;

import static net.officefloor.app.subscription.TestHelper.toText;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.googlecode.objectify.Ref;

import net.officefloor.app.subscription.DomainService.PaidDomain;
import net.officefloor.app.subscription.store.Domain;
import net.officefloor.app.subscription.store.User;
import net.officefloor.nosql.objectify.mock.ObjectifyRule;
import net.officefloor.web.jwt.mock.MockJwtAccessTokenRule;
import net.officefloor.woof.mock.MockWoofResponse;
import net.officefloor.woof.mock.MockWoofServer;
import net.officefloor.woof.mock.MockWoofServerRule;

/**
 * Tests obtaining domains.
 * 
 * @author Daniel Sagenschneider
 */
public class DomainServiceTest {

	private final MockJwtAccessTokenRule jwt = new MockJwtAccessTokenRule();

	private final ObjectifyRule objectify = new ObjectifyRule();

	private final MockWoofServerRule server = new MockWoofServerRule();

	@Rule
	public final RuleChain chain = RuleChain.outerRule(this.jwt).around(this.objectify).around(this.server);

	private final TestHelper helper = new TestHelper(this.objectify);

	@Test
	public void noDomainsPaid() throws Exception {

		// Setup user for payments
		User user = this.helper.setupUser("Daniel");

		// Obtain the domains
		MockWoofResponse response = this.server.send(this.jwt.authorize(user, MockWoofServer.mockRequest("/domains")));
		response.assertJson(200, new PaidDomain[0]);
	}

	@Test
	public void getDomains() throws Exception {

		// Setup user for payments
		User user = this.helper.setupUser("Daniel");
		Ref<User> userRef = Ref.create(user);

		// Setup other user
		User anotherUser = this.helper.setupUser("Another");
		Ref<User> anotherUserRef = Ref.create(anotherUser);

		// Load the payments
		ZonedDateTime now = TestHelper.now();
		this.helper.setupPayment(userRef, "officefloor.org", false, now.minus(2, ChronoUnit.YEARS));
		this.helper.setupPayment(userRef, "officefloor.org", false, now.minus(1, ChronoUnit.YEARS));
		this.helper.setupPayment(userRef, "officefloor.org", false, now);
		this.helper.setupPayment(userRef, "activicy.com", false, now);
		this.helper.setupPayment(anotherUserRef, "not.returned", false, now);
		this.helper.setupPayment(userRef, "missing.domain", false, now);

		// Create the expire dates
		ZonedDateTime expireOfficeFloor = TestHelper.now().plus(1, ChronoUnit.YEARS);
		ZonedDateTime expireActivicy = expireOfficeFloor.plus(2, ChronoUnit.WEEKS);
		ZonedDateTime expireNotReturned = expireOfficeFloor.plus(3, ChronoUnit.MONTHS);

		// Load the domains
		Domain domainOfficeFloor = new Domain("officefloor.org", Date.from(expireOfficeFloor.toInstant()));
		Domain domainActivicy = new Domain("activicy.com", Date.from(expireActivicy.toInstant()));
		Domain domainNotReturned = new Domain("not.returned", Date.from(expireNotReturned.toInstant()));
		this.objectify.store(domainOfficeFloor, domainActivicy, domainNotReturned);

		// Obtain the domains
		MockWoofResponse response = this.server.send(this.jwt.authorize(user, MockWoofServer.mockRequest("/domains")));
		response.assertJson(200, new PaidDomain[] { new PaidDomain("officefloor.org", toText(expireOfficeFloor)),
				new PaidDomain("activicy.com", toText(expireActivicy)) });
	}

}