package net.officefloor.app.subscription;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

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
import com.paypal.orders.OrdersCaptureRequest;
import com.paypal.orders.OrdersCreateRequest;
import com.paypal.orders.PurchaseUnitRequest;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import net.officefloor.app.subscription.store.Administration;
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
 * Logic to retrieve the domain entries.
 * 
 * @author Daniel Sagenschneider
 */
public class DomainLogic {

	private static final String SUBSCRIPTION_VALUE = "4.54";
	private static final String SUBSCRIPTION_TAX = "0.46";
	private static final String SUBSCRIPTION_TOTAL = "5.00";

	@Value
	public static class DomainCreatedOrder {
		private String orderId;
		private String status;
		private String invoiceId;
	}

	public static void createOrder(User user, @HttpPathParameter("domain") String domainName, Objectify objectify,
			PayPalHttpClient paypal, ObjectResponse<DomainCreatedOrder> response) throws IOException {

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
		response.send(new DomainCreatedOrder(order.id(), order.status(), invoiceId));
	}

	@Value
	public static class DomainCapturedOrder {
		private String orderId;
		private String status;
		private String domain;
	}

	public static void captureOrder(User user, @HttpPathParameter("orderId") String orderId, Objectify objectify,
			PayPalHttpClient paypal, ObjectResponse<DomainCapturedOrder> response) throws IOException {

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
		response.send(new DomainCapturedOrder(orderId, captureStatus, domainName));
	}

	@Value
	public static class DomainResponse {
		private String domainName;
		private String expireDate;
	}

	public static void getDomains(User user, Objectify objectify, ObjectResponse<DomainResponse[]> response)
			throws IOException {

		// Obtain the payments
		Set<String> domainNames = new HashSet<>();
		for (Payment payment : objectify.load().type(Payment.class).filter("user", Ref.create(user)).iterable()) {
			domainNames.add(payment.getProductReference());
		}

		// Obtain the domains
		List<Domain> domains = new ArrayList<>(domainNames.size());
		for (String domainName : domainNames) {
			Domain domain = objectify.load().type(Domain.class).filter("domain", domainName).first().now();
			if (domain != null) {
				domains.add(domain);
			}
		}

		// Return the domains
		DomainResponse[] domainResponses = domains.stream().sorted((a, b) -> a.getExpires().compareTo(b.getExpires()))
				.map((domain) -> new DomainResponse(domain.getDomain(), toText(domain.getExpires())))
				.toArray(DomainResponse[]::new);
		response.send(domainResponses);
	}

	@Value
	public static class DomainPaymentsResponse {
		private String domainName;
		private String expiresDate;
		private PaymentResponse[] payments;
	}

	@Value
	public static class PaymentResponse {
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
			Objectify objectify, ObjectResponse<DomainPaymentsResponse> response) throws IOException {

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
		PaymentResponse[] payments = Stream.of(states)
				.map((state) -> new PaymentResponse(toText(state.getPayment().getTimestamp()),
						toText(state.getExtendsToDate()), state.getPayment().getIsRestartSubscription(),
						state.getPayer().getName(), state.getPayer().getEmail(),
						state.getPayment().getInvoice().get().getPaymentOrderId()))
				.toArray(PaymentResponse[]::new);
		response.send(new DomainPaymentsResponse(domainName, payments[0].getExtendsToDate(), payments));
	}

	private static String toText(Date date) {
		return toText(date.toInstant().atZone(ObjectifyEntities.ZONE));
	}

	private static String toText(ZonedDateTime date) {
		return DateTimeFormatter.RFC_1123_DATE_TIME.format(date);
	}

}