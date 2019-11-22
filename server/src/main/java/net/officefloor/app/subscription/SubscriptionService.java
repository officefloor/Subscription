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

import static net.officefloor.app.subscription.ResponseUtil.toText;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import com.googlecode.objectify.Objectify;

import lombok.Value;
import net.officefloor.app.subscription.SubscriptionCalculator.Subscription;
import net.officefloor.app.subscription.store.Domain;
import net.officefloor.app.subscription.store.Payment;
import net.officefloor.app.subscription.store.User;
import net.officefloor.plugin.section.clazz.Parameter;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.HttpPathParameter;
import net.officefloor.web.ObjectResponse;

/**
 * Obtains the {@link Domain} {@link Payment} details.
 * 
 * @author Daniel Sagenschneider
 */
public class SubscriptionService {

	private static final String DOMAIN_PATH_PARAMETER = "domainName";

	@Value
	public static class DomainPayments {
		private String domainName;
		private String expiresDate;
		private DomainPayment[] payments;
	}

	@Value
	public static class DomainPayment {
		private String paymentDate;
		private String extendsToDate;
		private boolean restartSubscription;
		private String paidByName;
		private String paidByEmail;
		private String paymentOrderId;
		private String paymentReceipt;
		private Integer paymentAmount;
	}

	public static Payment[] getDomainPayments(User user, @HttpPathParameter(DOMAIN_PATH_PARAMETER) String domainName,
			Objectify objectify) throws IOException {

		/*
		 * Note: too many payments is good problem to have. Means will have funds to
		 * spend time writing fix.
		 */

		// Obtain payments for the domain
		List<Payment> payments = new ArrayList<>();
		boolean isPaidForDomain = false;
		for (Payment payment : objectify.load().type(Payment.class).filter("productReference", domainName).iterable()) {
			if (Domain.PRODUCT_TYPE.equals(payment.getProductType())) {

				// Add the payment
				payments.add(payment);

				// Determine if user paid for domain
				User payer = payment.getUser().get();
				if ((payer != null) && (user.getId().equals(payer.getId()))) {
					isPaidForDomain = true;
				}
			}
		}

		// Ensure paid to retrieve domain information
		if (!isPaidForDomain) {
			throw new HttpException(HttpStatus.FORBIDDEN, "No payment to access domain " + domainName);
		}

		// Return the payments
		return payments.toArray(new Payment[payments.size()]);
	}

	public static DomainPayments translateToDomainPayments(@Parameter Subscription[] subscriptions) {

		// Create domain payments from subscriptions
		Function<Subscription, String> name = (subscription) -> subscription == null ? null
				: subscription.getPaidBy().getName();
		Function<Subscription, String> email = (subscription) -> subscription == null ? null
				: subscription.getPaidBy().getEmail();
		DomainPayment[] domainPayments = Stream.of(subscriptions).map((subscription) -> {
			return new DomainPayment(toText(subscription.getPaymentDate()), toText(subscription.getExtendsToDate()),
					subscription.isRestartSubscription(), name.apply(subscription), email.apply(subscription),
					subscription.getPaymentOrderId(), subscription.getPaymentReceipt(),
					subscription.getPaymentAmount());
		}).toArray(DomainPayment[]::new);

		// Obtain the domain
		String domainName = subscriptions.length > 0 ? subscriptions[0].getProductReference() : null;
		String extendsToDate = subscriptions.length > 0 ? toText(subscriptions[0].getExtendsToDate()) : null;

		// Return the domain payments
		return new DomainPayments(domainName, extendsToDate, domainPayments);
	}

	public static void sendSubscriptions(@Parameter Subscription[] subscriptions,
			ObjectResponse<DomainPayments> response) {

		// Translate to domain payments
		DomainPayments domainPayments = translateToDomainPayments(subscriptions);

		// Send the response
		response.send(domainPayments);
	}

}