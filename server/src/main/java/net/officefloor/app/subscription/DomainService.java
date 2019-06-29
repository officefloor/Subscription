package net.officefloor.app.subscription;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.Ref;

import lombok.Value;
import net.officefloor.app.subscription.SubscriptionCalculator.Subscription;
import net.officefloor.app.subscription.store.Domain;
import net.officefloor.app.subscription.store.Payment;
import net.officefloor.app.subscription.store.User;
import net.officefloor.plugin.section.clazz.Next;
import net.officefloor.plugin.section.clazz.Parameter;
import net.officefloor.web.ObjectResponse;

/**
 * Logic to retrieve the domain entries.
 * 
 * @author Daniel Sagenschneider
 */
public class DomainService {

	@Value
	public static class PaidDomain {
		private String domainName;
		private String expiresDate;
	}

	public static void getDomains(User user, Objectify objectify, ObjectResponse<PaidDomain[]> response)
			throws IOException {

		// Obtain the payments
		Set<String> domainNames = new HashSet<>();
		for (Payment payment : objectify.load().type(Payment.class).filter("user", Ref.create(user)).iterable()) {
			domainNames.add(payment.getProductReference());
		}

		// Obtain the domains
		List<Domain> domains = new ArrayList<>(domainNames.size());
		for (String domainName : domainNames) {
			Domain domain = objectify.load().type(Domain.class).filter("domain", domainName).first().now();
			if (domain != null) {
				domains.add(domain);
			}
		}

		// Return the domains
		PaidDomain[] domainResponses = domains.stream().sorted((a, b) -> a.getExpires().compareTo(b.getExpires()))
				.map((domain) -> new PaidDomain(domain.getDomain(), ResponseUtil.toText(domain.getExpires())))
				.toArray(PaidDomain[]::new);
		response.send(domainResponses);
	}

	@Next("DomainUpdated")
	public static Subscription[] updateDomain(@Parameter Subscription[] subscriptions, Objectify objectify) {

		// Determine if have subscriptions
		if ((subscriptions == null) || (subscriptions.length == 0)) {
			return subscriptions; // no subscription, so don't update domain
		}

		// Obtain the expires date (first subscription)
		Subscription expireSubscription = subscriptions[0];
		String domainName = expireSubscription.getProductReference();
		Date expiresDate = Date.from(expireSubscription.getExtendsToDate().toInstant());

		// Obtain the domain (ensuring only one entry)
		Domain domain = null;
		Iterator<Domain> iterator = objectify.load().type(Domain.class).filter("domain", domainName).iterator();
		if (iterator.hasNext()) {
			domain = iterator.next();
		}
		iterator.forEachRemaining((extra) -> objectify.delete().entity(extra).now());

		// Update domain with expire time (or create with expire time)
		if (domain != null) {
			domain.setExpires(expiresDate);
		} else {
			domain = new Domain(domainName, expiresDate);
		}
		objectify.save().entities(domain).now();

		// Return the subscriptions
		return subscriptions;
	}

}