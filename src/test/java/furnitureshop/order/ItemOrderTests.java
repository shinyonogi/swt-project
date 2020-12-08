package furnitureshop.order;

import furnitureshop.inventory.Category;
import furnitureshop.inventory.Item;
import furnitureshop.inventory.Piece;
import furnitureshop.lkw.LKW;
import furnitureshop.lkw.LKWType;
import furnitureshop.supplier.Supplier;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.salespointframework.core.Currencies;
import org.salespointframework.quantity.Quantity;
import org.salespointframework.useraccount.UserAccount;

import javax.persistence.Entity;
import java.lang.reflect.Modifier;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class ItemOrderTests {

	UserAccount account;
	ContactInformation info;
	LKW lkw;
	LocalDate date;

	ItemOrder order;

	Item item1, item2;

	@BeforeEach
	void setUp() {
		this.account = mock(UserAccount.class);
		this.info = new ContactInformation("name", "address", "email");
		this.lkw = new LKW(LKWType.SMALL);
		this.date = LocalDate.now();

		this.order = new Delivery(account, info, lkw, date);

		final Supplier supplier = new Supplier("test", 0.2);

		this.item1 = new Piece(1, "Stuhl 1", Money.of(59.99, Currencies.EURO), "", "schwarz",
				"", supplier, 5, Category.CHAIR);
		this.item2 = new Piece(1, "Stuhl 2", Money.of(49.99, Currencies.EURO), "", "weiß",
				"", supplier, 5, Category.CHAIR);
	}

	@Test
	void testConstructorWithInvalidType() {
		assertThrows(IllegalArgumentException.class, () -> new Pickup(null, info),
				"Pickup() should throw an IllegalArgumentException if the name useraccount is invalid!"
		);
		assertThrows(IllegalArgumentException.class, () -> new Pickup(account, null),
				"Pickup() should throw an IllegalArgumentException if the name contactinformation is invalid!"
		);
		assertThrows(IllegalArgumentException.class, () -> new Delivery(null, info, lkw, date),
				"Delivery() should throw an IllegalArgumentException if the useraccount argument is invalid!"
		);
		assertThrows(IllegalArgumentException.class, () -> new Delivery(account, null, lkw, date),
				"Delivery() should throw an IllegalArgumentException if the contactinformation argument is invalid!"
		);
		assertThrows(IllegalArgumentException.class, () -> new Delivery(account, info, null, date),
				"Delivery() should throw an IllegalArgumentException if the lkw argument is invalid!"
		);
		assertThrows(IllegalArgumentException.class, () -> new Delivery(account, info, lkw, null),
				"Delivery() should throw an IllegalArgumentException if the date argument is invalid!"
		);
	}

	@Test
	void testItemOrderEntries() {
		order.addOrderLine(item1, Quantity.of(1));

		assertEquals(1, order.getOrderEntries().size(), "getOrderEntries() must contain 1 element");

		final ItemOrderEntry entry = order.getOrderEntries().get(0);
		assertEquals(item1, entry.getItem(), "getOrderEntries() must contain the correct Item");
		assertEquals(OrderStatus.OPEN, entry.getStatus(), "getOrderEntries() must contain the correct itemstatus");

		entry.setStatus(OrderStatus.CANCELLED);
		assertEquals(OrderStatus.CANCELLED, entry.getStatus(), "getOrderEntries() must contain the correct itemstatus");

		order.addOrderLine(item2, Quantity.of(2));

		assertEquals(3, order.getOrderEntries().size(), "getOrderEntries() must contain 3 elements");
	}

	@Test
	void testItemOrderIsAbstract() {
		assertTrue(Modifier.isAbstract(ItemOrder.class.getModifiers()), "ItemOrder should be an abstract class!");
	}

	@Test
	void testItemOrderIsChild() {
		assertTrue(ShopOrder.class.isAssignableFrom(ItemOrder.class), "ItemOrder must extends ShopOrder!");

		assertTrue(ItemOrder.class.isAssignableFrom(Pickup.class), "Pickup must extends ItemOrder!");
		assertTrue(ItemOrder.class.isAssignableFrom(Delivery.class), "Delivery must extends ItemOrder!");
	}

	@Test
	void testItemOrderIsEntity() {
		assertTrue(ItemOrder.class.isAnnotationPresent(Entity.class), "ItemOrder must have @Entity!");
		assertTrue(Pickup.class.isAnnotationPresent(Entity.class), "Pickup must have @Entity!");
		assertTrue(Delivery.class.isAnnotationPresent(Entity.class), "Delivery must have @Entity!");
	}

}
