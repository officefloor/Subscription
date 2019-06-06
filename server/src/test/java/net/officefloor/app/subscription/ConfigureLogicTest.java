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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.officefloor.app.subscription.ConfigureLogic.Configuration;
import net.officefloor.app.subscription.ConfigureLogic.Configured;
import net.officefloor.app.subscription.store.Administration;
import net.officefloor.app.subscription.store.User;
import net.officefloor.nosql.objectify.mock.ObjectifyRule;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpMethod;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.server.http.mock.MockHttpResponse;
import net.officefloor.web.json.JacksonHttpObjectResponderFactory;
import net.officefloor.web.jwt.mock.MockJwtAccessTokenRule;
import net.officefloor.woof.mock.MockWoofServer;
import net.officefloor.woof.mock.MockWoofServerRule;

/**
 * Tests configuring the application.
 * 
 * @author Daniel Sagenschneider
 */
public class ConfigureLogicTest {

	private static final ObjectMapper mapper = new ObjectMapper();

	private final MockJwtAccessTokenRule jwt = new MockJwtAccessTokenRule();

	private final ObjectifyRule objectify = new ObjectifyRule();

	private final MockWoofServerRule server = new MockWoofServerRule();

	@Rule
	public RuleChain chain = RuleChain.outerRule(this.jwt).around(this.objectify).around(this.server);

	@Test
	public void getConfiguration() throws Exception {

		User user = AuthenticateLogicTest.newUser("Daniel", User.ROLE_ADMIN);

		// Configure administration
		Administration existing = new Administration("MOCK_GOOGLE_ID", new String[] { "MOCK_GOOGLE_ADMIN" }, "sandbox",
				"MOCK_PAYPAL_ID", "MOCK_PAYPAL_SECRET", "MOCK_CURRENCY");
		this.objectify.store(existing);

		// Ensure can obtain configuration
		MockHttpResponse response = this.server.send(this.jwt
				.authorize(user, MockWoofServer.mockRequest("/configuration")).header("Accept", "application/json"));
		response.assertResponse(200,
				mapper.writeValueAsString(new Configuration("MOCK_GOOGLE_ID", new String[] { "MOCK_GOOGLE_ADMIN" },
						"sandbox", "MOCK_PAYPAL_ID", "MOCK_PAYPAL_SECRET", "MOCK_CURRENCY")));
	}

	@Test
	public void updateConfiguration() throws Exception {

		User user = AuthenticateLogicTest.newUser("Daniel", User.ROLE_ADMIN);

		// Configure administration
		Administration existing = new Administration("MOCK_OVERRIDE_GOOGLE_ID", new String[] { "MOCK_OVERRIDE_ADMIN" },
				"override", "MOCK_OVERRIDE_PAYPAL_ID", "MOCK_OVERRIDE_PAYPAL_SECRET", "MOCK_OVERRIDE_CURRENCY");
		this.objectify.store(existing);

		// Configure system
		MockHttpResponse response = this.server
				.send(this.jwt.authorize(user, MockWoofServer.mockRequest("/configuration")).method(HttpMethod.POST)
						.header("content-type", "application/json")
						.entity(mapper.writeValueAsString(new Configuration("MOCK_GOOGLE_CLIENT_ID",
								new String[] { "MOCK_ADMIN_1", "MOCK_ADMIN_2" }, "sandbox", "MOCK_CLIENT_PAYPAL_ID",
								"MOCK_CLIENT_PAYPAL_SECRET", "MOCK_CURRENCY"))));
		response.assertResponse(200, mapper.writeValueAsString(new Configured(true)));

		// Ensure configured
		Administration admin = this.objectify.consistent(() -> this.objectify.get(Administration.class),
				(administration) -> "MOCK_GOOGLE_CLIENT_ID".equals(administration.getGoogleClientId()));
		assertEquals("Should only be one entry", existing.getId(), admin.getId());
		assertEquals("MOCK_GOOGLE_CLIENT_ID", admin.getGoogleClientId());
		assertArrayEquals(new String[] { "MOCK_ADMIN_1", "MOCK_ADMIN_2" }, admin.getGoogleAdministratorIds());
		assertEquals("sandbox", admin.getPaypalEnvironment());
		assertEquals("MOCK_CLIENT_PAYPAL_ID", admin.getPaypalClientId());
		assertEquals("MOCK_CLIENT_PAYPAL_SECRET", admin.getPaypalClientSecret());
		assertEquals("MOCK_CURRENCY", admin.getPaypalCurrency());
	}

	@Test
	public void onlyAdministratorGetsConfiguration() throws Exception {

		// Configure administration
		Administration administration = new Administration("MOCK_GOOGLE_CLIENT_ID", new String[] { "MOCK_ADMIN" },
				"sandbox", "MOCK_CLIENT_PAYPAL_ID", "MOCK_CLIENT_PAYPAL_SECRET", "MOCK_CURRENCY");
		this.objectify.store(administration);

		// Non-admin attempt to get configuration
		User user = AuthenticateLogicTest.newUser("Daniel");
		MockHttpResponse response = this.server.send(this.jwt
				.authorize(user, MockWoofServer.mockRequest("/configuration")).header("Accept", "application/json"));
		response.assertResponse(403, JacksonHttpObjectResponderFactory
				.getEntity(new HttpException(HttpStatus.FORBIDDEN, "Forbidden"), mapper));
	}

	@Test
	public void onlyAdministratorUpdatesConfiguration() throws Exception {

		// Configure administration
		Administration administration = new Administration("MOCK_GOOGLE_CLIENT_ID", new String[] { "MOCK_ADMIN" },
				"sandbox", "MOCK_CLIENT_PAYPAL_ID", "MOCK_CLIENT_PAYPAL_SECRET", "MOCK_CURRENCY");
		this.objectify.store(administration);

		// Non-admin attempt to configure
		User user = AuthenticateLogicTest.newUser("Daniel");
		MockHttpResponse response = this.server
				.send(this.jwt.authorize(user, MockWoofServer.mockRequest("/configuration")).method(HttpMethod.POST)
						.header("content-type", "application/json")
						.entity(mapper.writeValueAsString(new Configuration("CHANGE_GOOGLE_ID",
								new String[] { "CHANGE_ADMIN_1", "CHANGE_ADMIN_2" }, "changed", "CHANGE_PAYPAL_ID",
								"CHANGE_PAYPAL_SECRET", "CHANGE_CURRENCY"))));
		response.assertResponse(403, JacksonHttpObjectResponderFactory
				.getEntity(new HttpException(HttpStatus.FORBIDDEN, "Forbidden"), mapper));

		// Ensure not changed
		Administration admin = this.objectify.get(Administration.class);
		assertEquals("Should only be one entry", administration.getId(), admin.getId());
		assertEquals("MOCK_GOOGLE_CLIENT_ID", admin.getGoogleClientId());
		assertArrayEquals(new String[] { "MOCK_ADMIN" }, admin.getGoogleAdministratorIds());
		assertEquals("sandbox", admin.getPaypalEnvironment());
		assertEquals("MOCK_CLIENT_PAYPAL_ID", admin.getPaypalClientId());
		assertEquals("MOCK_CLIENT_PAYPAL_SECRET", admin.getPaypalClientSecret());
		assertEquals("MOCK_CURRENCY", admin.getPaypalCurrency());
	}

}