package net.officefloor.app.subscription.store;

import java.util.Date;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Invoice {@link Entity}.
 * 
 * @author Daniel Sagenschneider
 */
@Entity
@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class Invoice {

	@Id
	private Long id;

	@Index
	@NonNull
	private Ref<User> user;

	@NonNull
	private String productType;

	@NonNull
	private String productReference;

	@NonNull
	private Boolean isRestartSubscription;

	@Index
	private String paymentOrderId;

	private Date timestamp = ObjectifyEntities.getCreationTimestamp();
}