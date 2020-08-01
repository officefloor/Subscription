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
package net.officefloor.app.subscription.google;

import java.io.IOException;
import java.util.Collections;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.googlecode.objectify.Objectify;

import net.officefloor.app.subscription.InitialiseService;
import net.officefloor.app.subscription.store.Administration;
import net.officefloor.identity.google.GoogleIdTokenVerifierFactory;
import net.officefloor.plugin.clazz.Dependency;

/**
 * {@link GoogleIdTokenVerifierFactory} to load {@link GoogleIdTokenVerifier}
 * from {@link Administration}.
 * 
 * @author Daniel Sagenschneider
 */
public class GoogleIdTokenVerifierConfiguration implements GoogleIdTokenVerifierFactory {

	private @Dependency Objectify objectify;

	@Override
	public GoogleIdTokenVerifier create() throws Exception {

		// Obtain the configuration
		Administration admin;
		try {
			admin = InitialiseService.getAdministration(this.objectify);
			if (admin == null) {
				return null;
			}
		} catch (IOException ex) {
			return null;
		}
		String googleClientId = admin.getGoogleClientId();

		// Create and return verifier
		JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
		HttpTransport transport = new NetHttpTransport();
		return new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
				.setAudience(Collections.singletonList(googleClientId)).build();
	}

}