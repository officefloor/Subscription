package net.officefloor.app.subscription.store;

import java.util.Date;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Domain {@link Entity}.
 * 
 * @author Daniel Sagenschneider
 */
@Entity
@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class Domain {

	public static final String PRODUCT_TYPE = "domain";

	@Id
	private Long id;

	@Index
	@NonNull
	private String domain;

	@Index
	@NonNull
	private Date expires;

	private Date timestamp = ObjectifyEntities.getCreationTimestamp();

}