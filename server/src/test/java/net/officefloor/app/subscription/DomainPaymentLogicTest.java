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

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.junit.Test;

import com.googlecode.objectify.Ref;

import net.officefloor.app.subscription.DomainPaymentLogic.DomainPayments;
import net.officefloor.app.subscription.SubscriptionCalculator.Subscription;
import net.officefloor.app.subscription.store.Domain;
import net.officefloor.app.subscription.store.ObjectifyEntities;
import net.officefloor.app.subscription.store.User;
import net.officefloor.server.http.HttpException;
import net.officefloor.woof.mock.MockWoofResponse;
import net.officefloor.woof.mock.MockWoofServer;

/**
 * @author Daniel Sagenschneider
 */
public class DomainPaymentLogicTest extends AbstractDomainTestCase {

	@Test
	public void noAccessToUnpaidDomain() throws Exception {

		// Setup user for payments
		User user = AuthenticateLogicTest.setupUser(this.objectify, "Daniel");

		// Setup another user
		User anotherUser = AuthenticateLogicTest.setupUser(this.objectify, "Another");

		// Load the domain and payments for another user
		this.setupPayment(Ref.create(anotherUser), "officefloor.org", false, ZonedDateTime.now(ObjectifyEntities.ZONE));
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
		User user = AuthenticateLogicTest.setupUser(this.objectify, "Daniel");
		Ref<User> userRef = Ref.create(user);

		// Load the payments
		ZonedDateTime now = ZonedDateTime.now(ObjectifyEntities.ZONE);
		ZonedDateTime firstPaymentDate = now.minus(4, ChronoUnit.YEARS);
		ZonedDateTime secondPaymentDate = now.minus(2, ChronoUnit.YEARS);
		ZonedDateTime thirdPaymentDate = now.minus(1, ChronoUnit.YEARS);
		this.setupPayment(userRef, "officefloor.org", false, firstPaymentDate);
		this.setupPayment(userRef, "officefloor.org", true, secondPaymentDate);
		this.setupPayment(userRef, "officefloor.org", false, thirdPaymentDate);

		// Obtain the payments
		MockWoofResponse response = this.server
				.send(this.jwt.authorize(user, MockWoofServer.mockRequest("/payments/domain/officefloor.org")));
		response.assertJson(200,
				new DomainPayments("officefloor.org", ResponseUtil.toText(now), new Subscription[] {
						new Subscription(toText(thirdPaymentDate), toText(now), false, "Daniel",
								"daniel@officefloor.org", null),
						new Subscription(toText(secondPaymentDate), toText(thirdPaymentDate), true, "Daniel",
								"daniel@officefloor.org", null),
						new Subscription(toText(firstPaymentDate), toText(firstPaymentDate.plus(1, ChronoUnit.YEARS)),
								false, "Daniel", "daniel@officefloor.org", null) }));
	}

	@Test
	public void getOverlappingDomainPayments() throws Exception {

		// Setup user for payments
		User user = AuthenticateLogicTest.setupUser(this.objectify, "Daniel");
		Ref<User> userRef = Ref.create(user);

		// Load the payments
		ZonedDateTime now = ZonedDateTime.now(ObjectifyEntities.ZONE);
		final int NUMBER_OF_PAYMENTS = 10;
		for (int i = 0; i < NUMBER_OF_PAYMENTS; i++) {
			this.setupPayment(userRef, "officefloor.org", false, now.plus(i, ChronoUnit.SECONDS));
		}

		// Load the expected payment responses
		Subscription[] payments = new Subscription[NUMBER_OF_PAYMENTS];
		for (int i = 0; i < NUMBER_OF_PAYMENTS; i++) {
			payments[NUMBER_OF_PAYMENTS - 1 - i] = new Subscription(toText(now.plus(i, ChronoUnit.SECONDS)),
					toText(now.plus(i + 1, ChronoUnit.YEARS)), false, "Daniel", "daniel@officefloor.org", null);
		}

		// Obtain the payments
		MockWoofResponse response = this.server
				.send(this.jwt.authorize(user, MockWoofServer.mockRequest("/payments/domain/officefloor.org")));
		response.assertJson(200, new DomainPayments("officefloor.org", payments[0].getExtendsToDate(), payments));
	}

}