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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.sql.Date;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Ref;

import net.officefloor.app.subscription.SubscriptionCalculator.Subscription;
import net.officefloor.app.subscription.store.Domain;
import net.officefloor.app.subscription.store.Invoice;
import net.officefloor.app.subscription.store.ObjectifyEntities;
import net.officefloor.app.subscription.store.Payment;
import net.officefloor.app.subscription.store.User;
import net.officefloor.nosql.objectify.mock.ObjectifyRule;

/**
 * Tests the {@link SubscriptionCalculator}.
 * 
 * @author Daniel Sagenschneider
 */
public class SubscriptionCalculatorTest {

	@Rule
	public ObjectifyRule objectify = new ObjectifyRule();

	private User user;

	private Ref<User> userRef;

	private Invoice invoice;

	private Ref<Invoice> invoiceRef;

	private ZonedDateTime now = ZonedDateTime.now(ObjectifyEntities.ZONE);

	@Before
	public void setup() {
		ObjectifyService.register(User.class);
		ObjectifyService.register(Invoice.class);
		this.user = AuthenticateServiceTest.setupUser(this.objectify, "Daniel");
		this.userRef = Ref.create(this.user);
		this.invoice = new Invoice(this.userRef, Domain.PRODUCT_TYPE, "officefloor.org");
		this.invoice.setPaymentOrderId("MOCK_PAYMENT_ORDER_ID");
		this.objectify.store(this.invoice);
		this.invoiceRef = Ref.create(this.invoice);
	}

	@Test
	public void noPayments() {
		this.doPaymentTest();
	}

	@Test
	public void singlePayment() throws Exception {
		this.doPaymentTest(payment(now, now.plus(1, ChronoUnit.YEARS)));
	}

	private void doPaymentTest(VerifiablePayment... payments) {

		// Calculate subscriptions
		Subscription[] subscriptions = SubscriptionCalculator.calculateSubscriptions(this.user, payments);

		// Obtain the payments involved in subscriptions in subscription order
		VerifiablePayment[] expected = Stream.of(payments).filter((payment) -> payment.getRefund() == null)
				.sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp())).toArray(VerifiablePayment[]::new);

		// Ensure appropriate number of subscriptions
		assertEquals("Incorrect number of subscriptions", expected.length, subscriptions.length);

		// Verify subscriptions are as expected
		for (int i = 0; i < expected.length; i++) {
			VerifiablePayment e = expected[i];
			Subscription a = subscriptions[i];
			assertEquals("Incorrect payment date", e.getTimestamp().toInstant().atZone(ObjectifyEntities.ZONE),
					a.getPaymentDate());
			assertEquals("Incorrect extends to date", e.extendsToDate, a.getExtendsToDate());
			assertEquals("Incorrect restart flag", e.getIsRestartSubscription(), a.isRestartSubscription());
			if (this.user.getId().equals(e.getUser().get().getId())) {
				// Same user (so has access to details)
				assertNotNull("Should have payer", a.getPaidBy());
				assertEquals("Incorrect payer", this.user.getId(), a.getPaidBy().getId());
				assertEquals("Incorrect payment order id", "MOCK_PAYMENT_ORDER_ID", a.getPaymentOrderId());
			} else {
				// No access
				assertNull("Should not provide payer", a.getPaidBy());
				assertNull("Should not provide payment order id", a.getPaymentOrderId());
			}
		}
	}

	private VerifiablePayment payment(ZonedDateTime timestamp, ZonedDateTime extendsTo) {
		return new VerifiablePayment(this.userRef, timestamp, false, extendsTo);
	}

	private class VerifiablePayment extends Payment {

		private final ZonedDateTime extendsToDate;

		private VerifiablePayment(Ref<User> userRef, ZonedDateTime timestamp, boolean isRestartSubscription,
				ZonedDateTime extendsToDate) {
			super(userRef, SubscriptionCalculatorTest.this.invoiceRef, Domain.PRODUCT_TYPE, "officefloor.org",
					isRestartSubscription, 500, "MOCK_RECEIPT");
			this.setTimestamp(Date.from(timestamp.toInstant()));
			this.extendsToDate = extendsToDate;
		}
	}

}