package net.officefloor.app.subscription;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.objectify.Ref;

import net.officefloor.app.subscription.AuthenticateLogic.AuthenticateRequest;
import net.officefloor.app.subscription.AuthenticateLogic.AuthenticateResponse;
import net.officefloor.app.subscription.AuthenticateLogic.RefreshRequest;
import net.officefloor.app.subscription.AuthenticateLogic.RefreshResponse;
import net.officefloor.app.subscription.store.Administration;
import net.officefloor.app.subscription.store.GoogleSignin;
import net.officefloor.app.subscription.store.User;
import net.officefloor.identity.google.mock.GoogleIdTokenRule;
import net.officefloor.nosql.objectify.mock.ObjectifyRule;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpMethod;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.server.http.mock.MockHttpResponse;
import net.officefloor.server.http.mock.MockHttpServer;
import net.officefloor.web.json.JacksonHttpObjectResponderFactory;
import net.officefloor.woof.mock.MockWoofServerRule;

/**
 * Tests integration of authentication.
 * 
 * @author Daniel Sagenschneider
 */
public class AuthenticateLogicTest {

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
	 * @param objectify {@link ObjectifyRule}.
	 * @param name      Name of {@link User}.
	 * @param roles     Roles.
	 * @return {@link User}.
	 */
	public static User setupUser(ObjectifyRule objectify, String name, String... roles) {
		User user = newUser(name, roles);
		objectify.store(user);
		return user;
	}

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
		MockHttpResponse response = this.server.send(MockHttpServer.mockRequest("/authenticate").secure(true)
				.method(HttpMethod.POST).header("Content-Type", "application/json")
				.entity(this.mapper.writeValueAsString(new AuthenticateRequest(token))));
		response.assertResponse(503, JacksonHttpObjectResponderFactory
				.getEntity(new HttpException(HttpStatus.SERVICE_UNAVAILABLE, "Server not initialised"), mapper));
	}

	@Test
	public void authenticate() throws Exception {

		// Initialise the server
		this.objectify.store(new Administration());

		// Undertake authentication
		String token = this.getMockIdToken("Guest User");
		MockHttpResponse response = this.server.send(MockHttpServer.mockRequest("/authenticate").secure(true)
				.method(HttpMethod.POST).header("Content-Type", "application/json")
				.entity(this.mapper.writeValueAsString(new AuthenticateRequest(token))));
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
	}

	@Test
	public void updateUserDetails() throws Exception {

		// Initialise the server
		this.objectify.store(new Administration());

		// Create the existing user
		String name = "Existing User";
		String updatedName = "Updated User";
		String existingGoogleId = this.getGoogleUserId(updatedName);
		User originalUser = newUser(name, "some_role");
		this.objectify.store(originalUser);
		GoogleSignin originalSignin = new GoogleSignin(existingGoogleId, originalUser.getEmail());
		originalSignin.setUser(Ref.create(originalUser));
		this.objectify.store(originalSignin);

		// Undertake authentication (to update user)
		String token = this.getMockIdToken(updatedName);
		MockHttpResponse response = this.server.send(MockHttpServer.mockRequest("/authenticate").secure(true)
				.method(HttpMethod.POST).header("Content-Type", "application/json")
				.entity(this.mapper.writeValueAsString(new AuthenticateRequest(token))));
		assertEquals("Should be successful", 200, response.getStatus().getStatusCode());

		// Validate signin (ensuring consistent state)
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
		administration.setGoogleAdministratorIds(new String[] { googleAdminId });
		this.objectify.store(administration);

		// Undertake authentication
		String token = this.getMockIdToken(name);
		MockHttpResponse response = this.server.send(MockHttpServer.mockRequest("/authenticate").secure(true)
				.method(HttpMethod.POST).header("Content-Type", "application/json")
				.entity(this.mapper.writeValueAsString(new AuthenticateRequest(token))));
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

		// Refresh the token
		MockHttpResponse response = this.server.send(MockHttpServer.mockRequest("/refreshAccessToken").secure(true)
				.method(HttpMethod.POST).header("Content-Type", "application/json")
				.entity(this.mapper.writeValueAsString(new RefreshRequest(refreshToken))));
		assertEquals("Should be successful", 200, response.getStatus().getStatusCode());
		RefreshResponse refreshResponse = mapper.readValue(response.getEntity(null), RefreshResponse.class);

		// As using same keys, should be same access token (times to second)
		assertEquals("Should be same token", this.authenticateResponse.getAccessToken(),
				refreshResponse.getAccessToken());
	}

	private String getGoogleUserId(String name) {
		return String.valueOf(name.hashCode());
	}

	private String getMockIdToken(String name) throws Exception {
		User user = newUser(name);
		return this.verifier.getMockIdToken(this.getGoogleUserId(name), user.getEmail(), "email_verified", "true",
				"name", name, "picture", user.getPhotoUrl());
	}

}