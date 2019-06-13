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
import static org.junit.Assert.assertNotSame;

import java.util.Date;
import java.util.function.Function;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.googlecode.objectify.Ref;

import net.officefloor.app.subscription.store.Administration;
import net.officefloor.app.subscription.store.Administration.Administrator;
import net.officefloor.app.subscription.store.Domain;
import net.officefloor.app.subscription.store.GoogleSignin;
import net.officefloor.app.subscription.store.Invoice;
import net.officefloor.app.subscription.store.Payment;
import net.officefloor.app.subscription.store.Refund;
import net.officefloor.app.subscription.store.User;
import net.officefloor.nosql.objectify.mock.ObjectifyRule;
import net.officefloor.woof.mock.MockWoofServerRule;

/**
 * Ensure can write/read values from store.
 * 
 * @author Daniel Sagenschneider
 */
public class StoreTest {

	private ObjectifyRule objectify = new ObjectifyRule();

	private MockWoofServerRule server = new MockWoofServerRule();

	@Rule
	public RuleChain order = RuleChain.outerRule(this.objectify).around(this.server);

	@Test
	public void writeReadEntities() {

		this.entity(
				new Administration("GOOGLE_CLIENT_ID", new Administrator[] { new Administrator("GOOGLE_ID", "NOTES") },
						"sandbox", "PAYPAL_CLIENT", "PAYPAL_SECRET", "AUD"),
				e -> e.getGoogleClientId(), e -> e.getAdministrators()[0].getGoogleId(),
				e -> e.getAdministrators()[0].getNotes(), e -> e.getPaypalEnvironment(), e -> e.getPaypalClientId(),
				e -> e.getPaypalClientSecret(), e -> e.getPaypalCurrency());

		User user = this.entity(TestHelper.newUser("Daniel", "admin"), e -> e.getId(), e -> e.getEmail(),
				e -> e.getName(), e -> e.getPhotoUrl(), e -> e.getRoles()[0], e -> e.getTimestamp());
		Ref<User> userRef = Ref.create(user);

		GoogleSignin signin = new GoogleSignin("1234", "daniel@officefloor.org");
		signin.setName("Daniel");
		signin.setPhotoUrl("photo.png");
		signin.setUser(userRef);
		signin = this.entity(signin, e -> e.getId(), e -> e.getGoogleId(), e -> e.getEmail(), e -> e.getName(),
				e -> e.getPhotoUrl(), e -> e.getTimestamp(), e -> e.getUser());

		Invoice invoice = new Invoice(userRef, Domain.PRODUCT_TYPE, "officefloor.org");
		invoice.setPaymentOrderId("ORDER_ID");
		invoice = this.entity(invoice, e -> e.getId(), e -> e.getUser(), e -> e.getProductType(),
				e -> e.getProductReference(), e -> e.getPaymentOrderId(), e -> e.getTimestamp());
		Ref<Invoice> invoiceRef = Ref.create(invoice);

		Payment payment = this.entity(
				new Payment(userRef, invoiceRef, Domain.PRODUCT_TYPE, "officefloor.org", true, 500, "RECEIPT_ID"),
				e -> e.getId(), e -> e.getUser(), e -> e.getInvoice(), e -> e.getProductType(),
				e -> e.getProductReference(), e -> e.getIsRestartSubscription(), e -> e.getAmount(),
				e -> e.getReceipt(), e -> e.getTimestamp());

		this.entity(new Domain("officefloor.org", new Date()), e -> e.getId(), e -> e.getDomain(), e -> e.getExpires(),
				e -> e.getTimestamp());

		Refund refund = this.entity(new Refund("Some reason"), e -> e.getId(), e -> e.getTimestamp());
		payment.setRefund(Ref.create(refund));
		this.objectify.store(payment);
		this.objectify.consistent(() -> this.objectify.get(Payment.class), (check) -> check.getRefund() != null);
		payment = this.entity(payment, e -> e.getRefund(), e -> e.getTimestamp());
	}

	@SafeVarargs
	@SuppressWarnings("unchecked")
	private final <T> T entity(T expected, Function<T, Object>... valueExtractors) {

		// Store the expected
		this.objectify.store(expected);

		// Obtain the entity
		this.objectify.ofy().clear();
		T actual = (T) this.objectify.get(expected.getClass());
		assertNotSame(expected, actual);

		// Validate the loaded entity
		for (Function<T, Object> valueExtractor : valueExtractors) {
			assertEquals(valueExtractor.apply(expected), valueExtractor.apply(actual));
		}

		// Return the retrieved
		return actual;
	}

}