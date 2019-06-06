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

import java.util.Iterator;

import com.googlecode.objectify.Objectify;

import lombok.Value;
import net.officefloor.app.subscription.store.Administration;
import net.officefloor.app.subscription.store.User;
import net.officefloor.web.HttpObject;
import net.officefloor.web.ObjectResponse;
import net.officefloor.web.security.HttpAccess;

/**
 * Enables configuring the application.
 * 
 * @author Daniel Sagenschneider
 */
public class ConfigureLogic {

	@Value
	@HttpObject
	public static class Configuration {
		private String googleClientId;
		private String[] googleAdministratorIds;
		private String paypalEnvironment;
		private String paypalClientId;
		private String paypalClientSecret;
		private String paypalCurrency;
	}

	@Value
	public static class Configured {
		private boolean successful;
	}

	@HttpAccess(ifRole = User.ROLE_ADMIN)
	public static void getConfiguration(Objectify objectify, ObjectResponse<Configuration> response) {

		// Obtain the administration
		Administration admin = objectify.load().type(Administration.class).first().now();
		if (admin == null) {
			admin = new Administration();
		}

		// Load and return the configuration
		Configuration configuration = new Configuration(admin.getGoogleClientId(), admin.getGoogleAdministratorIds(),
				admin.getPaypalEnvironment(), admin.getPaypalClientId(), admin.getPaypalClientSecret(),
				admin.getPaypalCurrency());
		response.send(configuration);
	}

	@HttpAccess(ifRole = User.ROLE_ADMIN)
	public static void updateConfiguration(User user, Configuration configuration, Objectify objectify,
			ObjectResponse<Configured> response) {

		// Obtain the administration (ensuring only one entry)
		Administration administration = null;
		Iterator<Administration> iterator = objectify.load().type(Administration.class).iterator();
		if (iterator.hasNext()) {
			administration = iterator.next();
		}
		iterator.forEachRemaining((extra) -> objectify.delete().entity(extra).now());

		// Updating configuration
		administration.setGoogleClientId(configuration.getGoogleClientId());
		administration.setGoogleAdministratorIds(configuration.getGoogleAdministratorIds());
		administration.setPaypalEnvironment(configuration.getPaypalEnvironment());
		administration.setPaypalClientId(configuration.getPaypalClientId());
		administration.setPaypalClientSecret(configuration.getPaypalClientSecret());
		administration.setPaypalCurrency(configuration.getPaypalCurrency());
		objectify.save().entity(administration).now();

		// Successfully configured
		response.send(new Configured(true));
	}

}