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
	public void configure() throws Exception {

		// Configure system
		User user = AuthenticateLogicTest.setupUser(this.objectify, "Daniel");
		MockHttpResponse response = this.server.send(this.jwt.authorize(user, MockWoofServer.mockRequest("/configure"))
				.method(HttpMethod.POST).header("content-type", "application/json")
				.entity(mapper.writeValueAsString(new Configuration("MOCK_GOOGLE_CLIENT_ID", "sandbox",
						"MOCK_CLIENT_PAYPAL_ID", "MOCK_CLIENT_PAYPAL_SECRET", "MOCK_CURRENCY"))));
		response.assertResponse(200, mapper.writeValueAsString(new Configured(true)));

		// Ensure configured
		Administration admin = this.objectify.get(Administration.class);
		assertEquals("MOCK_GOOGLE_CLIENT_ID", admin.getGoogleClientId());
		assertEquals("sandbox", admin.getPaypalEnvironment());
		assertEquals("MOCK_CLIENT_PAYPAL_ID", admin.getPaypalClientId());
		assertEquals("MOCK_CLIENT_PAYPAL_SECRET", admin.getPaypalClientSecret());
		assertEquals("MOCK_CURRENCY", admin.getPaypalCurrency());

		// User should be flagged as administrator
		User adminUser = this.objectify.get(User.class, user.getId());
		assertEquals("Incorrect user", "Daniel", adminUser.getName());
		assertEquals("Should now have role", 1, adminUser.getRoles().length);
		assertEquals("Should be admin", "admin", adminUser.getRoles()[0]);
	}

	@Test
	public void reConfigure() throws Exception {

		User user = AuthenticateLogicTest.setupUser(this.objectify, "Daniel", "admin");

		// Configure administration
		Administration existing = new Administration("MOCK_OVERRIDE_GOOGLE_ID", "override", "MOCK_OVERRIDE_PAYPAL_ID",
				"MOCK_OVERRIDE_PAYPAL_SECRET", "MOCK_OVERRIDE_CURRENCY");
		this.objectify.store(existing);

		// Configure system
		MockHttpResponse response = this.server.send(this.jwt.authorize(user, MockWoofServer.mockRequest("/configure"))
				.method(HttpMethod.POST).header("content-type", "application/json")
				.entity(mapper.writeValueAsString(new Configuration("MOCK_GOOGLE_CLIENT_ID", "sandbox",
						"MOCK_CLIENT_PAYPAL_ID", "MOCK_CLIENT_PAYPAL_SECRET", "MOCK_CURRENCY"))));
		response.assertResponse(200, mapper.writeValueAsString(new Configured(true)));

		// Ensure configured
		Administration admin = this.objectify.consistent(() -> this.objectify.get(Administration.class),
				(administration) -> "MOCK_GOOGLE_CLIENT_ID".equals(administration.getGoogleClientId()));
		assertEquals("Should only be one entry", existing.getId(), admin.getId());
		assertEquals("MOCK_GOOGLE_CLIENT_ID", admin.getGoogleClientId());
		assertEquals("sandbox", admin.getPaypalEnvironment());
		assertEquals("MOCK_CLIENT_PAYPAL_ID", admin.getPaypalClientId());
		assertEquals("MOCK_CLIENT_PAYPAL_SECRET", admin.getPaypalClientSecret());
		assertEquals("MOCK_CURRENCY", admin.getPaypalCurrency());
	}

	@Test
	public void onlyAdministratorConfigures() throws Exception {

		// Configure administration
		Administration administration = new Administration("MOCK_GOOGLE_CLIENT_ID", "sandbox", "MOCK_CLIENT_PAYPAL_ID",
				"MOCK_CLIENT_PAYPAL_SECRET", "MOCK_CURRENCY");
		this.objectify.store(administration);

		// Non-admin attempt to configure
		User user = AuthenticateLogicTest.setupUser(this.objectify, "Daniel");
		MockHttpResponse response = this.server.send(this.jwt.authorize(user, MockWoofServer.mockRequest("/configure"))
				.method(HttpMethod.POST).header("content-type", "application/json")
				.entity(mapper.writeValueAsString(new Configuration("CHANGE_GOOGLE_ID", "changed", "CHANGE_PAYPAL_ID",
						"CHANGE_PAYPAL_SECRET", "CHANGE_CURRENCY"))));
		response.assertResponse(403, JacksonHttpObjectResponderFactory
				.getEntity(new HttpException(HttpStatus.FORBIDDEN, "Must have 'admin' role"), mapper));

		// Ensure not changed
		Administration admin = this.objectify.get(Administration.class);
		assertEquals("Should only be one entry", administration.getId(), admin.getId());
		assertEquals("MOCK_GOOGLE_CLIENT_ID", admin.getGoogleClientId());
		assertEquals("sandbox", admin.getPaypalEnvironment());
		assertEquals("MOCK_CLIENT_PAYPAL_ID", admin.getPaypalClientId());
		assertEquals("MOCK_CLIENT_PAYPAL_SECRET", admin.getPaypalClientSecret());
		assertEquals("MOCK_CURRENCY", admin.getPaypalCurrency());
	}

}