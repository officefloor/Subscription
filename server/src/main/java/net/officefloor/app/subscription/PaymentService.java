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
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import com.braintreepayments.http.HttpResponse;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.Ref;
import com.paypal.core.PayPalHttpClient;
import com.paypal.orders.Order;
import com.paypal.orders.OrdersCaptureRequest;

import lombok.Value;
import net.officefloor.app.subscription.store.Domain;
import net.officefloor.app.subscription.store.Invoice;
import net.officefloor.app.subscription.store.ObjectifyEntities;
import net.officefloor.app.subscription.store.Payment;
import net.officefloor.app.subscription.store.User;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.HttpPathParameter;
import net.officefloor.web.ObjectResponse;

/**
 * Logic to create {@link Payment}.
 * 
 * @author Daniel Sagenschneider
 */
public class PaymentService {

	@Value
	public static class CreatedPayment {
		private String orderId;
		private String status;
		private String domain;
	}

	public static void capturePayment(User user, @HttpPathParameter("orderId") String orderId, Objectify objectify,
			PayPalHttpClient paypal, ObjectResponse<CreatedPayment> response) throws IOException {

		// Obtain the invoice
		Invoice invoice = objectify.load().type(Invoice.class).filter("paymentOrderId", orderId).first().now();
		String domainName = invoice.getProductReference();

		// Capture the funds
		HttpResponse<Order> orderResponse = paypal.execute(new OrdersCaptureRequest(orderId));
		Order order = orderResponse.result();
		String captureStatus = order.status();
		if (!"COMPLETED".equalsIgnoreCase(captureStatus)) {
			throw new HttpException(HttpStatus.PAYMENT_REQUIRED);
		}

		// TODO extract amount and receipt from response
		int amount = 500;
		String receipt = "CAPTURE_ID";

		// Funds captured, so create entries
		Ref<User> userRef = Ref.create(user);
		Ref<Invoice> invoiceRef = Ref.create(invoice);
		Payment payment = new Payment(userRef, invoiceRef, Domain.PRODUCT_TYPE, domainName, false, amount, receipt);
		objectify.save().entities(payment).now();

		// TODO trigger service to load domain
		ZonedDateTime expiresDate = ZonedDateTime.now(ObjectifyEntities.ZONE).plus(1, ChronoUnit.YEARS);
		Domain domain = new Domain(domainName, Date.from(expiresDate.toInstant()));
		objectify.save().entities(domain).now();

		// Send the response
		response.send(new CreatedPayment(orderId, captureStatus, domainName));
	}

}