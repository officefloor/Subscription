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
import java.util.Arrays;

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
import net.officefloor.web.ObjectResponse;

/**
 * Logic to create {@link Invoice}.
 * 
 * @author Daniel Sagenschneider
 */
public class InvoiceService {

	private static final String SUBSCRIPTION_VALUE = "4.54";
	private static final String SUBSCRIPTION_TAX = "0.46";
	private static final String SUBSCRIPTION_TOTAL = "5.00";

	@Value
	public static class CreatedInvoice {
		private String orderId;
		private String status;
		private String invoiceId;
	}

	public static void createInvoice(User user, @HttpPathParameter("domain") String domainName, Objectify objectify,
			PayPalHttpClient paypal, ObjectResponse<CreatedInvoice> response) throws IOException {

		// TODO validate the domain name

		// Obtain the administration
		Administration administration = objectify.load().type(Administration.class).first().now();
		if (administration == null) {
			throw new HttpException(HttpStatus.SERVICE_UNAVAILABLE, "Server not initialised");
		}

		// Obtain the currency
		String currency = administration.getPaypalCurrency();

		// Create the invoice entry
		Invoice invoice = new Invoice(Ref.create(user), Domain.PRODUCT_TYPE, domainName);
		objectify.save().entities(invoice).now();
		String invoiceId = String.valueOf(invoice.getId());

		// Create order for the domain
		HttpResponse<Order> orderResponse = paypal.execute(new OrdersCreateRequest().requestBody(new OrderRequest()
				.intent("CAPTURE")
				.applicationContext(new ApplicationContext().shippingPreference("NO_SHIPPING").userAction("PAY_NOW"))
				.purchaseUnits(Arrays
						.asList(new PurchaseUnitRequest().description("OfficeFloor domain subscription " + domainName)
								.softDescriptor("OfficeFloor domain").invoiceId(invoiceId)
								.amount(new AmountWithBreakdown().value(SUBSCRIPTION_TOTAL).currencyCode(currency)
										.breakdown(new AmountBreakdown()
												.itemTotal(new Money().value(SUBSCRIPTION_VALUE).currencyCode(currency))
												.taxTotal(new Money().value(SUBSCRIPTION_TAX).currencyCode(currency))))
								.items(Arrays.asList(new Item().name("Subscription")
										.description("Domain subscription " + domainName)
										.unitAmount(new Money().value(SUBSCRIPTION_VALUE).currencyCode(currency))
										.tax(new Money().value(SUBSCRIPTION_TAX).currencyCode(currency)).quantity("1")
										.category("DIGITAL_GOODS").url("http://" + domainName)))))));
		Order order = orderResponse.result();

		// Update the invoice with the order
		String paymentOrderId = order.id();
		invoice.setPaymentOrderId(paymentOrderId);
		objectify.save().entity(invoice).now();

		// Send the response
		response.send(new CreatedInvoice(order.id(), order.status(), invoiceId));
	}

}