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

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.paypal.orders.ApplicationContext;
import com.paypal.orders.Item;
import com.paypal.orders.Order;
import com.paypal.orders.OrderRequest;
import com.paypal.orders.PurchaseUnitRequest;

import net.officefloor.app.subscription.InvoiceService.CreatedInvoice;
import net.officefloor.app.subscription.store.Administration;
import net.officefloor.app.subscription.store.Administration.Administrator;
import net.officefloor.app.subscription.store.Domain;
import net.officefloor.app.subscription.store.Invoice;
import net.officefloor.app.subscription.store.User;
import net.officefloor.nosql.objectify.mock.ObjectifyRule;
import net.officefloor.pay.paypal.mock.PayPalRule;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpMethod;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.jwt.mock.MockJwtAccessTokenRule;
import net.officefloor.woof.mock.MockWoofResponse;
import net.officefloor.woof.mock.MockWoofServer;
import net.officefloor.woof.mock.MockWoofServerRule;

/**
 * Tests creating invoices.
 * 
 * @author Daniel Sagenschneider
 */
public class InvoiceServiceTest {

	private final MockJwtAccessTokenRule jwt = new MockJwtAccessTokenRule();

	private final PayPalRule payPal = new PayPalRule();

	private final ObjectifyRule objectify = new ObjectifyRule();

	private final MockWoofServerRule server = new MockWoofServerRule();

	@Rule
	public RuleChain chain = RuleChain.outerRule(this.jwt).around(this.payPal).around(this.objectify)
			.around(this.server);

	@Test
	public void notInitialised() throws Exception {

		// Attempt to create order
		User user = AuthenticateServiceTest.setupUser(this.objectify, "Daniel");
		MockWoofResponse response = this.server
				.send(this.jwt.authorize(user, MockWoofServer.mockRequest("/invoices/domain/officefloor.org"))
						.method(HttpMethod.POST));
		response.assertJsonError(new HttpException(HttpStatus.SERVICE_UNAVAILABLE, "Server not initialised"));
	}

	@Test
	public void createDomainOrder() throws Exception {

		// Initialise
		String CURRENCY = "MOCK_AUD";
		this.objectify.store(new Administration("MOCK_GOOGLE_ID", new Administrator[0], "sandbox", "MOCK_PAYPAL_ID",
				"MOCK_PAYPAL_SECRET", CURRENCY));

		// Record
		this.payPal.addOrdersCreateResponse(new Order().id("MOCK_ORDER_ID").status("CREATED")).validate((request) -> {
			OrderRequest order = (OrderRequest) request.requestBody();
			assertEquals("CAPTURE", order.intent());
			ApplicationContext appContext = order.applicationContext();
			assertEquals("NO_SHIPPING", appContext.shippingPreference());
			assertEquals("PAY_NOW", appContext.userAction());
			List<PurchaseUnitRequest> purchaseUnits = order.purchaseUnits();
			assertEquals("Should be one domain purchased", 1, purchaseUnits.size());
			PurchaseUnitRequest purchase = purchaseUnits.get(0);
			assertEquals("OfficeFloor domain subscription officefloor.org", purchase.description());
			assertEquals("OfficeFloor domain", purchase.softDescriptor());
			assertNotNull(purchase.invoiceId());
			assertEquals("5.00", purchase.amount().value());
			assertEquals(CURRENCY, purchase.amount().currencyCode());
			assertEquals("4.54", purchase.amount().breakdown().itemTotal().value());
			assertEquals(CURRENCY, purchase.amount().breakdown().itemTotal().currencyCode());
			assertEquals("0.46", purchase.amount().breakdown().taxTotal().value());
			assertEquals(CURRENCY, purchase.amount().breakdown().taxTotal().currencyCode());
			assertEquals("Should only be one item", 1, purchase.items().size());
			Item item = purchase.items().get(0);
			assertEquals("Subscription", item.name());
			assertEquals("Domain subscription officefloor.org", item.description());
			assertEquals("4.54", item.unitAmount().value());
			assertEquals(CURRENCY, item.unitAmount().currencyCode());
			assertEquals("0.46", item.tax().value());
			assertEquals(CURRENCY, item.tax().currencyCode());
			assertEquals("1", item.quantity());
			assertEquals("DIGITAL_GOODS", item.category());
			assertEquals("http://officefloor.org", item.url());
		});

		// Send request
		User user = AuthenticateServiceTest.setupUser(this.objectify, "Daniel");
		MockWoofResponse response = this.server
				.send(this.jwt.authorize(user, MockWoofServer.mockRequest("/invoices/domain/officefloor.org"))
						.method(HttpMethod.POST));

		// Ensure correct response
		CreatedInvoice order = response.getJson(200, CreatedInvoice.class);
		assertEquals("Incorrect order ID", "MOCK_ORDER_ID", order.getOrderId());
		assertEquals("Incorrect status", "CREATED", order.getStatus());
		assertNotNull("Should have invoice", order.getInvoiceId());

		// Ensure invoice captured in data store
		Invoice invoice = this.objectify.get(Invoice.class, Long.parseLong(order.getInvoiceId()));
		assertEquals("Incorrect invoiced user", user.getId(), invoice.getUser().get().getId());
		assertEquals("Incorrect product type", Domain.PRODUCT_TYPE, invoice.getProductType());
		assertEquals("Incorrect invoiced domain", "officefloor.org", invoice.getProductReference());
		assertEquals("Incorrect payment order id", "MOCK_ORDER_ID", invoice.getPaymentOrderId());
		assertNotNull("Should have invoice timestamp", invoice.getTimestamp());
	}

}