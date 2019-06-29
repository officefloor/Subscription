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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

import java.sql.Date;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Ref;

import net.officefloor.app.subscription.SubscriptionCalculator.Subscription;
import net.officefloor.app.subscription.store.Domain;
import net.officefloor.app.subscription.store.Invoice;
import net.officefloor.app.subscription.store.Payment;
import net.officefloor.app.subscription.store.Refund;
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

	private final TestHelper helper = new TestHelper(this.objectify);

	private User user;

	private Ref<User> userRef;

	private Invoice invoice;

	private Ref<Invoice> invoiceRef;

	private ZonedDateTime now = TestHelper.now();

	private boolean isReversePayments = false;

	@Test
	public void noPayments() {
		this.verifySubscriptionCalculation();
	}

	@Test
	public void singlePayment() {
		this.verifySubscriptionCalculation(payment(now, now.plus(1, ChronoUnit.YEARS)));
	}

	@Test
	public void consecutivePayments() {
		this.verifySubscriptionCalculation(payment(now.plus(2, ChronoUnit.MONTHS), now.plus(3, ChronoUnit.YEARS)),
				payment(now.plus(1, ChronoUnit.MONTHS), now.plus(2, ChronoUnit.YEARS)),
				payment(now, now.plus(1, ChronoUnit.YEARS)));
	}

	@Test
	public void consecutivePaymentsIgnoreRestart() {
		this.verifySubscriptionCalculation(payment(now.plus(2, ChronoUnit.MONTHS), true, now.plus(3, ChronoUnit.YEARS)),
				payment(now.plus(1, ChronoUnit.MONTHS), true, now.plus(2, ChronoUnit.YEARS)),
				payment(now, now.plus(1, ChronoUnit.YEARS)));
	}

	@Test
	public void irregularPaymentsWithoutRestart() {
		this.verifySubscriptionCalculation(payment(now.plus(10, ChronoUnit.YEARS), now.plus(3, ChronoUnit.YEARS)),
				payment(now.plus(5, ChronoUnit.YEARS), now.plus(2, ChronoUnit.YEARS)),
				payment(now, now.plus(1, ChronoUnit.YEARS)));
	}

	@Test
	public void irregularPaymentsWithRestart() {
		this.verifySubscriptionCalculation(
				payment(now.plus(10, ChronoUnit.YEARS), true, now.plus(11, ChronoUnit.YEARS)),
				payment(now.plus(5, ChronoUnit.YEARS), true, now.plus(6, ChronoUnit.YEARS)),
				payment(now, now.plus(1, ChronoUnit.YEARS)));
	}

	@Test
	public void outOfOrderPayments() {
		this.isReversePayments = true;
		this.consecutivePayments();
		this.irregularPaymentsWithoutRestart();
		this.irregularPaymentsWithRestart();
	}

	@Test
	public void ignoreRefundedPayments() {
		VerifiablePayment refunded = payment(now.plus(1, ChronoUnit.MONTHS), null);
		Refund refund = new Refund("TEST");
		this.objectify.store(refund);
		refunded.setRefund(Ref.create(refund));
		this.verifySubscriptionCalculation(payment(now.plus(2, ChronoUnit.MONTHS), now.plus(2, ChronoUnit.YEARS)),
				refunded, payment(now, now.plus(1, ChronoUnit.YEARS)));
	}

	@Test
	public void accessOwnPayments() {

		// Create payments as user
		Subscription[] subscriptions = this.verifySubscriptionCalculation(
				payment(now.plus(2, ChronoUnit.MONTHS), now.plus(3, ChronoUnit.YEARS)),
				payment(now.plus(1, ChronoUnit.MONTHS), now.plus(2, ChronoUnit.YEARS)),
				payment(now, now.plus(1, ChronoUnit.YEARS)));

		// Ensure access to payments
		assertEquals("Incorrect number of subscriptions", 3, subscriptions.length);
		for (Subscription subscription : subscriptions) {
			assertNotNull("Should have payer", subscription.getPaidBy());
			assertEquals("Incorrect payer", this.user.getId(), subscription.getPaidBy().getId());
			assertEquals("Incorrect payment order id", "MOCK_PAYMENT_ORDER_ID", subscription.getPaymentOrderId());
			assertEquals("Incorrect payment receipt", "MOCK_RECEIPT", subscription.getPaymentReceipt());
			assertEquals("Incorrect payment amount", Integer.valueOf(500), subscription.getPaymentAmount());
		}
	}

	@Test
	public void notAccessOtherUserPayments() {

		// Create payments on another user
		Ref<User> anotherRef = Ref.create(this.helper.setupUser("Another"));
		Subscription[] subscriptions = this.verifySubscriptionCalculation(
				payment(anotherRef, now.plus(2, ChronoUnit.MONTHS), false, now.plus(3, ChronoUnit.YEARS)),
				payment(anotherRef, now.plus(1, ChronoUnit.MONTHS), false, now.plus(2, ChronoUnit.YEARS)),
				payment(anotherRef, now, false, now.plus(1, ChronoUnit.YEARS)));

		// Ensure no access to payment details
		assertEquals("Incorrect number of subscriptions", 3, subscriptions.length);
		for (Subscription subscription : subscriptions) {
			assertNull("Should not have payer", subscription.getPaidBy());
			assertNull("Should not have payment order id", subscription.getPaymentOrderId());
		}
	}

	@Test
	public void adminAccessAllPayments() {

		// Setup user as admin
		this.user = this.helper.setupUser("Daniel", User.ROLE_ADMIN);
		this.userRef = Ref.create(this.user);

		// Create payments on another user
		Ref<User> anotherRef = Ref.create(this.helper.setupUser("Another"));
		Subscription[] subscriptions = this.verifySubscriptionCalculation(
				payment(anotherRef, now.plus(2, ChronoUnit.MONTHS), false, now.plus(3, ChronoUnit.YEARS)),
				payment(anotherRef, now.plus(1, ChronoUnit.MONTHS), false, now.plus(2, ChronoUnit.YEARS)),
				payment(anotherRef, now, false, now.plus(1, ChronoUnit.YEARS)));

		// Verify admin has access to payment details
		assertEquals("Incorrect number of subscriptions", 3, subscriptions.length);
		for (Subscription subscription : subscriptions) {
			assertNotNull("Should have payer", subscription.getPaidBy());
			assertEquals("Incorrect payer", anotherRef.get().getId(), subscription.getPaidBy().getId());
			assertEquals("Incorrect payment order id", "MOCK_PAYMENT_ORDER_ID", subscription.getPaymentOrderId());
			assertEquals("Incorrect payment receipt", "MOCK_RECEIPT", subscription.getPaymentReceipt());
			assertEquals("Incorrect payment amount", Integer.valueOf(500), subscription.getPaymentAmount());
		}
	}

	@Before
	public void setup() {
		ObjectifyService.register(User.class);
		ObjectifyService.register(Invoice.class);
		ObjectifyService.register(Refund.class);
		this.user = this.helper.setupUser("Daniel");
		this.userRef = Ref.create(this.user);
		this.invoice = new Invoice(this.userRef, Domain.PRODUCT_TYPE, "officefloor.org", false);
		this.invoice.setPaymentOrderId("MOCK_PAYMENT_ORDER_ID");
		this.objectify.store(this.invoice);
		this.invoiceRef = Ref.create(this.invoice);
	}

	private Subscription[] verifySubscriptionCalculation(VerifiablePayment... payments) {

		// Determine if reverse payments
		if (this.isReversePayments) {
			VerifiablePayment first = payments[0];
			Collections.reverse(Arrays.asList(payments));
			assertNotSame("INVALID TEST: should be reversed", first, payments[0]);
		}

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
			assertEquals("Incorrect payment date", TestHelper.toZonedDateTime(e.getTimestamp()), a.getPaymentDate());
			assertEquals("Incorrect extends to date", e.extendsToDate, a.getExtendsToDate());
			assertEquals("Incorrect restart flag", e.getIsRestartSubscription(), a.isRestartSubscription());
		}

		// Return the subscriptions
		return subscriptions;
	}

	private VerifiablePayment payment(ZonedDateTime timestamp, ZonedDateTime extendsTo) {
		return payment(timestamp, false, extendsTo);
	}

	private VerifiablePayment payment(ZonedDateTime timestamp, boolean isRestartSubscription, ZonedDateTime extendsTo) {
		return payment(this.userRef, timestamp, isRestartSubscription, extendsTo);
	}

	private VerifiablePayment payment(Ref<User> userRef, ZonedDateTime timestamp, boolean isRestartSubscription,
			ZonedDateTime extendsTo) {
		return new VerifiablePayment(userRef, timestamp, isRestartSubscription, extendsTo);
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