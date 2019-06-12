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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.googlecode.objectify.Objectify;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import net.officefloor.app.subscription.store.Domain;
import net.officefloor.app.subscription.store.ObjectifyEntities;
import net.officefloor.app.subscription.store.Payment;
import net.officefloor.app.subscription.store.User;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.HttpPathParameter;
import net.officefloor.web.ObjectResponse;

/**
 * Obtains the {@link Domain} {@link Payment} details.
 * 
 * @author Daniel Sagenschneider
 */
public class DomainPaymentLogic {

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
		private boolean isRestartSubscription;
		private String paidByName;
		private String paidByEmail;
		private String paymentOrderId;
	}

	@Data
	@RequiredArgsConstructor
	private static class PaymentState {
		private final Payment payment;
		private final User payer;
		private final boolean isPaidByUser;
		private final ZonedDateTime paymentDate;
		private ZonedDateTime extendsToDate = null;
	}

	public static void getDomainPayments(User user, @HttpPathParameter("domainName") String domainName,
			Objectify objectify, ObjectResponse<DomainPayments> response) throws IOException {

		/*
		 * Note: too many payments is good problem to have. Means will have funds to
		 * spend time writing fix.
		 */

		// Obtain payments for the domain
		List<PaymentState> paymentStates = new ArrayList<>();
		boolean isPaidForDomain = false;
		for (Payment payment : objectify.load().type(Payment.class).filter("productReference", domainName).iterable()) {
			if (Domain.PRODUCT_TYPE.equals(payment.getProductType())) {

				// Determine if paid by user
				User payer = payment.getUser().get();
				boolean isPaidByUser = (payer != null) && (user.getId().equals(payer.getId()));

				// Add payment
				paymentStates.add(new PaymentState(payment, payer, isPaidByUser,
						payment.getTimestamp().toInstant().atZone(ObjectifyEntities.ZONE)));

				// Determine if user paid for domain
				if (isPaidByUser) {
					isPaidForDomain = true;
				}
			}
		}

		// Ensure paid to retrieve domain information
		if (!isPaidForDomain) {
			throw new HttpException(HttpStatus.FORBIDDEN, "No payment to access domain " + domainName);
		}

		// Sort the payment states reverse chronologically
		PaymentState[] states = paymentStates.stream()
				.sorted((a, b) -> b.getPaymentDate().compareTo(a.getPaymentDate())).toArray(PaymentState[]::new);

		// Determine the extends to dates (always at least one payment)
		int lastIndex = states.length - 1;
		ZonedDateTime expiresToDate = states[lastIndex].getPaymentDate().plus(1, ChronoUnit.YEARS);
		states[lastIndex].setExtendsToDate(expiresToDate);
		for (int i = lastIndex - 1; i >= 0; i--) {
			PaymentState state = states[i];

			// Determine if payment after expires date (and reset)
			if (state.getPaymentDate().isAfter(expiresToDate) && state.getPayment().getIsRestartSubscription()) {
				expiresToDate = state.getPaymentDate(); // restart subscription from payment
			}

			// Determine expires to date
			expiresToDate = expiresToDate.plus(1, ChronoUnit.YEARS);
			state.setExtendsToDate(expiresToDate);
		}

		// Construct response
		DomainPayment[] payments = Stream.of(states)
				.map((state) -> new DomainPayment(ResponseUtil.toText(state.getPayment().getTimestamp()),
						ResponseUtil.toText(state.getExtendsToDate()), state.getPayment().getIsRestartSubscription(),
						state.getPayer().getName(), state.getPayer().getEmail(),
						state.getPayment().getInvoice().get().getPaymentOrderId()))
				.toArray(DomainPayment[]::new);
		response.send(new DomainPayments(domainName, payments[0].getExtendsToDate(), payments));
	}

}