package net.officefloor.app.subscription.jwt;

import java.security.Key;
import java.util.ArrayList;
import java.util.List;

import com.googlecode.objectify.Objectify;

import lombok.Value;
import net.officefloor.app.subscription.store.AccessKey;
import net.officefloor.app.subscription.store.RefreshKey;
import net.officefloor.plugin.managedobject.clazz.Dependency;
import net.officefloor.web.jwt.authority.repository.JwtAccessKey;
import net.officefloor.web.jwt.authority.repository.JwtAuthorityRepository;
import net.officefloor.web.jwt.authority.repository.JwtRefreshKey;

/**
 * Implementation of the {@link JwtAuthorityRepository}.
 * 
 * @author Daniel Sagenschneider
 */
public class JwtAuthorityRepositoryImpl implements JwtAuthorityRepository {

	@Dependency
	private Objectify objectify;

	/*
	 * ===================== JwtAuthorityRepository =======================
	 */

	@Override
	public List<JwtAccessKey> retrieveJwtAccessKeys(RetrieveKeysContext context) throws Exception {

		// Obtain the access keys (deleting old keys)
		List<JwtAccessKey> jwtAccessKeys = new ArrayList<>();
		NEXT_KEY: for (AccessKey accessKey : this.objectify.load().type(AccessKey.class).iterable()) {

			// Determine if expired
			if (accessKey.getExpireTime() < context.getActiveAfter()) {
				this.objectify.delete().entity(accessKey).now();
				continue NEXT_KEY;
			}

			// Add the active access key
			Key publicKey = context.deserialise(accessKey.getPublicKey());
			Key privateKey = context.deserialise(accessKey.getPrivateKey());
			jwtAccessKeys.add(
					new JwtAccessKeyImpl(accessKey.getStartTime(), accessKey.getExpireTime(), publicKey, privateKey));
		}

		// Return the JWT access keys
		return jwtAccessKeys;
	}

	@Override
	public void saveJwtAccessKeys(SaveKeysContext context, JwtAccessKey... keys) throws Exception {

		// Save the access keys
		List<AccessKey> accessKeys = new ArrayList<>(keys.length);
		for (JwtAccessKey key : keys) {
			String publicKey = context.serialise(key.getPublicKey());
			String privateKey = context.serialise(key.getPrivateKey());
			accessKeys.add(new AccessKey(null, key.getStartTime(), key.getExpireTime(), publicKey, privateKey));
		}

		// Save the keys
		this.objectify.save().entities(accessKeys).now();
	}

	@Override
	public List<JwtRefreshKey> retrieveJwtRefreshKeys(RetrieveKeysContext context) throws Exception {

		// Obtain the refresh keys
		List<JwtRefreshKey> jwtRefreshKeys = new ArrayList<>();
		NEXT_KEY: for (RefreshKey refreshKey : this.objectify.load().type(RefreshKey.class).iterable()) {

			// Determine if expired
			if (refreshKey.getExpireTime() < context.getActiveAfter()) {
				this.objectify.delete().entity(refreshKey).now();
				continue NEXT_KEY;
			}

			// Add the active refresh key
			Key key = context.deserialise(refreshKey.getKey());
			jwtRefreshKeys.add(new JwtRefreshKeyImpl(refreshKey.getStartTime(), refreshKey.getExpireTime(),
					refreshKey.getInitVector(), refreshKey.getStartSalt(), refreshKey.getLace(),
					refreshKey.getEndSalt(), key));
		}

		// Return the JWT refresh keys
		return jwtRefreshKeys;
	}

	@Override
	public void saveJwtRefreshKeys(SaveKeysContext context, JwtRefreshKey... keys) {

		// Save the refresh keys
		List<RefreshKey> refreshKeys = new ArrayList<>(keys.length);
		for (JwtRefreshKey key : keys) {
			String keyText = context.serialise(key.getKey());
			refreshKeys.add(new RefreshKey(null, key.getStartTime(), key.getExpireTime(), key.getInitVector(),
					key.getStartSalt(), key.getLace(), key.getEndSalt(), keyText));
		}

		// Save the keys
		this.objectify.save().entities(refreshKeys).now();
	}

	/**
	 * {@link JwtAccessKey} implementation.
	 */
	@Value
	private static class JwtAccessKeyImpl implements JwtAccessKey {
		private final long startTime;
		private final long expireTime;
		private final Key publicKey;
		private final Key privateKey;
	}

	/**
	 * {@link JwtRefreshKey} implementation.
	 */
	@Value
	private static class JwtRefreshKeyImpl implements JwtRefreshKey {
		private final long startTime;
		private final long expireTime;
		private final String initVector;
		private final String startSalt;
		private final String lace;
		private final String endSalt;
		private final Key key;
	}

}