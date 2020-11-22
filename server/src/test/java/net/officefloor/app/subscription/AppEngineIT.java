package net.officefloor.app.subscription;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.datastore.Datastore;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;

import lombok.Data;
import net.officefloor.app.subscription.store.Administration;
import net.officefloor.maven.IntegrationAppEngine;
import net.officefloor.server.http.HttpClientRule;

/**
 * Ensure working within Google App Engine.
 * 
 * @author Daniel Sagenschneider
 */
public class AppEngineIT {

	private static final ObjectMapper mapper = new ObjectMapper();

	@BeforeClass
	public static void setup() throws Exception {

		// Obtain the datastore
		Datastore datastore = IntegrationAppEngine.getDatastore();

		// Initialise datastore
		ObjectifyFactory objectifyFactory = new ObjectifyFactory(datastore);
		ObjectifyService.init(objectifyFactory);
		ObjectifyService.register(Administration.class);
		Administration admin = new Administration();
		admin.setGoogleClientId("TEST CLIENT");
		ObjectifyService.run(() -> {
			return ObjectifyService.ofy().save().entities(admin).now();
		});
	}

	@Rule
	public final HttpClientRule client = new HttpClientRule(false, 8181).followRedirects(false);

	@Test
	public void index() throws IOException {
		HttpGet get = new HttpGet(this.client.url("/index.html"));
		HttpResponse response = this.client.execute(get);
		String html = EntityUtils.toString(response.getEntity());
		assertEquals("Should be successful: " + html, 200, response.getStatusLine().getStatusCode());
		assertTrue("Incorrect response: " + html, html.contains("<title>OfficeFloor Subscription</title>"));
	}

	@Test
	public void initialise() throws IOException {
		HttpGet get = new HttpGet(this.client.url("/initialise"));
		HttpResponse response = this.client.execute(get);
		if (response.getStatusLine().getStatusCode() == 500) {
			// Not yet consistent, so try again
			response = this.client.execute(get);
		}
		String json = EntityUtils.toString(response.getEntity());
		assertEquals("Should be successful: " + json, 200, response.getStatusLine().getStatusCode());
		InitialisationResponse init = mapper.readValue(json, InitialisationResponse.class);
		assertEquals("Incorrect initialisation", "TEST CLIENT", init.getGoogleClientId());
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class InitialisationResponse {
		private String googleClientId;
	}

}