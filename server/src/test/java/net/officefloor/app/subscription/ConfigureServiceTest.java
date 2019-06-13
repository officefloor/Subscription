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

import com.google.common.base.Function;

import net.officefloor.app.subscription.ConfigureService.Configuration;
import net.officefloor.app.subscription.ConfigureService.ConfigurationAdministrator;
import net.officefloor.app.subscription.ConfigureService.Configured;
import net.officefloor.app.subscription.store.Administration;
import net.officefloor.app.subscription.store.Administration.Administrator;
import net.officefloor.app.subscription.store.User;
import net.officefloor.nosql.objectify.mock.ObjectifyRule;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpMethod;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.jwt.mock.MockJwtAccessTokenRule;
import net.officefloor.woof.mock.MockWoofResponse;
import net.officefloor.woof.mock.MockWoofServer;
import net.officefloor.woof.mock.MockWoofServerRule;

/**
 * Tests configuring the application.
 * 
 * @author Daniel Sagenschneider
 */
public class ConfigureServiceTest {

	private final MockJwtAccessTokenRule jwt = new MockJwtAccessTokenRule();

	private final ObjectifyRule objectify = new ObjectifyRule();

	private final MockWoofServerRule server = new MockWoofServerRule();

	@Rule
	public RuleChain chain = RuleChain.outerRule(this.jwt).around(this.objectify).around(this.server);

	@Test
	public void getConfiguration() throws Exception {

		User user = TestHelper.newUser("Daniel", User.ROLE_ADMIN);

		// Configure administration
		Administration existing = new Administration("MOCK_GOOGLE_ID",
				new Administrator[] { new Administrator("MOCK_GOOGLE_ADMIN", "MOCK_NOTES") }, "sandbox",
				"MOCK_PAYPAL_ID", "MOCK_PAYPAL_SECRET", "MOCK_CURRENCY");
		this.objectify.store(existing);

		// Ensure can obtain configuration
		MockWoofResponse response = this.server.send(this.jwt
				.authorize(user, MockWoofServer.mockRequest("/configuration")).header("Accept", "application/json"));
		response.assertJson(200, new Configuration("MOCK_GOOGLE_ID",
				new ConfigurationAdministrator[] { new ConfigurationAdministrator("MOCK_GOOGLE_ADMIN", "MOCK_NOTES") },
				"sandbox", "MOCK_PAYPAL_ID", "MOCK_PAYPAL_SECRET", "MOCK_CURRENCY"));
	}

	@Test
	public void updateConfiguration() throws Exception {

		User user = TestHelper.newUser("Daniel", User.ROLE_ADMIN);

		// Configure administration
		Administration existing = new Administration("MOCK_OVERRIDE_GOOGLE_ID",
				new Administrator[] { new Administrator("MOCK_OVERRIDE_ADMIN", "MOCK_NOTES") }, "override",
				"MOCK_OVERRIDE_PAYPAL_ID", "MOCK_OVERRIDE_PAYPAL_SECRET", "MOCK_OVERRIDE_CURRENCY");
		this.objectify.store(existing);

		// Configure system
		final String UPDATED_GOOGLE_ID = "MOCK_GOOGLE_CLIENT_ID";
		ConfigurationAdministrator[] configurationAdministrators = new ConfigurationAdministrator[] {
				new ConfigurationAdministrator("MOCK_ADMIN_1", "MOCK_NOTES_1"),
				new ConfigurationAdministrator("MOCK_ADMIN_2", "MOCK_NOTES_2") };
		MockWoofResponse response = this.server.send(this.jwt.authorize(user,
				MockWoofServer.mockJsonRequest(HttpMethod.POST, "/configuration",
						new Configuration(UPDATED_GOOGLE_ID, configurationAdministrators, "sandbox",
								"MOCK_CLIENT_PAYPAL_ID", "MOCK_CLIENT_PAYPAL_SECRET", "MOCK_CURRENCY"))));
		response.assertJson(200, new Configured(true));

		// Ensure configured
		Administration admin = this.objectify.consistent(() -> this.objectify.get(Administration.class),
				(administration) -> UPDATED_GOOGLE_ID.equals(administration.getGoogleClientId()));
		assertEquals("Should only be one entry", existing.getId(), admin.getId());
		assertEquals("MOCK_GOOGLE_CLIENT_ID", admin.getGoogleClientId());
		assertAdministrators(configurationAdministrators, (a) -> a, admin.getAdministrators());
		assertEquals("sandbox", admin.getPaypalEnvironment());
		assertEquals("MOCK_CLIENT_PAYPAL_ID", admin.getPaypalClientId());
		assertEquals("MOCK_CLIENT_PAYPAL_SECRET", admin.getPaypalClientSecret());
		assertEquals("MOCK_CURRENCY", admin.getPaypalCurrency());
	}

	@Test
	public void onlyAdministratorGetsConfiguration() throws Exception {

		// Configure administration
		Administration administration = new Administration("MOCK_GOOGLE_CLIENT_ID",
				new Administrator[] { new Administrator("MOCK_ADMIN", "MOCK_NOTES") }, "sandbox",
				"MOCK_CLIENT_PAYPAL_ID", "MOCK_CLIENT_PAYPAL_SECRET", "MOCK_CURRENCY");
		this.objectify.store(administration);

		// Non-admin attempt to get configuration
		User user = TestHelper.newUser("Daniel");
		MockWoofResponse response = this.server.send(this.jwt
				.authorize(user, MockWoofServer.mockRequest("/configuration")).header("Accept", "application/json"));
		response.assertJsonError(new HttpException(HttpStatus.FORBIDDEN, "Forbidden"));
	}

	@Test
	public void onlyAdministratorUpdatesConfiguration() throws Exception {

		// Configure administration
		Administrator[] administrators = new Administrator[] { new Administrator("MOCK_ADMIN", "MOCK_NOTES") };
		Administration administration = new Administration("MOCK_GOOGLE_CLIENT_ID", administrators, "sandbox",
				"MOCK_CLIENT_PAYPAL_ID", "MOCK_CLIENT_PAYPAL_SECRET", "MOCK_CURRENCY");
		this.objectify.store(administration);

		// Non-admin attempt to configure
		User user = TestHelper.newUser("Daniel");
		ConfigurationAdministrator[] configurationAdministrators = new ConfigurationAdministrator[] {
				new ConfigurationAdministrator("CHANGE_ADMIN_1", "CHANGE_NOTES_1"),
				new ConfigurationAdministrator("CHANGE_ADMIN_2", "CHANGE_NOTES_2") };
		MockWoofResponse response = this.server.send(this.jwt.authorize(user,
				MockWoofServer.mockJsonRequest(HttpMethod.POST, "/configuration",
						new Configuration("CHANGE_GOOGLE_ID", configurationAdministrators, "changed",
								"CHANGE_PAYPAL_ID", "CHANGE_PAYPAL_SECRET", "CHANGE_CURRENCY"))));
		response.assertJsonError(new HttpException(HttpStatus.FORBIDDEN, "Forbidden"));

		// Ensure not changed
		Administration admin = this.objectify.get(Administration.class);
		assertEquals("Should only be one entry", administration.getId(), admin.getId());
		assertEquals("MOCK_GOOGLE_CLIENT_ID", admin.getGoogleClientId());
		assertAdministrators(administrators, (a) -> new ConfigurationAdministrator(a.getGoogleId(), a.getNotes()),
				admin.getAdministrators());
		assertEquals("sandbox", admin.getPaypalEnvironment());
		assertEquals("MOCK_CLIENT_PAYPAL_ID", admin.getPaypalClientId());
		assertEquals("MOCK_CLIENT_PAYPAL_SECRET", admin.getPaypalClientSecret());
		assertEquals("MOCK_CURRENCY", admin.getPaypalCurrency());
	}

	private static <T> void assertAdministrators(T[] configurationAdministrators,
			Function<T, ConfigurationAdministrator> getConfigurationAdministrator, Administrator[] administrators) {
		assertEquals("Incorrect number of administrators", configurationAdministrators.length, administrators.length);
		for (int i = 0; i < configurationAdministrators.length; i++) {
			ConfigurationAdministrator configurationAdministrator = getConfigurationAdministrator
					.apply(configurationAdministrators[i]);
			assertEquals(configurationAdministrator.getGoogleId(), administrators[i].getGoogleId());
			assertEquals(configurationAdministrator.getNotes(), administrators[i].getNotes());
		}
	}

}