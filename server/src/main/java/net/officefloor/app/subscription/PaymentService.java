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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.Ref;
import com.paypal.core.PayPalHttpClient;
import com.paypal.orders.Capture;
import com.paypal.orders.Order;
import com.paypal.orders.OrdersCaptureRequest;
import com.paypal.orders.PurchaseUnit;

import lombok.Value;
import net.officefloor.app.subscription.SubscriptionCalculator.Subscription;
import net.officefloor.app.subscription.SubscriptionService.DomainPayments;
import net.officefloor.app.subscription.store.Domain;
import net.officefloor.app.subscription.store.Invoice;
import net.officefloor.app.subscription.store.Payment;
import net.officefloor.app.subscription.store.User;
import net.officefloor.plugin.section.clazz.Next;
import net.officefloor.plugin.section.clazz.Parameter;
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

	private static ObjectMapper mapper = new ObjectMapper();

	public static Payment[] capturePayment(User user, @HttpPathParameter("orderId") String orderId, Objectify objectify,
			PayPalHttpClient paypal) throws IOException {

		// Obtain the invoice
		Invoice invoice = objectify.load().type(Invoice.class).filter("paymentOrderId", orderId).first().now();
		if (invoice == null) {
			throw new HttpException(HttpStatus.INTERNAL_SERVER_ERROR, "No invoice for orderId " + orderId);
		}
		String domainName = invoice.getProductReference();
		boolean isRestartSubscription = invoice.getIsRestartSubscription();

		// Capture the funds
		HttpResponse<Order> orderResponse;
		try {
			orderResponse = paypal.execute(new OrdersCaptureRequest(orderId));
		} catch (IOException ex) {

			// Obtain the error message
			String errorMessage = ex.getMessage();

			// Determine if extract PayPal error
			try {
				PayPalError paypalError = mapper.readValue(ex.getMessage(), PayPalError.class);
				if ((paypalError.details != null) && (paypalError.details.length > 0)) {
					errorMessage = paypalError.details[0].getDescription();
				}
			} catch (IOException ignoreJsonEx) {
				// just take as text error
			}

			// Indicate failure in paypal
			throw new HttpException(HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
		}
		Order order = orderResponse.result();
		String captureStatus = order.status();
		if (!"COMPLETED".equalsIgnoreCase(captureStatus)) {
			throw new HttpException(HttpStatus.PAYMENT_REQUIRED);
		}

		// Obtain the payment details
		int amount = 0;
		String receipt = null;
		if (order.purchaseUnits().size() > 0) {
			PurchaseUnit purchaseUnit = order.purchaseUnits().get(0);
			if (purchaseUnit.payments().captures().size() > 0) {
				Capture capture = purchaseUnit.payments().captures().get(0);
				amount = Math.round(Float.parseFloat(capture.amount().value()) * 100);
				receipt = capture.id();
			}
		}

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

		// Return the payments
		return payments.toArray(new Payment[payments.size()]);
	}

	public static void sendPayment(User user, @Parameter Subscription[] subscriptions,
			ObjectResponse<DomainPayments> response) {

		// Obtain the domain payments
		DomainPayments domainPayments = SubscriptionService.translateToDomainPayments(subscriptions);

		// Send the response
		response.send(domainPayments);
	}

	@Value
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class PayPalError {
		PayPalIssue[] details;
	}

	@Value
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class PayPalIssue {
		String description;
	}

}