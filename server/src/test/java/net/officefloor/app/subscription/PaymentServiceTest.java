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
import static org.junit.Assert.fail;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.googlecode.objectify.Ref;
import com.paypal.orders.Order;

import net.officefloor.app.subscription.PaymentService.CapturedPayment;
import net.officefloor.app.subscription.store.Domain;
import net.officefloor.app.subscription.store.Invoice;
import net.officefloor.app.subscription.store.Payment;
import net.officefloor.app.subscription.store.User;
import net.officefloor.nosql.objectify.mock.ObjectifyRule;
import net.officefloor.pay.paypal.mock.PayPalRule;
import net.officefloor.server.http.HttpMethod;
import net.officefloor.web.jwt.mock.MockJwtAccessTokenRule;
import net.officefloor.woof.mock.MockWoofResponse;
import net.officefloor.woof.mock.MockWoofServer;
import net.officefloor.woof.mock.MockWoofServerRule;

/**
 * Test creating payments.
 * 
 * @author Daniel Sagenschneider
 */
public class PaymentServiceTest {

	private final MockJwtAccessTokenRule jwt = new MockJwtAccessTokenRule();

	private final PayPalRule payPal = new PayPalRule();

	private final ObjectifyRule objectify = new ObjectifyRule();

	private final MockWoofServerRule server = new MockWoofServerRule();

	@Rule
	public RuleChain chain = RuleChain.outerRule(this.jwt).around(this.payPal).around(this.objectify)
			.around(this.server);

	private final TestHelper helper = new TestHelper(this.objectify);

	@Test
	public void createFirstDomainPayment() throws Exception {
		fail("TODO test create first payment");
	}

	@Test
	public void extendSubscription() throws Exception {
		fail("TODO test extend payment");
	}

	@Test
	public void extendSubscriptionOnLatePayment() throws Exception {
		fail("TODO test extend payment on late payment");
	}

	@Test
	public void extendSubscriptionIgnoringRestart() throws Exception {
		fail("TODO test extend payment ignoring restart as not necessary");
	}

	@Test
	public void restartSubscription() throws Exception {
		fail("TODO test restart domain subscription");
	}

	@Test
	public void createPayment() throws Exception {

		// Record
		this.payPal.addOrdersCaptureResponse(new Order().id("MOCK_ORDER_ID").status("COMPLETED"))
				.validate((request) -> {
					assertEquals("MOCK_ORDER_ID", this.payPal.getOrderId(request));
				});

		// Setup the invoice
		User user = this.helper.setupUser("Daniel");
		Invoice invoice = new Invoice(Ref.create(user), Domain.PRODUCT_TYPE, "officefloor.org");
		invoice.setPaymentOrderId("MOCK_ORDER_ID");
		this.objectify.store(invoice);

		// Send request
		MockWoofResponse response = this.server.send(this.jwt
				.authorize(user, MockWoofServer.mockRequest("/payments/domain/MOCK_ORDER_ID")).method(HttpMethod.POST));

		// Ensure correct response
		CapturedPayment order = response.getJson(200, CapturedPayment.class);
		assertEquals("Incorrect order ID", "MOCK_ORDER_ID", order.getOrderId());
		assertEquals("Incorrect status", "COMPLETED", order.getStatus());
		assertEquals("Incorrect domain", "officefloor.org", order.getDomain());

		// Ensure payment captured
		Payment payment = this.objectify.get(Payment.class);
		assertEquals("Incorrect invoice", invoice.getId(), payment.getInvoice().get().getId());
		assertEquals("Incorrect user for payment", user.getId(), payment.getUser().get().getId());
		assertEquals("Incorrect product type", Domain.PRODUCT_TYPE, payment.getProductType());
		assertEquals("Incorrect product reference", "officefloor.org", payment.getProductReference());
		assertEquals("Incorrect amount", Integer.valueOf(5_00), payment.getAmount());
		assertEquals("Incorrect receipt", "CAPTURE_ID", payment.getReceipt());
		assertEquals("Incorrect restart subscription", Boolean.valueOf(false), payment.getIsRestartSubscription());
		assertNotNull("Should have payment timestamp", payment.getTimestamp());

		// Ensure domain capture in data store
		Domain domain = this.objectify.get(Domain.class);
		assertEquals("Incorrect domain on domain", "officefloor.org", domain.getDomain());
		assertNotNull("Should have expires", domain.getExpires());
		assertNotNull("Should have domain timestamp", domain.getTimestamp());
	}

}