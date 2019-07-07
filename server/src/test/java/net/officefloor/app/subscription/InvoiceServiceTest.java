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
import static org.junit.Assert.assertTrue;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.googlecode.objectify.Ref;
import com.paypal.orders.ApplicationContext;
import com.paypal.orders.Item;
import com.paypal.orders.Order;
import com.paypal.orders.OrderRequest;
import com.paypal.orders.PurchaseUnitRequest;

import net.officefloor.app.subscription.InvoiceService.CreatedInvoice;
import net.officefloor.app.subscription.store.Administration;
import net.officefloor.app.subscription.store.Domain;
import net.officefloor.app.subscription.store.Invoice;
import net.officefloor.app.subscription.store.Payment;
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

	private final TestHelper helper = new TestHelper(this.objectify);

	@Test
	public void notInitialised() {

		// Attempt to create order
		User user = this.helper.setupUser("Daniel");
		MockWoofResponse response = this.server
				.send(this.jwt.authorize(user, MockWoofServer.mockRequest("/invoices/domain/officefloor.org"))
						.secure(true).method(HttpMethod.POST));
		response.assertJsonError(new HttpException(HttpStatus.SERVICE_UNAVAILABLE, "Server not initialised"));
	}

	@Test
	public void invalidDomain() {

		// Provide user
		User user = this.helper.setupUser("Daniel");

		// Function to validate domain
		Consumer<String> validate = (domainName) -> {
			MockWoofResponse response = this.server
					.send(this.jwt.authorize(user, MockWoofServer.mockRequest("/invoices/domain/" + domainName))
							.secure(true).method(HttpMethod.POST));
			response.assertJsonError(new HttpException(422, "Invalid domain " + domainName));
		};

		// Validate invalid domain names
		validate.accept("with space");
		validate.accept(".starting_dot");
		validate.accept("ending_dot.");
		validate.accept("no_dot");
	}

	@Test
	public void createInvoice() {
		this.doCreateInvoiceTest("officefloor.org", false);
	}

	@Test
	public void createInvoiceWithRestartButNotRegisteredDomain() {
		this.doCreateInvoiceTest("officefloor.org?restart=true", false);
	}

	@Test
	public void createInvoiceWithRestart() {

		// Setup expired domain
		User user = this.helper.setupUser("Daniel");
		Payment payment = this.helper.setupPayment(Ref.create(user), "officefloor.org", false,
				TestHelper.now().minus(3, ChronoUnit.YEARS));
		this.helper.setupDomain(payment);

		// Ensure restart domain
		this.doCreateInvoiceTest("officefloor.org?restart=true", true);
	}

	@Test
	public void createInvoiceWithRestartButNotExpiredDomain() {

		// Setup expired domain
		User user = this.helper.setupUser("Daniel");
		Payment payment = this.helper.setupPayment(Ref.create(user), "officefloor.org", false, TestHelper.now());
		this.helper.setupDomain(payment);

		// Ensure restart domain
		this.doCreateInvoiceTest("officefloor.org?restart=true", false);
	}

	private void doCreateInvoiceTest(String urlSuffix, boolean isRestart) {

		// Initialise
		Administration admin = this.helper.setupAdministration();
		String currency = admin.getPaypalCurrency();

		// Obtain domain URL
		String domainUrl = "http://" + urlSuffix.split("\\?")[0];

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
			assertTrue("PayPal invoice ID: " + purchase.invoiceId(),
					purchase.invoiceId().matches("^MOCK_PAYPAL_INVOICE_\\d+$"));
			assertEquals(isRestart ? "25.00" : "5.00", purchase.amount().value());
			assertEquals(currency, purchase.amount().currencyCode());
			assertEquals(isRestart ? "22.72" : "4.54", purchase.amount().breakdown().itemTotal().value());
			assertEquals(currency, purchase.amount().breakdown().itemTotal().currencyCode());
			assertEquals(isRestart ? "2.28" : "0.46", purchase.amount().breakdown().taxTotal().value());
			assertEquals(currency, purchase.amount().breakdown().taxTotal().currencyCode());
			assertEquals("Incorrect number of purchase items", isRestart ? 2 : 1, purchase.items().size());
			Item item = purchase.items().get(0);
			assertEquals("Subscription", item.name());
			assertEquals("Domain subscription officefloor.org", item.description());
			assertEquals("4.54", item.unitAmount().value());
			assertEquals(currency, item.unitAmount().currencyCode());
			assertEquals("0.46", item.tax().value());
			assertEquals(currency, item.tax().currencyCode());
			assertEquals("1", item.quantity());
			assertEquals("DIGITAL_GOODS", item.category());
			assertEquals(domainUrl, item.url());
			if (isRestart) {
				item = purchase.items().get(1);
				assertEquals("Restart", item.name());
				assertEquals("Restart domain subscription", item.description());
				assertEquals("18.18", item.unitAmount().value());
				assertEquals(currency, item.unitAmount().currencyCode());
				assertEquals("1.82", item.tax().value());
				assertEquals(currency, item.tax().currencyCode());
				assertEquals("1", item.quantity());
				assertEquals("DIGITAL_GOODS", item.category());
				assertEquals(domainUrl, item.url());
			}
		});

		// Send request
		User user = this.helper.setupUser("Daniel");
		MockWoofResponse response = this.server
				.send(this.jwt.authorize(user, MockWoofServer.mockRequest("/invoices/domain/" + urlSuffix)).secure(true)
						.method(HttpMethod.POST));

		// Ensure correct response
		CreatedInvoice order = response.getJson(200, CreatedInvoice.class);
		assertEquals("Incorrect order ID", "MOCK_ORDER_ID", order.getOrderId());
		assertEquals("Incorrect status", "CREATED", order.getStatus());
		assertNotNull("Should have invoice ID" + order.getInvoiceId());

		// Ensure invoice captured in data store
		Invoice invoice = this.objectify.get(Invoice.class, Long.parseLong(order.getInvoiceId()));
		assertEquals("Incorrect invoiced user", user.getId(), invoice.getUser().get().getId());
		assertEquals("Incorrect product type", Domain.PRODUCT_TYPE, invoice.getProductType());
		assertEquals("Incorrect invoiced domain", "officefloor.org", invoice.getProductReference());
		assertEquals("Incorrect payment order id", "MOCK_ORDER_ID", invoice.getPaymentOrderId());
		assertEquals("Incorrect restart", Boolean.valueOf(isRestart), invoice.getIsRestartSubscription());
		assertNotNull("Should have invoice timestamp", invoice.getTimestamp());
	}

}