package net.officefloor.app.subscription;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.Ref;

import lombok.Value;
import net.officefloor.app.subscription.store.Domain;
import net.officefloor.app.subscription.store.Payment;
import net.officefloor.app.subscription.store.User;
import net.officefloor.web.ObjectResponse;

/**
 * Logic to retrieve the domain entries.
 * 
 * @author Daniel Sagenschneider
 */
public class DomainLogic {

	@Value
	public static class PaidDomain {
		private String domainName;
		private String expireDate;
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

}