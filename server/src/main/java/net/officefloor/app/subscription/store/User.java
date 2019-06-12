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
 * User {@link Entity}.
 * 
 * @author Daniel Sagenschneider
 */
@Entity
@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class User {

	public static final String ROLE_ADMIN = "admin";

	@Id
	private Long id;

	@Index
	@NonNull
	private String email;

	private String name;

	private String photoUrl;

	private String[] roles = new String[0];

	private Date timestamp = ObjectifyEntities.getCreationTimestamp();

}