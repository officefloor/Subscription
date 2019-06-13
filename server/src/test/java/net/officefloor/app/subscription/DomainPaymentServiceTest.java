/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2019 Daniel Sagenschneider
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.officefloor.app.subscription;

import static net.officefloor.app.subscription.TestHelper.toText;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.googlecode.objectify.Ref;

import net.officefloor.app.subscription.DomainPaymentService.DomainPayment;
import net.officefloor.app.subscription.DomainPaymentService.DomainPayments;
import net.officefloor.app.subscription.store.Domain;
import net.officefloor.app.subscription.store.ObjectifyEntities;
import net.officefloor.app.subscription.store.User;
import net.officefloor.nosql.objectify.mock.ObjectifyRule;
import net.officefloor.server.http.HttpException;
import net.officefloor.web.jwt.mock.MockJwtAccessTokenRule;
import net.officefloor.woof.mock.MockWoofResponse;
import net.officefloor.woof.mock.MockWoofServer;
import net.officefloor.woof.mock.MockWoofServerRule;

/**
 * @author Daniel Sagenschneider
 */
public class DomainPaymentServiceTest {

	private final MockJwtAccessTokenRule jwt = new MockJwtAccessTokenRule();

	private final ObjectifyRule objectify = new ObjectifyRule();

	private final MockWoofServerRule server = new MockWoofServerRule();

	@Rule
	public final RuleChain chain = RuleChain.outerRule(this.jwt).around(this.objectify).around(this.server);

	private final TestHelper helper = new TestHelper(this.objectify);

	@Test
	public void noAccessToUnpaidDomain() throws Exception {

		// Setup user for payments
		User user = this.helper.setupUser("Daniel");

		// Setup another user
		User anotherUser = this.helper.setupUser("Another");

		// Load the domain and payments for another user
		this.helper.setupPayment(Ref.create(anotherUser), "officefloor.org", false,
				ZonedDateTime.now(ObjectifyEntities.ZONE));
		this.objectify
				.store(new Domain("officefloor.org", Date.from(ZonedDateTime.now(ObjectifyEntities.ZONE).toInstant())));

		// Obtain the payments
		MockWoofResponse response = this.server
				.send(this.jwt.authorize(user, MockWoofServer.mockRequest("/payments/domain/officefloor.org")));
		response.assertJsonError(new HttpException(403, "No payment to access domain officefloor.org"));
	}

	@Test
	public void getDomainPayments() throws Exception {

		// Setup user for payments
		User user = this.helper.setupUser("Daniel");
		Ref<User> userRef = Ref.create(user);

		// Load the payments
		ZonedDateTime now = ZonedDateTime.now(ObjectifyEntities.ZONE);
		ZonedDateTime firstPaymentDate = now.minus(4, ChronoUnit.YEARS);
		ZonedDateTime secondPaymentDate = now.minus(2, ChronoUnit.YEARS);
		ZonedDateTime thirdPaymentDate = now.minus(1, ChronoUnit.YEARS);
		this.helper.setupPayment(userRef, "officefloor.org", false, firstPaymentDate);
		this.helper.setupPayment(userRef, "officefloor.org", true, secondPaymentDate);
		this.helper.setupPayment(userRef, "officefloor.org", false, thirdPaymentDate);

		// Obtain the payments
		MockWoofResponse response = this.server
				.send(this.jwt.authorize(user, MockWoofServer.mockRequest("/payments/domain/officefloor.org")));
		response.assertJson(200,
				new DomainPayments("officefloor.org", ResponseUtil.toText(now), new DomainPayment[] {
						new DomainPayment(toText(thirdPaymentDate), toText(now), false, "Daniel",
								"daniel@officefloor.org", null),
						new DomainPayment(toText(secondPaymentDate), toText(thirdPaymentDate), true, "Daniel",
								"daniel@officefloor.org", null),
						new DomainPayment(toText(firstPaymentDate), toText(firstPaymentDate.plus(1, ChronoUnit.YEARS)),
								false, "Daniel", "daniel@officefloor.org", null) }));
	}

	@Test
	public void getOverlappingDomainPayments() throws Exception {

		// Setup user for payments
		User user = this.helper.setupUser("Daniel");
		Ref<User> userRef = Ref.create(user);

		// Load the payments
		ZonedDateTime now = ZonedDateTime.now(ObjectifyEntities.ZONE);
		final int NUMBER_OF_PAYMENTS = 10;
		for (int i = 0; i < NUMBER_OF_PAYMENTS; i++) {
			this.helper.setupPayment(userRef, "officefloor.org", false, now.plus(i, ChronoUnit.SECONDS));
		}

		// Load the expected payment responses
		DomainPayment[] payments = new DomainPayment[NUMBER_OF_PAYMENTS];
		for (int i = 0; i < NUMBER_OF_PAYMENTS; i++) {
			payments[NUMBER_OF_PAYMENTS - 1 - i] = new DomainPayment(toText(now.plus(i, ChronoUnit.SECONDS)),
					toText(now.plus(i + 1, ChronoUnit.YEARS)), false, "Daniel", "daniel@officefloor.org", null);
		}

		// Obtain the payments
		MockWoofResponse response = this.server
				.send(this.jwt.authorize(user, MockWoofServer.mockRequest("/payments/domain/officefloor.org")));
		response.assertJson(200, new DomainPayments("officefloor.org", payments[0].getExtendsToDate(), payments));
	}

}