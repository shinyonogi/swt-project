package furnitureshop.order;

import furnitureshop.FurnitureShop;
import furnitureshop.inventory.Category;
import furnitureshop.inventory.Item;
import furnitureshop.inventory.ItemCatalog;
import furnitureshop.inventory.Piece;
import furnitureshop.lkw.LKW;
import furnitureshop.lkw.LKWService;
import furnitureshop.lkw.LKWType;
import furnitureshop.supplier.Supplier;
import furnitureshop.supplier.SupplierRepository;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.salespointframework.catalog.Product;
import org.salespointframework.core.Currencies;
import org.salespointframework.order.*;
import org.salespointframework.time.BusinessTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ContextConfiguration(classes = FurnitureShop.class)
public class OrderServiceTests {

	@Autowired
	OrderManagement<ShopOrder> orderManagement;

	@Autowired
	ItemCatalog itemCatalog;

	@Autowired
	SupplierRepository supplierRepository;

	@Autowired
	OrderService orderService;

	@Autowired
	BusinessTime businessTime;

	@Autowired
	LKWService lkwService;

	Cart exampleCart;

	@BeforeEach
	void setUp() {
		for (ShopOrder order : orderManagement.findBy(orderService.getDummyUser())) {
			orderManagement.delete(order);
		}

		itemCatalog.deleteAll();
		supplierRepository.deleteAll();

		final Supplier supplier = new Supplier("test", 0.2);
		supplierRepository.save(supplier);

		Piece stuhl1 = new Piece(1, "Stuhl 1", Money.of(59.99, Currencies.EURO), new byte[0], "schwarz",
				"", supplier, 5, Category.CHAIR);
		Piece sofa1_green = new Piece(2, "Sofa 1", Money.of(259.99, Currencies.EURO), new byte[0], "grün",
				"", supplier, 50, Category.COUCH);

		itemCatalog.save(stuhl1);
		itemCatalog.save(sofa1_green);

		this.exampleCart = new Cart();

		exampleCart.addOrUpdateItem(stuhl1, 1);
		exampleCart.addOrUpdateItem(sofa1_green, 2);

		// Reset Time
		final LocalDateTime time = LocalDateTime.of(2020, 12, 21, 0, 0);

		businessTime.reset();
		final Duration delta = Duration.between(businessTime.getTime(), time);
		businessTime.forward(delta);
	}

	/**
	 * testFindById method
	 * Tests if u find the matching {@link Order} for a specific Id
	 */
	@Test
	void testFindById() {
		final ContactInformation info = new ContactInformation("testName", "testAdresse", "testEmail");
		final Pickup order = orderService.orderPickupItem(exampleCart, info);

		final Optional<ShopOrder> shopOrder = orderService.findById(order.getId().getIdentifier());
		assertTrue(shopOrder.isPresent(), "findById() should find the correct Order!");
		assertEquals(order, shopOrder.get(), "findById() should find the correct Order!");

		assertTrue(orderService.findById("id").isEmpty(), "findById() should not find an Order!");
	}

	/**
	 * testFindAll method
	 * Tests if all {@link Order} that are saved in the repository are returned
	 */
	@Test
	void testFindAll() {
		for (int i = 0; i < 10; i++) {
			final ContactInformation info = new ContactInformation("testName", "testAdresse", "testEmail");
			orderService.orderPickupItem(exampleCart, info);
		}

		assertEquals(10L, orderService.findAll().stream().count(), "findAll() should find all Orders!");
	}

	/**
	 * testOrderPickupItem method
	 * Tests if {@link Pickup} has all correct properties of the Order
	 */
	@Test
	void testOrderPickupItem() {
		final ContactInformation info = new ContactInformation("testName", "testAdresse", "testEmail");

		final Pickup order = orderService.orderPickupItem(exampleCart, info);

		final Pickup goalOrder = new Pickup(orderService.getDummyUser(), info);
		exampleCart.addItemsTo(goalOrder);

		assertEquals(goalOrder.getContactInformation(), order.getContactInformation(), "orderPickupItem() should use the correct ContactInformation!");

		final Iterator<OrderLine> goalOrderLineIterator = goalOrder.getOrderLines().get().iterator();
		final Iterator<OrderLine> orderOrderLineIterator = order.getOrderLines().get().iterator();

		for (int i = 0; i < goalOrder.getOrderLines().get().count(); i++) {
			final OrderLine goalOrderEntry = goalOrderLineIterator.next();
			final OrderLine orderOrderEntry = orderOrderLineIterator.next();

			assertEquals(goalOrderEntry.getProductName(), orderOrderEntry.getProductName(), "orderPickupItem() should add the correct Item!");
			assertEquals(goalOrderEntry.getPrice(), orderOrderEntry.getPrice(), "orderPickupItem() should add the correct Item!");
			assertEquals(goalOrderEntry.getQuantity(), orderOrderEntry.getQuantity(), "orderPickupItem() should add the correct Item!");
		}
	}

	/**
	 * testOrderDeliveryItem method
	 * Tests if a {@link Delivery} is booked for the correct delivery date, if all properties of the Order are correctly
	 * mapped to the Delivery and that the correct Lkw is booked for a specific order
	 */
	@Test
	void testOrderDeliveryItem() {
		final ContactInformation info = new ContactInformation("testName", "testAdresse", "testEmail");

		final LocalDate deliveryDate = businessTime.getTime().toLocalDate().plusDays(2);

		final LKW lkw = lkwService.createDeliveryLKW(deliveryDate, LKWType.SMALL).orElse(null);
		final Delivery order = orderService.orderDelieveryItem(exampleCart, info);

		final Delivery goalOrder = new Delivery(orderService.getDummyUser(), info, lkw, deliveryDate);
		exampleCart.addItemsTo(goalOrder);

		assertEquals(goalOrder.getDeliveryDate(), order.getDeliveryDate(), "orderDelieveryItem() should use the correct DeliveryDate!");
		assertEquals(goalOrder.getContactInformation(), order.getContactInformation(), "orderDelieveryItem() should use the correct ContactInformation!");
		assertEquals(goalOrder.getLkw(), order.getLkw(), "orderDelieveryItem() should use the correct LKW!");

		final Iterator<OrderLine> goalOrderLineIterator = goalOrder.getOrderLines().get().iterator();
		final Iterator<OrderLine> orderOrderLineIterator = order.getOrderLines().get().iterator();

		for (int i = 0; i < goalOrder.getOrderLines().get().count(); i++) {
			final OrderLine goalOrderEntry = goalOrderLineIterator.next();
			final OrderLine orderOrderEntry = orderOrderLineIterator.next();

			assertEquals(goalOrderEntry.getProductName(), orderOrderEntry.getProductName(), "orderDelieveryItem() should add the correct Item!");
			assertEquals(goalOrderEntry.getPrice(), orderOrderEntry.getPrice(), "orderDelieveryItem() should add the correct Item!");
			assertEquals(goalOrderEntry.getQuantity(), orderOrderEntry.getQuantity(), "orderDelieveryItem() should add the correct Item!");
		}
	}


	/**
	 * testOrderLKW method
	 * Tests if {@link LKWCharter} has all correct properties of the Order
	 */
	@Test
	void testOrderLKW() {
		final ContactInformation info = new ContactInformation("testName", "testAdresse", "testEmail");

		final LocalDate date = businessTime.getTime().toLocalDate();
		final LKW lkw = lkwService.createCharterLKW(date, LKWType.SMALL).orElse(null);

		final LKWCharter order = orderService.orderLKW(lkw, date, info);
		final LKWCharter goalOrder = new LKWCharter(orderService.getDummyUser(), info, lkw, date);

		assertEquals(goalOrder.getRentDate(), order.getRentDate(), "orderLKW() should use the correct RentDate!");
		assertEquals(goalOrder.getContactInformation(), order.getContactInformation(), "orderLKW() should use the correct ContactInformation!");
		assertEquals(goalOrder.getLkw(), order.getLkw(), "orderLKW() should use the correct LKW!");
	}

	/**
	 * testCancelLKW method
	 * Tests if a {@link LKW} is properly cancelled
	 */
	@Test
	void testCancelLKW() {
		final ContactInformation info = new ContactInformation("testName", "testAdresse", "testEmail");

		final LocalDate date = businessTime.getTime().toLocalDate();
		final LKW lkw = lkwService.createCharterLKW(date, LKWType.SMALL).orElse(null);

		final LKWCharter order = orderService.orderLKW(lkw, date, info);
		final String id = order.getId().getIdentifier();

		assertTrue(orderService.cancelLKW(order), "cancelLKW() should return the correct value!");
		assertTrue(orderService.findById(id).isEmpty(), "cancelLKW() should cancel the Order!");
	}

	/**
	 * testRemoveItemFromOrders method
	 * Tests if {@link Item} are properly removed out of all existing {@link ItemOrder} and that resulting empty {@link ItemOrder}
	 * are deleted respectively
	 */
	@Test
	void testRemoveItemFromOrders() {
		final ContactInformation info = new ContactInformation("testName", "testAdresse", "testEmail");
		final List<Product> items = exampleCart.get().map(CartItem::getProduct).collect(Collectors.toList());
		final Item item = (Item) items.get(0);

		final String id = orderService.orderPickupItem(exampleCart, info).getId().getIdentifier();
		orderService.removeItemFromOrders(item);

		final ItemOrder itemOrder = (ItemOrder) orderService.findById(id).get();
		assertTrue(() -> itemOrder.getOrderEntries().stream().noneMatch(entry -> entry.getItem().equals(item)), "removeItemFromOrders() should remove simular Items!");

		final Item item2 = (Item) items.get(1);

		orderService.removeItemFromOrders(item2);
		assertTrue(orderService.findById(id).isEmpty(), "removeItemFromOrders() should remove empty Orders!");
	}

	@Test
	void testGetStatus() {
		final ContactInformation info = new ContactInformation("testName", "testAdresse", "testEmail");
		final LKW lkw = new LKW(LKWType.SMALL);

		LocalDate date = businessTime.getTime().toLocalDate().minusDays(1);
		LKWCharter lkwOrder = new LKWCharter(orderService.getDummyUser(), info, lkw, date);
		assertEquals(OrderStatus.COMPLETED, orderService.getStatus(lkwOrder), "getStatus() should return the correct OrderStatus!");

		date = businessTime.getTime().toLocalDate().plusDays(1);
		lkwOrder = new LKWCharter(orderService.getDummyUser(), info, lkw, date);
		assertEquals(OrderStatus.PAID, orderService.getStatus(lkwOrder), "getStatus() should return the correct OrderStatus!");

		final Pickup itemOrder = new Pickup(orderService.getDummyUser(), info);
		assertEquals(OrderStatus.OPEN, orderService.getStatus(itemOrder), "getStatus() should return the correct OrderStatus!");

		exampleCart.addItemsTo(itemOrder);
		assertEquals(OrderStatus.OPEN, orderService.getStatus(itemOrder), "getStatus() should return the correct OrderStatus!");

		itemOrder.changeAllStatus(OrderStatus.PAID);
		assertEquals(OrderStatus.PAID, orderService.getStatus(itemOrder), "getStatus() should return the correct OrderStatus!");

		itemOrder.changeStatus(0, OrderStatus.COMPLETED);
		assertEquals(OrderStatus.PAID, orderService.getStatus(itemOrder), "getStatus() should return the correct OrderStatus!");
	}

}
