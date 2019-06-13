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
import java.util.ArrayList;
import java.util.List;

import com.braintreepayments.http.HttpResponse;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.Ref;
import com.paypal.core.PayPalHttpClient;
import com.paypal.orders.Order;
import com.paypal.orders.OrdersCaptureRequest;

import lombok.Value;
import net.officefloor.app.subscription.store.Domain;
import net.officefloor.app.subscription.store.Invoice;
import net.officefloor.app.subscription.store.Payment;
import net.officefloor.app.subscription.store.User;
import net.officefloor.plugin.section.clazz.Next;
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
	public static class CapturedPayment {
		private String orderId;
		private String status;
		private String domain;
	}

	@Next("UpdateDomain")
	public static Payment[] capturePayment(User user, @HttpPathParameter("orderId") String orderId, Objectify objectify,
			PayPalHttpClient paypal, ObjectResponse<CapturedPayment> response) throws IOException {

		// Obtain the invoice
		Invoice invoice = objectify.load().type(Invoice.class).filter("paymentOrderId", orderId).first().now();
		if (invoice == null) {
			throw new HttpException(HttpStatus.INTERNAL_SERVER_ERROR, "No invoice for orderId " + orderId);
		}
		String domainName = invoice.getProductReference();
		boolean isRestartSubscription = invoice.getIsRestartSubscription();

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

		// Obtain payments for the domain
		List<Payment> payments = new ArrayList<>();
		for (Payment payment : objectify.load().type(Payment.class).filter("productReference", domainName).iterable()) {
			if (Domain.PRODUCT_TYPE.equals(payment.getProductType())) {
				payments.add(payment);
			}
		}

		// Funds captured, so create entries
		Ref<User> userRef = Ref.create(user);
		Ref<Invoice> invoiceRef = Ref.create(invoice);
		Payment payment = new Payment(userRef, invoiceRef, Domain.PRODUCT_TYPE, domainName, isRestartSubscription,
				amount, receipt);
		objectify.save().entities(payment).now();

		// Include payment
		payments.add(payment);

		// Send the captured payment
		response.send(new CapturedPayment(orderId, captureStatus, domainName));

		// Return the payments
		return payments.toArray(new Payment[payments.size()]);
	}

}