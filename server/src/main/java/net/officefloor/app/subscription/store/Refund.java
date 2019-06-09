package net.officefloor.app.subscription.store;

import java.util.Date;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Refund {@link Entity}.
 * 
 * @author Daniel Sagenschneider
 */
@Entity
@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class Refund {

	@Id
	private Long id;

	@NonNull
	private String reason;

	private Date timestamp = new Date(System.currentTimeMillis());
}