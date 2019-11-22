package net.officefloor.app.subscription.cors;

import net.officefloor.frame.api.source.ServiceContext;
import net.officefloor.web.build.WebInterceptServiceFactory;

/**
 * CORS {@link WebInterceptServiceFactory}.
 * 
 * @author Daniel Sagenschneider
 */
public class CorsWebIntercepterServiceFactory implements WebInterceptServiceFactory {

	@Override
	public Class<?> createService(ServiceContext context) throws Throwable {
		return Cors.class;
	}

}