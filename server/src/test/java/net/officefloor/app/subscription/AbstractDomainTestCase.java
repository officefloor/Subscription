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
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.junit.Rule;
import org.junit.rules.RuleChain;

import com.googlecode.objectify.Ref;

import net.officefloor.app.subscription.store.Domain;
import net.officefloor.app.subscription.store.Invoice;
import net.officefloor.app.subscription.store.Payment;
import net.officefloor.app.subscription.store.User;
import net.officefloor.nosql.objectify.mock.ObjectifyRule;
import net.officefloor.web.jwt.mock.MockJwtAccessTokenRule;
import net.officefloor.woof.mock.MockWoofServerRule;

/**
 * Abstract domain test logic.
 * 
 * @author Daniel Sagenschneider
 */
public abstract class AbstractDomainTestCase {

	protected final MockJwtAccessTokenRule jwt = new MockJwtAccessTokenRule();

	protected final ObjectifyRule objectify = new ObjectifyRule();

	protected final MockWoofServerRule server = new MockWoofServerRule();

	@Rule
	public final RuleChain chain = RuleChain.outerRule(this.jwt).around(this.objectify).around(this.server);

	protected Payment setupPayment(Ref<User> userRef, String domain, boolean isRestart, ZonedDateTime timestamp) {

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

	protected static String toText(ZonedDateTime date) {
		return DateTimeFormatter.RFC_1123_DATE_TIME.format(date);
	}

}