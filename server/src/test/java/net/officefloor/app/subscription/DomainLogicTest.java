package net.officefloor.app.subscription;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.objectify.Ref;
import com.paypal.orders.ApplicationContext;
import com.paypal.orders.Item;
import com.paypal.orders.Order;
import com.paypal.orders.OrderRequest;
import com.paypal.orders.PurchaseUnitRequest;

import net.officefloor.app.subscription.DomainLogic.DomainCapturedOrder;
import net.officefloor.app.subscription.DomainLogic.DomainCreatedOrder;
import net.officefloor.app.subscription.DomainLogic.DomainPaymentsResponse;
import net.officefloor.app.subscription.DomainLogic.DomainResponse;
import net.officefloor.app.subscription.DomainLogic.PaymentResponse;
import net.officefloor.app.subscription.store.Administration;
import net.officefloor.app.subscription.store.Administration.Administrator;
import net.officefloor.app.subscription.store.Domain;
import net.officefloor.app.subscription.store.Invoice;
import net.officefloor.app.subscription.store.ObjectifyEntities;
import net.officefloor.app.subscription.store.Payment;
import net.officefloor.app.subscription.store.User;
import net.officefloor.nosql.objectify.mock.ObjectifyRule;
import net.officefloor.pay.paypal.mock.PayPalRule;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpMethod;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.server.http.mock.MockHttpResponse;
import net.officefloor.web.jwt.mock.MockJwtAccessTokenRule;
import net.officefloor.woof.mock.MockWoofResponse;
import net.officefloor.woof.mock.MockWoofServer;
import net.officefloor.woof.mock.MockWoofServerRule;

/**
 * Tests creating the PayPal order for a domain.
 * 
 * @author Daniel Sagenschneider
 */
public class DomainLogicTest {

	private static final ObjectMapper mapper = new ObjectMapper();

	private final MockJwtAccessTokenRule jwt = new MockJwtAccessTokenRule();

	private final PayPalRule payPal = new PayPalRule();

	private final ObjectifyRule objectify = new ObjectifyRule();

	private final MockWoofServerRule server = new MockWoofServerRule();

	@Rule
	public RuleChain chain = RuleChain.outerRule(this.jwt).around(this.payPal).around(this.objectify)
			.around(this.server);

	@Test
	public void notInitialised() throws Exception {

		// Attempt to create order
		User user = AuthenticateLogicTest.setupUser(this.objectify, "Daniel");
		MockWoofResponse response = this.server
				.send(this.jwt.authorize(user, MockWoofServer.mockRequest("/invoices/domain/officefloor.org"))
						.method(HttpMethod.POST));
		response.assertJsonError(new HttpException(HttpStatus.SERVICE_UNAVAILABLE, "Server not initialised"));
	}

	@Test
	public void createDomainOrder() throws Exception {

		// Initialise
		String CURRENCY = "MOCK_AUD";
		this.objectify.store(new Administration("MOCK_GOOGLE_ID", new Administrator[0], "sandbox", "MOCK_PAYPAL_ID",
				"MOCK_PAYPAL_SECRET", CURRENCY));

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
			assertNotNull(purchase.invoiceId());
			assertEquals("5.00", purchase.amount().value());
			assertEquals(CURRENCY, purchase.amount().currencyCode());
			assertEquals("4.54", purchase.amount().breakdown().itemTotal().value());
			assertEquals(CURRENCY, purchase.amount().breakdown().itemTotal().currencyCode());
			assertEquals("0.46", purchase.amount().breakdown().taxTotal().value());
			assertEquals(CURRENCY, purchase.amount().breakdown().taxTotal().currencyCode());
			assertEquals("Should only be one item", 1, purchase.items().size());
			Item item = purchase.items().get(0);
			assertEquals("Subscription", item.name());
			assertEquals("Domain subscription officefloor.org", item.description());
			assertEquals("4.54", item.unitAmount().value());
			assertEquals(CURRENCY, item.unitAmount().currencyCode());
			assertEquals("0.46", item.tax().value());
			assertEquals(CURRENCY, item.tax().currencyCode());
			assertEquals("1", item.quantity());
			assertEquals("DIGITAL_GOODS", item.category());
			assertEquals("http://officefloor.org", item.url());
		});

		// Send request
		User user = AuthenticateLogicTest.setupUser(this.objectify, "Daniel");
		MockHttpResponse response = this.server
				.send(this.jwt.authorize(user, MockWoofServer.mockRequest("/invoices/domain/officefloor.org"))
						.method(HttpMethod.POST));

		// Ensure correct response
		String entity = response.getEntity(null);
		assertEquals("Should be successful: " + entity, 200, response.getStatus().getStatusCode());
		DomainCreatedOrder order = mapper.readValue(entity, DomainCreatedOrder.class);
		assertEquals("Incorrect order ID", "MOCK_ORDER_ID", order.getOrderId());
		assertEquals("Incorrect status", "CREATED", order.getStatus());
		assertNotNull("Should have invoice", order.getInvoiceId());

		// Ensure invoice captured in data store
		Invoice invoice = this.objectify.get(Invoice.class, Long.parseLong(order.getInvoiceId()));
		assertEquals("Incorrect invoiced user", user.getId(), invoice.getUser().get().getId());
		assertEquals("Incorrect product type", Domain.PRODUCT_TYPE, invoice.getProductType());
		assertEquals("Incorrect invoiced domain", "officefloor.org", invoice.getProductReference());
		assertEquals("Incorrect payment order id", "MOCK_ORDER_ID", invoice.getPaymentOrderId());
		assertNotNull("Should have invoice timestamp", invoice.getTimestamp());
	}

	@Test
	public void captureDomainOrder() throws Exception {

		// Record
		this.payPal.addOrdersCaptureResponse(new Order().id("MOCK_ORDER_ID").status("COMPLETED"))
				.validate((request) -> {
					assertEquals("MOCK_ORDER_ID", this.payPal.getOrderId(request));
				});

		// Setup the invoice
		User user = AuthenticateLogicTest.setupUser(this.objectify, "Daniel");
		Invoice invoice = new Invoice(Ref.create(user), Domain.PRODUCT_TYPE, "officefloor.org");
		invoice.setPaymentOrderId("MOCK_ORDER_ID");
		this.objectify.store(invoice);

		// Send request
		MockHttpResponse response = this.server.send(this.jwt
				.authorize(user, MockWoofServer.mockRequest("/payments/domain/MOCK_ORDER_ID")).method(HttpMethod.POST));

		// Ensure correct response
		String entity = response.getEntity(null);
		assertEquals("Should be successful: " + entity, 200, response.getStatus().getStatusCode());
		DomainCapturedOrder order = mapper.readValue(entity, DomainCapturedOrder.class);
		assertEquals("Incorrect order ID", "MOCK_ORDER_ID", order.getOrderId());
		assertEquals("Incorrect status", "COMPLETED", order.getStatus());
		assertEquals("Incorrect domain", "officefloor.org", order.getDomain());

		// Ensure payment captured
		Payment payment = this.objectify.get(Payment.class);
		assertEquals("Incorrect invoice", invoice.getId(), payment.getInvoice().get().getId());
		assertEquals("Incorrect user for payment", user.getId(), payment.getUser().get().getId());
		assertEquals("Incorrect amount", Integer.valueOf(5_00), payment.getAmount());
		assertEquals("Incorrect receipt", "CAPTURE_ID", payment.getReceipt());

		// Ensure domain capture in data store
		Domain domain = this.objectify.get(Domain.class);
		assertEquals("Incorrect domain on domain", "officefloor.org", domain.getDomain());
		assertNotNull("Should have expires", domain.getExpires());
		assertNotNull("Should have domain timestamp", domain.getTimestamp());
	}

	@Test
	public void noDomainsPaid() throws Exception {

		// Setup user for payments
		User user = AuthenticateLogicTest.setupUser(this.objectify, "Daniel");

		// Obtain the domains
		MockWoofResponse response = this.server.send(this.jwt.authorize(user, MockWoofServer.mockRequest("/domains")));
		response.assertJson(200, new DomainResponse[0]);
	}

	@Test
	public void getDomains() throws Exception {

		// Setup user for payments
		User user = AuthenticateLogicTest.setupUser(this.objectify, "Daniel");
		Ref<User> userRef = Ref.create(user);

		// Setup other user
		User anotherUser = AuthenticateLogicTest.setupUser(this.objectify, "Another");
		Ref<User> anotherUserRef = Ref.create(anotherUser);

		// Load the payments
		ZonedDateTime now = ZonedDateTime.now(ObjectifyEntities.ZONE);
		this.setupPayment(userRef, "officefloor.org", false, now.minus(2, ChronoUnit.YEARS));
		this.setupPayment(userRef, "officefloor.org", false, now.minus(1, ChronoUnit.YEARS));
		this.setupPayment(userRef, "officefloor.org", false, now);
		this.setupPayment(userRef, "activicy.com", false, now);
		this.setupPayment(anotherUserRef, "not.returned", false, now);
		this.setupPayment(userRef, "missing.domain", false, now);

		// Create the expire dates
		ZonedDateTime expireOfficeFloor = Instant.now().atZone(ObjectifyEntities.ZONE).plus(1, ChronoUnit.YEARS);
		ZonedDateTime expireActivicy = expireOfficeFloor.plus(2, ChronoUnit.WEEKS);
		ZonedDateTime expireNotReturned = expireOfficeFloor.plus(3, ChronoUnit.MONTHS);

		// Load the domains
		Domain domainOfficeFloor = new Domain("officefloor.org", Date.from(expireOfficeFloor.toInstant()));
		Domain domainActivicy = new Domain("activicy.com", Date.from(expireActivicy.toInstant()));
		Domain domainNotReturned = new Domain("not.returned", Date.from(expireNotReturned.toInstant()));
		this.objectify.store(domainOfficeFloor, domainActivicy, domainNotReturned);

		// Obtain the domains
		MockWoofResponse response = this.server.send(this.jwt.authorize(user, MockWoofServer.mockRequest("/domains")));
		response.assertJson(200,
				new DomainResponse[] { new DomainResponse("officefloor.org", toText(expireOfficeFloor)),
						new DomainResponse("activicy.com", toText(expireActivicy)) });
	}

	@Test
	public void noAccessToUnpaidDomain() throws Exception {

		// Setup user for payments
		User user = AuthenticateLogicTest.setupUser(this.objectify, "Daniel");

		// Setup another user
		User anotherUser = AuthenticateLogicTest.setupUser(this.objectify, "Another");

		// Load the domain and payments for another user
		this.setupPayment(Ref.create(anotherUser), "officefloor.org", false, ZonedDateTime.now(ObjectifyEntities.ZONE));
		this.objectify
				.store(new Domain("officefloor.org", Date.from(ZonedDateTime.now(ObjectifyEntities.ZONE).toInstant())));

		// Obtain the payments
		MockWoofResponse response = this.server
				.send(this.jwt.authorize(user, MockWoofServer.mockRequest("/payments/domain/officefloor.org")));
		response.assertJsonError(new HttpException(403, "No payment to access domain officefloor.org"));
	}

	@Test
	public void getDomainPayments() throws Exception {

		// Setup user for payments
		User user = AuthenticateLogicTest.setupUser(this.objectify, "Daniel");
		Ref<User> userRef = Ref.create(user);

		// Load the payments
		ZonedDateTime now = ZonedDateTime.now(ObjectifyEntities.ZONE);
		ZonedDateTime firstPaymentDate = now.minus(4, ChronoUnit.YEARS);
		ZonedDateTime secondPaymentDate = now.minus(2, ChronoUnit.YEARS);
		ZonedDateTime thirdPaymentDate = now.minus(1, ChronoUnit.YEARS);
		this.setupPayment(userRef, "officefloor.org", false, firstPaymentDate);
		this.setupPayment(userRef, "officefloor.org", true, secondPaymentDate);
		this.setupPayment(userRef, "officefloor.org", false, thirdPaymentDate);

		// Obtain the payments
		MockWoofResponse response = this.server
				.send(this.jwt.authorize(user, MockWoofServer.mockRequest("/payments/domain/officefloor.org")));
		response.assertJson(200, new DomainPaymentsResponse("officefloor.org", toText(now), new PaymentResponse[] {
				new PaymentResponse(toText(thirdPaymentDate), toText(now), false, "Daniel", "daniel@officefloor.org",
						null),
				new PaymentResponse(toText(secondPaymentDate), toText(thirdPaymentDate), true, "Daniel",
						"daniel@officefloor.org", null),
				new PaymentResponse(toText(firstPaymentDate), toText(firstPaymentDate.plus(1, ChronoUnit.YEARS)), false,
						"Daniel", "daniel@officefloor.org", null) }));
	}

	@Test
	public void getOverlappingDomainPayments() throws Exception {

		// Setup user for payments
		User user = AuthenticateLogicTest.setupUser(this.objectify, "Daniel");
		Ref<User> userRef = Ref.create(user);

		// Load the payments
		ZonedDateTime now = ZonedDateTime.now(ObjectifyEntities.ZONE);
		final int NUMBER_OF_PAYMENTS = 10;
		for (int i = 0; i < NUMBER_OF_PAYMENTS; i++) {
			this.setupPayment(userRef, "officefloor.org", false, now.plus(i, ChronoUnit.SECONDS));
		}

		// Load the expected payment responses
		PaymentResponse[] payments = new PaymentResponse[NUMBER_OF_PAYMENTS];
		for (int i = 0; i < NUMBER_OF_PAYMENTS; i++) {
			payments[NUMBER_OF_PAYMENTS - 1 - i] = new PaymentResponse(toText(now.plus(i, ChronoUnit.SECONDS)),
					toText(now.plus(i + 1, ChronoUnit.YEARS)), false, "Daniel", "daniel@officefloor.org", null);
		}

		// Obtain the payments
		MockWoofResponse response = this.server
				.send(this.jwt.authorize(user, MockWoofServer.mockRequest("/payments/domain/officefloor.org")));
		response.assertJson(200,
				new DomainPaymentsResponse("officefloor.org", payments[0].getExtendsToDate(), payments));
	}

	private Payment setupPayment(Ref<User> userRef, String domain, boolean isRestart, ZonedDateTime timestamp) {

		// Create the invoice
		Invoice invoice = new Invoice(userRef, Domain.PRODUCT_TYPE, domain);
		this.objectify.store(invoice);

		// Create the payment
		Payment payment = new Payment(userRef, Ref.create(invoice), Domain.PRODUCT_TYPE, domain, isRestart, 500,
				"R" + toText(timestamp));
		payment.setTimestamp(Date.from(timestamp.toInstant()));
		this.objectify.store(payment);

		// Return the setup payment
		return payment;
	}

	private static String toText(ZonedDateTime date) {
		return DateTimeFormatter.RFC_1123_DATE_TIME.format(date);
	}

}