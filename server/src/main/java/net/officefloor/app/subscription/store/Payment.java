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
 * Payment {@link Entity}.
 * 
 * @author Daniel Sagenschneider
 */
@Entity
@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class Payment {

	@Id
	private Long id;

	@Index
	@NonNull
	private Ref<User> user;

	@NonNull
	private Ref<Invoice> invoice;

	@NonNull
	private String productType;

	@Index
	@NonNull
	private String productReference;

	@NonNull
	private Boolean isRestartSubscription;

	/**
	 * Amount in cents.
	 */
	@NonNull
	private Integer amount;

	@NonNull
	private String receipt;

	private Ref<Refund> refund = null;

	private Date timestamp = ObjectifyEntities.getCreationTimestamp();

}