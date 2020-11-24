package net.officefloor.app.subscription;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.objectify.Ref;

import net.officefloor.app.subscription.AuthenticateService.AuthenticateRequest;
import net.officefloor.app.subscription.AuthenticateService.AuthenticateResponse;
import net.officefloor.app.subscription.AuthenticateService.RefreshRequest;
import net.officefloor.app.subscription.AuthenticateService.RefreshResponse;
import net.officefloor.app.subscription.store.Administration;
import net.officefloor.app.subscription.store.Administration.Administrator;
import net.officefloor.app.subscription.store.GoogleSignin;
import net.officefloor.app.subscription.store.User;
import net.officefloor.identity.google.mock.GoogleIdTokenRule;
import net.officefloor.nosql.objectify.mock.ObjectifyRule;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpMethod;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.server.http.mock.MockHttpResponse;
import net.officefloor.woof.mock.MockWoofResponse;
import net.officefloor.woof.mock.MockWoofServer;
import net.officefloor.woof.mock.MockWoofServerRule;

/**
 * Tests integration of authentication.
 * 
 * @author Daniel Sagenschneider
 */
public class AuthenticateServiceTest {

	private GoogleIdTokenRule verifier = new GoogleIdTokenRule();

	private ObjectifyRule objectify = new ObjectifyRule();

	private MockWoofServerRule server = new MockWoofServerRule();

	@Rule
	public RuleChain chain = RuleChain.outerRule(this.verifier).around(this.objectify).around(this.server);

	private final ObjectMapper mapper = new ObjectMapper();

	private AuthenticateResponse authenticateResponse = null;

	@Test
	public void notInitialised() throws Exception {
		String token = this.getMockIdToken("Not Initialised");
		MockWoofResponse response = this.server.send(MockWoofServer
				.mockJsonRequest(HttpMethod.POST, "/authenticate", new AuthenticateRequest(token)).secure(true));
		response.assertJsonError(new HttpException(HttpStatus.SERVICE_UNAVAILABLE, "Server not initialised"));
	}

	@Test
	public void authenticate() throws Exception {

		// Initialise the server
		this.objectify.store(new Administration());

		// Undertake authentication
		String token = this.getMockIdToken("Guest User");
		MockHttpResponse response = this.server.send(MockWoofServer
				.mockJsonRequest(HttpMethod.POST, "/authenticate", new AuthenticateRequest(token)).secure(true));
		String entity = response.getEntity(null);
		assertEquals("Should be successful: " + entity, 200, response.getStatus().getStatusCode());

		// Ensure login created in store
		GoogleSignin login = this.objectify
				.get(GoogleSignin.class, 1, (load) -> load.filter("email", "guest.user@officefloor.org")).get(0);
		assertNotNull("Should have the login", login);
		assertNotNull("Should have google id", login.getGoogleId());
		assertEquals("Incorrect name", "Guest User", login.getName());
		assertEquals("Incorrect photoUrl", "https://google.com/Guest_User_photo.png", login.getPhotoUrl());

		// Ensure user created in store
		User user = this.objectify.get(User.class, 1, (load) -> load.filter("email", "guest.user@officefloor.org"))
				.get(0);
		assertEquals("Incorrect user", user.getId(), login.getUser().get().getId());
		assertEquals("Incorrect name", "Guest User", user.getName());
		assertEquals("Incorrect photoUrl", "https://google.com/Guest_User_photo.png", user.getPhotoUrl());
		assertArrayEquals("Incorrect roles (not admin)", new String[0], user.getRoles());

		// Ensure refresh and access token point to user
		this.authenticateResponse = mapper.readValue(entity, AuthenticateResponse.class);
		assertNotNull("Should have refresh token", this.authenticateResponse.getRefreshToken());
		assertNotNull("Should have access token", this.authenticateResponse.getAccessToken());

		// Ensure valid expire times
		ZonedDateTime now = ZonedDateTime.now(ResponseUtil.ZONE);
		ZonedDateTime refreshExpireTime = TestHelper.toZonedDateTime(this.authenticateResponse.getRefreshExpireTime());
		assertApproximateTime("Incorrect refresh expire time ", now.plus(8, ChronoUnit.HOURS), refreshExpireTime);
		ZonedDateTime accessExpireTime = TestHelper.toZonedDateTime(this.authenticateResponse.getAccessExpireTime());
		assertApproximateTime("Incorrect access expire time ", now.plus(20, ChronoUnit.MINUTES), accessExpireTime);
	}

	@Test
	public void updateUserDetails() throws Exception {

		// Initialise the server
		this.objectify.store(new Administration());

		// Create the existing user
		String name = "Existing User";
		String updatedName = "Updated User";
		String existingGoogleId = this.getGoogleUserId(updatedName);
		User originalUser = TestHelper.newUser(name, "some_role");
		this.objectify.store(originalUser);
		GoogleSignin originalSignin = new GoogleSignin(existingGoogleId, originalUser.getEmail());
		originalSignin.setUser(Ref.create(originalUser));
		this.objectify.store(originalSignin);

		// Undertake authentication (to update user)
		String token = this.getMockIdToken(updatedName);
		MockHttpResponse response = this.server.send(MockWoofServer
				.mockJsonRequest(HttpMethod.POST, "/authenticate", new AuthenticateRequest(token)).secure(true));
		assertEquals("Should be successful", 200, response.getStatus().getStatusCode());

		// Validate sign-in (ensuring consistent state)
		GoogleSignin updatedSignin = this.objectify.consistent(
				() -> this.objectify.get(GoogleSignin.class, originalSignin.getId()),
				(checkSignin) -> "updated.user@officefloor.org".equals(checkSignin.getEmail()));
		assertEquals("Incorrect name", "Updated User", updatedSignin.getName());
		assertEquals("Incorrect photoUrl", "https://google.com/Updated_User_photo.png", updatedSignin.getPhotoUrl());

		// Ensure user updated in store
		User updatedUser = this.objectify.consistent(() -> this.objectify.get(User.class, originalUser.getId()),
				(checkUser) -> "updated.user@officefloor.org".equals(checkUser.getEmail()));
		assertEquals("Incorrect user", updatedUser.getId(), updatedSignin.getUser().get().getId());
		assertEquals("Incorrect name", "Updated User", updatedUser.getName());
		assertEquals("Incorrect photoUrl", "https://google.com/Updated_User_photo.png", updatedUser.getPhotoUrl());
		assertArrayEquals("Incorrect roles (not admin)", new String[0], updatedUser.getRoles());
	}

	@Test
	public void authenticateAdministrator() throws Exception {

		// Determine admin identifier
		String name = "Admin User";
		String googleAdminId = this.getGoogleUserId(name);

		// Initialise the server
		Administration administration = new Administration();
		administration.setAdministrators(new Administrator[] { new Administrator(googleAdminId, "Notes") });
		this.objectify.store(administration);

		// Undertake authentication
		String token = this.getMockIdToken(name);
		MockHttpResponse response = this.server.send(MockWoofServer
				.mockJsonRequest(HttpMethod.POST, "/authenticate", new AuthenticateRequest(token)).secure(true));
		assertEquals("Should be successful", 200, response.getStatus().getStatusCode());

		// Ensure user created in store
		User user = this.objectify.get(User.class, 1, (load) -> load.filter("email", "admin.user@officefloor.org"))
				.get(0);
		assertArrayEquals("Should be admin", new String[] { User.ROLE_ADMIN }, user.getRoles());
	}

	@Test
	public void refreshAccessToken() throws Exception {

		// Authenticate (to get refresh token)
		this.authenticate();
		String refreshToken = this.authenticateResponse.getRefreshToken();

		// Ensure token in database
		this.objectify.get(GoogleSignin.class, 1, (load) -> load.filter("email", "guest.user@officefloor.org"));

		// Due to timing access token may be different
		String previousAccessToken = this.authenticateResponse.getAccessToken();
		String previousExpireTime = this.authenticateResponse.getAccessExpireTime();
		for (int i = 0; i < 3; i++) {

			// Refresh the token
			MockHttpResponse response = this.server.send(MockWoofServer
					.mockJsonRequest(HttpMethod.POST, "/refreshAccessToken", new RefreshRequest(refreshToken))
					.secure(true));
			assertEquals("Should be successful", 200, response.getStatus().getStatusCode());
			RefreshResponse refreshResponse = mapper.readValue(response.getEntity(null), RefreshResponse.class);

			// Determine if match
			if (previousAccessToken.equals(refreshResponse.getAccessToken())) {

				// As using same keys, should be same access token (times to second)
				assertEquals("Should be same token", previousAccessToken, refreshResponse.getAccessToken());
				assertEquals("Incorrect expire time", previousExpireTime, refreshResponse.getAccessExpireTime());

				// Successfully refreshed in same second to get same access token
				return;
			}

			// Invoked across second boundary, so try again
			previousAccessToken = refreshResponse.getAccessToken();
			previousExpireTime = refreshResponse.getAccessExpireTime();
		}
		fail("Should have matched access token");
	}

	private String getGoogleUserId(String name) {
		return String.valueOf(name.hashCode());
	}

	private String getMockIdToken(String name) throws Exception {
		User user = TestHelper.newUser(name);
		return this.verifier.getMockIdToken(this.getGoogleUserId(name), user.getEmail(), "email_verified", "true",
				"name", name, "picture", user.getPhotoUrl());
	}

	private static void assertApproximateTime(String message, ZonedDateTime expected, ZonedDateTime actual) {
		ZonedDateTime start = expected.minus(1, ChronoUnit.MINUTES);
		ZonedDateTime end = expected.plus(1, ChronoUnit.MINUTES);
		assertTrue(message + ResponseUtil.toText(actual) + " is before " + ResponseUtil.toText(start),
				actual.isAfter(start));
		assertTrue(message + ResponseUtil.toText(actual) + " is after " + ResponseUtil.toText(end),
				actual.isBefore(end));
	}
}