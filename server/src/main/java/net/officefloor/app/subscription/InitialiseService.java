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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.objectify.Objectify;

import lombok.Value;
import net.officefloor.app.subscription.store.Administration;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.HttpObject;
import net.officefloor.web.ObjectResponse;

/**
 * Provides initialisation details.
 * 
 * @author Daniel Sagenschneider
 */
public class InitialiseService {

	public static final String PROPERTY_INITIALISE_FILE_PATH = "initialise.file.path";

	@Value
	@HttpObject
	public static class Initialisation {
		@JsonProperty("isAuthenticationRequired")
		private boolean isAuthenticationRequired;
		private String googleClientId;
		private String paypalClientId;
		private String paypalCurrency;
	}

	public static Administration getAdministration(Objectify objectify) throws IOException {

		// Retrieve the administration
		Administration administration = objectify.load().type(Administration.class).first().now();
		if (administration == null) {

			// Determine if use initialisation file
			String initialiseFilePath = System.getProperty(PROPERTY_INITIALISE_FILE_PATH, null);
			if (initialiseFilePath == null) {
				// Not system property, so try environment
				initialiseFilePath = System.getenv(PROPERTY_INITIALISE_FILE_PATH.replace('.', '_'));
			}
			if (initialiseFilePath != null) {

				// Determine if class path or file system
				InputStream initialiseInputStream;
				final String CLASSPATH_PREFIX = "classpath://";
				if (initialiseFilePath.startsWith(CLASSPATH_PREFIX)) {
					// Load from class path
					String resourcePath = initialiseFilePath.substring(CLASSPATH_PREFIX.length());
					initialiseInputStream = InitialiseService.class.getClassLoader().getResourceAsStream(resourcePath);

				} else {
					// Load from file system
					Path initialiseFile = Paths.get(initialiseFilePath);
					initialiseInputStream = Files.exists(initialiseFile) ? Files.newInputStream(initialiseFile) : null;
				}
				if (initialiseInputStream != null) {
					// Load the input
					ObjectMapper mapper = new ObjectMapper();
					mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
					administration = mapper.readValue(initialiseInputStream, Administration.class);

					// Save initialised administration
					objectify.save().entities(administration).now();
				}
			}

			// Determine if application configured
			if (administration == null) {
				throw new HttpException(HttpStatus.INTERNAL_SERVER_ERROR, "Application not configured");
			}
		}

		// Return the administration
		return administration;
	}

	public static void getInitialisation(Objectify objectify, ObjectResponse<Initialisation> response, Logger logger)
			throws IOException {

		// Obtain the administration
		Administration administration = getAdministration(objectify);

		// Response with the initialisation details
		response.send(new Initialisation(true, administration.getGoogleClientId(), administration.getPaypalClientId(),
				administration.getPaypalCurrency()));
	}

}