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

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.braintreepayments.http.HttpResponse;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.Ref;
import com.paypal.core.PayPalHttpClient;
import com.paypal.orders.AmountBreakdown;
import com.paypal.orders.AmountWithBreakdown;
import com.paypal.orders.ApplicationContext;
import com.paypal.orders.Item;
import com.paypal.orders.Money;
import com.paypal.orders.Order;
import com.paypal.orders.OrderRequest;
import com.paypal.orders.OrdersCreateRequest;
import com.paypal.orders.PurchaseUnitRequest;

import lombok.Value;
import net.officefloor.app.subscription.store.Administration;
import net.officefloor.app.subscription.store.Domain;
import net.officefloor.app.subscription.store.Invoice;
import net.officefloor.app.subscription.store.User;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.HttpPathParameter;
import net.officefloor.web.HttpQueryParameter;
import net.officefloor.web.ObjectResponse;

/**
 * Logic to create {@link Invoice}.
 * 
 * @author Daniel Sagenschneider
 */
public class InvoiceService {

	/*
	 * Values in cents.
	 */

	private static final int SUBSCRIPTION_VALUE = 4_54;
	private static final int SUBSCRIPTION_TAX = 46;

	private static final int RESTART_VALUE = 18_18;
	private static final int RESTART_TAX = 1_82;

	private static final DecimalFormat formatCurrency = new DecimalFormat("0.00");

	@Value
	public static class CreatedInvoice {
		private String orderId;
		private String status;
		private String invoiceId;
	}

	public static void createInvoice(User user, @HttpPathParameter("domain") String domainName,
			@HttpQueryParameter("restart") String restart, Objectify objectify, PayPalHttpClient paypal,
			ObjectResponse<CreatedInvoice> response) throws IOException {

		// Validate the domain name
		domainName = domainName.trim();
		if (domainName.contains(" ") || domainName.startsWith(".") || domainName.endsWith(".")
				|| (!domainName.contains("."))) {
			throw new HttpException(422, "Invalid domain " + domainName);
		}

		// Determine if restart subscription
		boolean isRestart = Boolean.parseBoolean(restart);

		// Obtain the administration
		Administration administration = objectify.load().type(Administration.class).first().now();
		if (administration == null) {
			throw new HttpException(HttpStatus.SERVICE_UNAVAILABLE, "Server not initialised");
		}

		// Obtain the currency
		String currency = administration.getPaypalCurrency();
		Function<Integer, String> amount = (value) -> formatCurrency.format(value / 100.0);
		Function<Integer, Money> newMoney = (value) -> new Money().value(amount.apply(value)).currencyCode(currency);

		// Determine if restart required
		Domain domain = objectify.load().type(Domain.class).filter("domain", domainName).first().now();
		if (domain == null) {
			isRestart = false; // domain not registered, so no restart required
		} else if (domain.getExpires().toInstant().isAfter(Instant.now())) {
			isRestart = false; // domain not expires, so no restart required
		}

		// Create the invoice entry
		Invoice invoice = new Invoice(Ref.create(user), Domain.PRODUCT_TYPE, domainName, isRestart);
		objectify.save().entities(invoice).now();
		String invoiceId = String.valueOf(invoice.getId());

		// Calculate the PayPal unique invoice Id
		String paypalInvoiceId = administration.getPaypalInvoiceIdTemplate();
		paypalInvoiceId = paypalInvoiceId.replace("{id}", invoiceId);
		paypalInvoiceId = paypalInvoiceId.replace("{timestamp}", String.valueOf(System.currentTimeMillis()));

		// Load the items
		List<Item> items = new ArrayList<>(2);
		items.add(new Item().name("Subscription").description("12 month subscription for " + domainName)
				.unitAmount(newMoney.apply(SUBSCRIPTION_VALUE)).tax(newMoney.apply(SUBSCRIPTION_TAX)).quantity("1")
				.category("DIGITAL_GOODS"));
		if (isRestart) {
			items.add(new Item().name("Restart").description("Restart domain subscription for " + domainName)
					.unitAmount(newMoney.apply(RESTART_VALUE)).tax(newMoney.apply(RESTART_TAX)).quantity("1")
					.category("DIGITAL_GOODS"));
		}

		// Create order for the domain
		HttpResponse<Order> orderResponse = paypal.execute(new OrdersCreateRequest().requestBody(new OrderRequest()
				.checkoutPaymentIntent("CAPTURE")
				.applicationContext(new ApplicationContext().shippingPreference("NO_SHIPPING").userAction("PAY_NOW"))
				.purchaseUnits(Arrays.asList(new PurchaseUnitRequest()
						.invoiceId(
								paypalInvoiceId)
						.description("OfficeFloor 12 month subscription for " + domainName)
						.softDescriptor("OfficeFloor domain")
						.amountWithBreakdown(new AmountWithBreakdown()
								.value(amount.apply(SUBSCRIPTION_VALUE
										+ SUBSCRIPTION_TAX + (isRestart ? RESTART_VALUE + RESTART_TAX : 0)))
								.currencyCode(currency)
								.amountBreakdown(new AmountBreakdown()
										.itemTotal(newMoney.apply(SUBSCRIPTION_VALUE + (isRestart ? RESTART_VALUE : 0)))
										.taxTotal(newMoney.apply(SUBSCRIPTION_TAX + (isRestart ? RESTART_TAX : 0)))))
						.items(items)))));
		Order order = orderResponse.result();

		// Update the invoice with the order
		String paymentOrderId = order.id();
		invoice.setPaymentOrderId(paymentOrderId);
		objectify.save().entity(invoice).now();

		// Send the response
		response.send(new CreatedInvoice(order.id(), order.status(), invoiceId));
	}

}