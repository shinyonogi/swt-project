package furnitureshop.order;

import furnitureshop.inventory.Item;
import org.salespointframework.catalog.Product;
import org.salespointframework.order.OrderLine;
import org.salespointframework.quantity.Quantity;
import org.salespointframework.useraccount.UserAccount;
import org.springframework.data.util.Streamable;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
public abstract class ItemOrder extends ShopOrder {

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	private List<ItemOrderEntry> orderWithStatus;

	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	protected ItemOrder() {}

	/**
	 * Creates a new instance of {@link ItemOrder}
	 *
	 * @param userAccount        The dummy {@link UserAccount}
	 * @param contactInformation {@link ContactInformation} of the user
	 *
	 * @throws IllegalArgumentException if any argument is {@code null}
	 */
	public ItemOrder(UserAccount userAccount, ContactInformation contactInformation) {
		super(userAccount, contactInformation);

		this.orderWithStatus = new ArrayList<>();
	}

	@Override
	@SuppressWarnings("NullableProblems")
	public OrderLine addOrderLine(Product product, Quantity quantity) {
		final OrderLine orderLine = super.addOrderLine(product, quantity);

		if (product instanceof Item) {
			final int amount = quantity.getAmount().intValue();
			for (int i = 0; i < amount; i++) {
				orderWithStatus.add(new ItemOrderEntry((Item) product, OrderStatus.OPEN));
			}
		}

		return orderLine;
	}

	public boolean removeEntry(long entryId) {
		for (ItemOrderEntry entry : orderWithStatus) {
			if (entry.getId() == entryId) {
				orderWithStatus.remove(entry);
				return true;
			}
		}
		return false;
	}

	public boolean changeStatus(long entryId, OrderStatus newStatus) {
		for (ItemOrderEntry orderEntry : orderWithStatus) {
			if (orderEntry.getId() == entryId) {
				orderEntry.setStatus(newStatus);
				return true;
			}
		}

		return false;
	}

	public void changeAllStatus(OrderStatus newStatus) {
		for (ItemOrderEntry orderEntry : orderWithStatus) {
			orderEntry.setStatus(newStatus);
		}
	}

	public List<ItemOrderEntry> getOrderEntries() {
		return Collections.unmodifiableList(orderWithStatus);
	}

	public List<ItemOrderEntry> getOrderEntriesByItem(Item item) {
		return Streamable.of(orderWithStatus).filter(e -> e.getItem().equals(item)).toList();
	}

}
