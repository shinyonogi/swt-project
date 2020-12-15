package furnitureshop.order;

import org.salespointframework.order.Order;
import org.salespointframework.useraccount.UserAccount;
import org.springframework.util.Assert;

import javax.money.MonetaryAmount;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import java.time.LocalDateTime;

@Entity
public abstract class ShopOrder extends Order {

	@OneToOne(cascade = CascadeType.ALL)
	private ContactInformation contactInformation;

	private LocalDateTime created;

	/**
	 * Empty constructor for {@code Spring}. Not in use.
	 *
	 * @deprecated
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	protected ShopOrder() { }

	/**
	 * Creates new instance of {@link ShopOrder}
	 *
	 * @param userAccount        The dummy {@link UserAccount}
	 * @param contactInformation The {@link ContactInformation} of the customer
	 *
	 * @throws IllegalArgumentException if any argument is {@code null}
	 */
	ShopOrder(UserAccount userAccount, ContactInformation contactInformation) {
		super(userAccount);

		Assert.notNull(contactInformation, "ContactInformation must not be null!");

		this.contactInformation = contactInformation;
	}

	public ContactInformation getContactInformation() {
		return contactInformation;
	}

	public abstract MonetaryAmount getMissingPayment();

	public abstract MonetaryAmount getRefund();

	public abstract MonetaryAmount getCancelFee();

	public LocalDateTime getCreated() {
		return created;
	}

	public LocalDateTime getUpdated() {
		return getDateCreated();
	}

	public void setCreated(LocalDateTime created) {
		this.created = created;
	}

}
