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

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import net.officefloor.app.subscription.SubscriptionService.DomainPayments;
import net.officefloor.app.subscription.store.Payment;
import net.officefloor.app.subscription.store.User;
import net.officefloor.plugin.section.clazz.Next;
import net.officefloor.plugin.section.clazz.Parameter;

/**
 * Calculates the {@link DomainPayments}.
 * 
 * @author Daniel Sagenschneider
 */
public class SubscriptionCalculator {

	@Value
	public static class Subscription {
		private String productReference;
		private ZonedDateTime paymentDate;
		private ZonedDateTime extendsToDate;
		private boolean isRestartSubscription;
		private User paidBy;
		private String paymentOrderId;
		private String paymentReceipt;
		private Integer paymentAmount;
	}

	@Data
	@RequiredArgsConstructor
	private static class PaymentState {
		private final Payment payment;
		private final User payer;
		private final ZonedDateTime paymentDate;
		private final String paymentOrderId;
		private final String paymentReceipt;
		private final Integer paymentAmount;
		private ZonedDateTime extendsToDate = null;
	}

	@Next("Subscription")
	public static Subscription[] calculateSubscriptions(User user, @Parameter Payment... payments) {

		// Ensure have payments
		if ((payments == null) || (payments.length == 0)) {
			return new Subscription[0];
		}

		// Determine if user is admin
		boolean isAdmin = User.isAdmin(user);

		// Obtain payments for the domain
		List<PaymentState> paymentStates = new ArrayList<>(payments.length);
		NEXT_PAYMENT: for (Payment payment : payments) {

			// Ignore payment if refunded
			if ((payment.getRefund() != null) && (payment.getRefund().get() != null)) {
				continue NEXT_PAYMENT;
			}

			// Obtain details of payment
			ZonedDateTime paymentDate = payment.getTimestamp().toInstant().atZone(ResponseUtil.ZONE);
			User payer = payment.getUser().get();
			String paymentOrderId = payment.getInvoice().get().getPaymentOrderId();
			String paymentReceipt = payment.getReceipt();
			Integer amount = payment.getAmount();

			// Determine if paid by user
			if ((!isAdmin) && ((payer == null) || (!user.getId().equals(payer.getId())))) {
				// Not paid by user (so no access to details)
				payer = null;
				paymentOrderId = null;
				paymentReceipt = null;
				amount = null;
			}

			// Add payment
			paymentStates.add(new PaymentState(payment, payer, paymentDate, paymentOrderId, paymentReceipt, amount));
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
		return Stream.of(states)
				.map((state) -> new Subscription(state.getPayment().getProductReference(), state.getPaymentDate(),
						state.getExtendsToDate(), state.getPayment().getIsRestartSubscription(), state.getPayer(),
						state.getPaymentOrderId(), state.getPaymentReceipt(), state.getPaymentAmount()))
				.toArray(Subscription[]::new);
	}

}