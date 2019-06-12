package net.officefloor.app.subscription.store;

import java.time.ZoneId;
import java.util.Date;

import net.officefloor.nosql.objectify.ObjectifyEntityLocator;

/**
 * {@link ObjectifyEntityLocator} for application.
 * 
 * @author Daniel Sagenschneider
 */
public class ObjectifyEntities implements ObjectifyEntityLocator {

	public static final ZoneId ZONE = ZoneId.of("GMT");

	public static Date getCreationTimestamp() {
		return new Date();
	}

	@Override
	public Class<?>[] locateEntities() throws Exception {
		return new Class[] { AccessKey.class, RefreshKey.class, GoogleSignin.class, User.class, Administration.class,
				Domain.class, Invoice.class, Payment.class, Refund.class };
	}

}