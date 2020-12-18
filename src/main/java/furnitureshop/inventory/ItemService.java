package furnitureshop.inventory;

import furnitureshop.order.ItemOrder;
import furnitureshop.order.ItemOrderEntry;
import furnitureshop.order.OrderService;
import furnitureshop.order.OrderStatus;
import furnitureshop.supplier.Supplier;
import furnitureshop.supplier.SupplierService;
import org.salespointframework.catalog.ProductIdentifier;
import org.salespointframework.core.Currencies;
import org.salespointframework.time.BusinessTime;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.util.Pair;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.money.MonetaryAmount;
import java.lang.reflect.Array;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * This class manages all methods to add, remove or find an {@link Item} by its attributes.
 */
@Service
@Transactional
public class ItemService {

	private final ItemCatalog itemCatalog;
	private final SupplierService supplierService;
	private final OrderService orderService;
	private final BusinessTime businessTime;

	/**
	 * Creates a new instance of an {@link ItemService}
	 *
	 * @param itemCatalog     {@link ItemCatalog} which contains all items
	 * @param supplierService {@link SupplierService} reference to the SupplierService
	 * @param orderService    {@link OrderService} reference to the OrderService
	 * @param businessTime	  {@link BusinessTime} reference to BusinessTime
	 *
	 * @throws IllegalArgumentException If {@code itemCatalog} is {@code null}
	 */
	public ItemService(ItemCatalog itemCatalog, @Lazy SupplierService supplierService, @Lazy OrderService orderService, BusinessTime businessTime) {
		Assert.notNull(itemCatalog, "ItemCatalog must not be null!");
		Assert.notNull(supplierService, "SupplierService must not be null!");
		Assert.notNull(orderService, "OrderService must not be null!");
		Assert.notNull(businessTime, "BusinessTime must not be null!");

		this.itemCatalog = itemCatalog;
		this.supplierService = supplierService;
		this.orderService = orderService;
		this.businessTime = businessTime;
	}

	/**
	 * Adds or updates an {@link Item} in the catalog
	 *
	 * @param item A {@link Item} to add to the {@code ItemCatalog}
	 *
	 * @throws IllegalArgumentException If {@code item} is {@code null}
	 */
	public void addOrUpdateItem(Item item) {
		Assert.notNull(item, "Item must not be null!");

		itemCatalog.save(item);
	}

	/**
	 * Removes an {@link Item} from the catalog
	 *
	 * @param item A {@link Item} to remove from the {@code itemCatalog}
	 *
	 * @return {@code true} if the {@link Item} was removed
	 *
	 * @throws IllegalArgumentException If {@code item} is {@code null}
	 */
	public boolean removeItem(Item item) {
		Assert.notNull(item, "Item must not be null!");

		if (findById(item.getId()).isEmpty()) {
			return false;
		}

		orderService.removeItemFromOrders(item);

		for (Item it : findAllSetsByItem(item)) {
			removeItem(it);
		}

		itemCatalog.delete(item);

		return true;
	}

	/**
	 * Finds all items in the catalog
	 *
	 * @return Returns all items in the {@code itemCatalog}
	 */
	public Streamable<Item> findAll() {
		return itemCatalog.findAll();
	}

	/**
	 * Finds all visibble items in the catalog
	 *
	 * @return Returns all visible items in the {@code itemCatalog}
	 */
	public Streamable<Item> findAllVisible() {
		return itemCatalog.findAll().filter(Item::isVisible);
	}

	/**
	 * Finds all items from a specific supplier
	 *
	 * @param supplier A {@link Supplier}
	 *
	 * @return Returns a stream of {@link Item}s with the same {@link Supplier}
	 *
	 * @throws IllegalArgumentException If {@code supplier} is {@code null}
	 */
	public Streamable<Item> findBySupplier(Supplier supplier) {
		Assert.notNull(supplier, "Supplier must not be null!");

		return itemCatalog.findAll().filter(it -> it.getSupplier() == supplier);
	}

	/**
	 * Finds a specific item by its id
	 *
	 * @param id A {@link ProductIdentifier}
	 *
	 * @return Returns an {@link Item}
	 *
	 * @throws IllegalArgumentException If {@code id} is {@code null}
	 */
	public Optional<Item> findById(ProductIdentifier id) {
		Assert.notNull(id, "Id must not be null!");

		return itemCatalog.findById(id);
	}

	/**
	 * Finds all items of a specific category
	 *
	 * @param category A {@link Category}
	 *
	 * @return Returns a stream of {@link Item}s all with the same category
	 *
	 * @throws IllegalArgumentException If {@code category} is {@code null}
	 */
	public Streamable<Item> findAllByCategory(Category category) {
		Assert.notNull(category, "Category must not be null!");

		return findAll().filter(it -> it.getCategory() == category);
	}

	/**
	 * Finds all visible items of a specific category
	 *
	 * @param category A {@link Category}
	 *
	 * @return Returns a stream of visible {@link Item}s all with the same category
	 *
	 * @throws IllegalArgumentException If {@code category} is {@code null}
	 */
	public Streamable<Item> findAllVisibleByCategory(Category category) {
		Assert.notNull(category, "Category must not be null!");

		return findAllVisible().filter(it -> it.getCategory() == category);
	}

	/**
	 * Finds all items of a specific category
	 *
	 * @param groupId GroupId
	 *
	 * @return Returns a stream of {@link Item}s all with the same {@code groupId}
	 */
	public Streamable<Item> findAllByGroupId(int groupId) {
		return findAll().filter(it -> it.getGroupId() == groupId);
	}

	/**
	 * Finds all visible items of a specific category
	 *
	 * @param groupId GroupId
	 *
	 * @return Returns a stream of visible {@link Item}s all with the same {@code groupId}
	 */
	public Streamable<Item> findAllVisibleByGroupId(int groupId) {
		return findAllVisible().filter(it -> it.getGroupId() == groupId);
	}

	/**
	 * Finds all sets of which a given item is a part of
	 *
	 * @param item An {@link Item}
	 *
	 * @return A list of {@link Set}s
	 */
	public List<Set> findAllSetsByItem(Item item) {
		final List<Set> sets = new ArrayList<>();

		for (Item it : findAll()) {
			if (it instanceof Set) {
				Set set = (Set) it;
				if (set.getItems().contains(item)) {
					sets.add(set);
				}
			}
		}

		return sets;
	}

	/**
	 * Find a specific supplier with the given id
	 *
	 * @param id A {@link Supplier} id
	 *
	 * @return Returns a {@link Supplier}
	 */
	public Optional<Supplier> findSupplierById(long id) {
		return supplierService.findById(id);
	}

	public LocalDate getFirstOrderDate() {
		LocalDateTime cur = businessTime.getTime();

		for (ItemOrder itemOrder : orderService.findAllItemOrders()) {
			if (itemOrder.getCreated().isBefore(cur)) {
				cur = itemOrder.getCreated();
			}
		}
		return cur.toLocalDate();
	}

	public List<StatisticEntry> analyse(LocalDate initDate, LocalDate compareDate) {
		List<StatisticEntry> statisticEntries = new ArrayList<>();
		boolean initFlag, compareFlag;

		for (ItemOrder order: orderService.findAllItemOrders()) {
			initFlag = order.getCreated().getMonth() == initDate.getMonth() && order.getCreated().getYear() == initDate.getYear();
			compareFlag = order.getCreated().getMonth() == compareDate.getMonth() && order.getCreated().getYear() == compareDate.getYear();

			if (!initFlag && !compareFlag) {
				continue;
			}

			for (Map.Entry<Item, MonetaryAmount> entry : order.getProfits().entrySet()) {
				StatisticEntry statEntry = null;

				for (StatisticEntry statisticEntry : statisticEntries) {
					if (statisticEntry.getSupplier().equals(entry.getKey().getSupplier())) {
						statEntry = statisticEntry;
					}
				}

				if (statEntry == null) {
					statEntry = new StatisticEntry(entry.getKey().getSupplier());
					statisticEntries.add(statEntry);
				}

				StatisticItemEntry itemEntry;

				if (initFlag && !compareFlag) {
					itemEntry = new StatisticItemEntry(entry.getKey(), entry.getValue(), Currencies.ZERO_EURO);
				} else if (!initFlag) {
					itemEntry = new StatisticItemEntry(entry.getKey(), Currencies.ZERO_EURO, entry.getValue());
				} else {
					itemEntry = new StatisticItemEntry(entry.getKey(), entry.getValue(), entry.getValue());
				}

				statEntry.addEntry(itemEntry);
			}
		}

		return statisticEntries;
	}
}
