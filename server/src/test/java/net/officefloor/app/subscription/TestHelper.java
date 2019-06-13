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

import com.googlecode.objectify.Ref;

import lombok.Value;
import net.officefloor.app.subscription.store.Domain;
import net.officefloor.app.subscription.store.Invoice;
import net.officefloor.app.subscription.store.Payment;
import net.officefloor.app.subscription.store.User;
import net.officefloor.nosql.objectify.mock.ObjectifyRule;

/**
 * Domain helper logic.
 * 
 * @author Daniel Sagenschneider
 */
@Value
public class TestHelper {

	private ObjectifyRule objectify;

	/**
	 * Converts {@link ZonedDateTime} to network date format.
	 * 
	 * @param date Date.
	 * @return Network date text.
	 */
	public static String toText(ZonedDateTime date) {
		return DateTimeFormatter.RFC_1123_DATE_TIME.format(date);
	}

	/**
	 * Creates a new {@link User} without storing in database.
	 * 
	 * @param name  Name of {@link User}.
	 * @param roles Roles.
	 * @return {@link User}.
	 */
	public static User newUser(String name, String... roles) {
		String dotName = name.replaceAll("\\s", ".").toLowerCase();
		String noSpaceName = name.replaceAll("\\s", "_");
		User user = new User(dotName + "@officefloor.org");
		user.setName(name);
		user.setPhotoUrl("https://google.com/" + noSpaceName + "_photo.png");
		user.setRoles(roles);
		return user;
	}

	/**
	 * Sets up a {@link User} in database.
	 * 
	 * @param name  Name of {@link User}.
	 * @param roles Roles.
	 * @return {@link User}.
	 */
	public User setupUser(String name, String... roles) {
		User user = newUser(name, roles);
		this.objectify.store(user);
		return user;
	}

	/**
	 * Sets up a {@link Payment} in the database.
	 * 
	 * @param userRef   {@link User}.
	 * @param domain    Domain name.
	 * @param isRestart Is restart description.
	 * @param timestamp Timestamp for {@link Payment}.
	 * @return {@link Payment}.
	 */
	public Payment setupPayment(Ref<User> userRef, String domain, boolean isRestart, ZonedDateTime timestamp) {

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

}