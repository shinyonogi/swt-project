package furnitureshop.inventory;

import furnitureshop.supplier.Supplier;
import org.javamoney.moneta.Money;
import org.salespointframework.core.Currencies;
import org.salespointframework.time.BusinessTime;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This class manages all HTTP Requests for Items
 */
@Controller
public class ItemController {

	private final ItemService itemService;

	// A refernce to the BusinessTime to access the current system time
	private final BusinessTime businessTime;

	/**
	 * Creates a new instance of {@link ItemController}
	 *
	 * @param itemService  The {@link ItemService} to access system information
	 * @param businessTime The {@link BusinessTime} to get the current time
	 *
	 * @throws IllegalArgumentException If any argument is {@code null}
	 */
	public ItemController(ItemService itemService, BusinessTime businessTime) {
		Assert.notNull(itemService, "ItemService must not be null");
		Assert.notNull(businessTime, "BusinessTime must not be null!");

		this.itemService = itemService;
		this.businessTime = businessTime;
	}

	/**
	 * Handles all GET-Requests for '/catalog'
	 *
	 * @param model The {@code Spring} Page {@link Model}
	 *
	 * @return Returns the page with a list of all available {@link Item}s
	 */
	@GetMapping("/catalog")
	String getCatalog(Model model) {
		model.addAttribute("items", itemService.findAllVisible());

		return "catalog";
	}

	/**
	 * Handles all GET-Requests for '/catalog/{type}'.
	 *
	 * @param category The Category of the Item
	 * @param model    The {@code Spring} Page {@link Model}
	 *
	 * @return Returns the page with a list of all available {@link Item}s of
	 * the given {@link Category} if the {@code category} is present. Otherwise it redirects to '/catalog'
	 */
	@GetMapping("/catalog/{type}")
	String getCategory(@PathVariable("type") String category, Model model) {
		final Optional<Category> cat = Category.getByName(category);

		if (cat.isPresent()) {
			model.addAttribute("items", itemService.findAllVisibleByCategory(cat.get()));
			return "catalog";
		}

		return "redirect:/catalog";
	}

	/**
	 * Handles all GET-Requests for '/catalog/{category}/{itemId}'.
	 *
	 * @param category The Category of the {@link Item}
	 * @param item     The requested {@link Item}
	 * @param model    The {@code Spring} Page {@link Model}
	 *
	 * @return Returns a page with details of the {@link Item} if all arguments are given. Otherwise it redirects either
	 * to '/catalog' if no {@code category} is given or to '/catalog/ + category' if the Item is missing.
	 */
	@GetMapping("/catalog/{category}/{itemId}")
	String getItemDetails(@PathVariable("category") String category, @PathVariable("itemId") Optional<Item> item, Model model) {
		if (item.isEmpty()) {
			return "redirect:/catalog/" + category;
		}

		final Optional<Category> cat = Category.getByName(category);

		if (cat.isEmpty() || cat.get() != item.get().getCategory()) {
			return "redirect:/catalog";
		}

		model.addAttribute("item", item.get());
		model.addAttribute("variants", itemService.findAllVisibleByGroupId(item.get().getGroupId()));

		return "itemView";
	}

	/**
	 * Handles all GET-Requests for '/admin/supplier/{id}/items/add'.
	 *
	 * @param suppId The id of a {@link Supplier}
	 * @param model  The {@code Spring} Page {@link Model}
	 *
	 * @return Returns a redirect to '/admin/supplier/{id}/sets/add' if the given supplier is the Set supplier.
	 * Otherwise it populates the model with an instance of itemForm {@link ItemForm} for the addition of new items
	 * and then returns the view for the item addition for normal suppliers.
	 */
	@GetMapping("/admin/supplier/{id}/items/add")
	String getAddItemForSupplier(@PathVariable("id") long suppId, Model model) {
		Optional<Supplier> supplier = itemService.findSupplierById(suppId);
		if (supplier.isPresent()) {
			if (supplier.get().getName().equals("Set Supplier")) {
				return String.format("redirect:/admin/supplier/%s/sets/add", suppId);
			}
		}

		model.addAttribute("itemForm", new ItemForm(0, 0, "", "placeholder.png", "", "", 0, null));
		model.addAttribute("suppId", suppId);
		model.addAttribute("categories", Category.values());
		model.addAttribute("edit", false);

		return "supplierItemform";
	}

	/**
	 * Handles all POST-Requests for '/admin/supplier/{id}/items/add'.
	 * If the given {@link Supplier} is not present then the page will return the form page again.
	 * Otherwise a new piece will be constructed from the {@link ItemForm} and saved into the {@link ItemCatalog}
	 * via the {@link ItemService}.
	 *
	 * @param suppId The id of a {@link Supplier}
	 * @param form   The {@link ItemForm} with the information about new {@link Item}
	 * @param model  The {@code Spring} Page {@link Model}
	 *
	 * @return Returns a redirect to '/admin/supplier/{id}/items' when everything was successfully created. Otherwise
	 * returns the user created {@link ItemForm} and the corresponding view.
	 */
	@PostMapping("/admin/supplier/{id}/items/add")
	String addItemForSupplier(@PathVariable("id") long suppId, @ModelAttribute("itemForm") ItemForm form, Model model) {
		final Optional<Supplier> supplier = itemService.findSupplierById(suppId);

		if (supplier.isEmpty()) {
			model.addAttribute("itemForm", form);
			return "supplierItemform";
		}

		final Piece piece = new Piece(
				form.getGroupId(),
				form.getName(),
				Money.of(form.getPrice(), Currencies.EURO),
				form.getPicture(),
				form.getVariant(),
				form.getDescription(),
				supplier.get(),
				form.getWeight(),
				form.getCategory()
		);

		itemService.addOrUpdateItem(piece);

		return String.format("redirect:/admin/supplier/%d/items", suppId);
	}

	/**
	 * Handles all GET-Requests for '/admin/supplier/{id}/sets/add'.
	 *
	 * @param id    The id of a {@link Supplier}
	 * @param model The {@code Spring} Page {@link Model}
	 *
	 * @return Returns a page with all possible values of {@link Category} to choose from.
	 */
	@GetMapping("/admin/supplier/{id}/sets/add")
	String getAddSetsForSupplier(@PathVariable("id") long id, Model model) {
		model.addAttribute("categories", Category.values());
		model.addAttribute("suppId", id);
		model.addAttribute("showCats", true);
		return "supplierSetform";
	}

	/**
	 * Handles all POST-Requests for '/admin/supplier/{id}/sets/add'.
	 * Creates a {@link EnumMap} mapping a {@link Category} to a {@link Streamable} of {@link Item} and binds that to
	 * the model.
	 *
	 * @param suppId     The id of a {@link Supplier}
	 * @param categories The {@link List} of selected {@link Category}s
	 * @param model      The {@code Spring} Page {@link Model}
	 *
	 * @return Returns the view for set addition with the proper selection of {@link Item} instances.
	 */
	@PostMapping("/admin/supplier/{suppId}/sets/add")
	String getDetailSetAddPage(@PathVariable("suppId") long suppId, @RequestParam("checkedCategories") List<Category> categories, Model model) {
		final Map<Category, Streamable<Item>> itemMap = new EnumMap<>(Category.class);

		for (Category category : categories) {
			itemMap.put(category, itemService.findAllByCategory(category));
		}

		model.addAttribute("itemMap", itemMap);
		model.addAttribute("showCats", false);
		model.addAttribute("suppId", suppId);
		model.addAttribute("setForm", new ItemForm(0, 0, "", "placeholder.png", "", "", 0, Category.SET));

		return "supplierSetform";
	}

	/**
	 * Handles all POST-Requests for '/admin/supplier/{id}/sets/add/set'.
	 * Creates a new {@link Set} and saves it to {@link ItemCatalog} if the given {@link Supplier} is the SetSupplier.
	 *
	 * @param suppId   The id of a {@link Supplier}
	 * @param form     The {@link ItemForm} with the information about new {@link Item}
	 * @param itemList A {@link List} of selected {@link Item}s to be added to the {@link Set}
	 *
	 * @return Either redirects to the supplier overview when {@link Supplier} is not found or to the item overview of
	 * the SetSupplier when everything was correctly created.
	 */
	@PostMapping("/admin/supplier/{suppId}/sets/add/set")
	String addSetForSupplier(@PathVariable("suppId") long suppId, @ModelAttribute("setForm") ItemForm form, @RequestParam("itemList") List<Item> itemList) {
		final Optional<Supplier> supplier = itemService.findSupplierById(suppId);

		if (supplier.isEmpty()) {
			return "redirect:/admin/suppliers";
		}

		if (!supplier.get().getName().equals("Set Supplier")) {
			return String.format("redirect:/admin/supplier/%d/items", suppId);
		}

		/*
		if (itemList.size() == 0) {
			return String.format("redirect:/admin/supplier/%d/sets/add", suppId);
		}*/

		final Set set = new Set(
				form.getGroupId(),
				form.getName(),
				Money.of(form.getPrice(), Currencies.EURO),
				form.getPicture(),
				form.getVariant(),
				form.getDescription(),
				supplier.get(),
				itemList
		);

		itemService.addOrUpdateItem(set);

		return String.format("redirect:/admin/supplier/%d/items", suppId);
	}

	/**
	 * Handles all GET-Requests for '/admin/supplier/{suppId}/items/edit/{itemId}'.
	 *
	 * @param suppId The id of a {@link Supplier}
	 * @param item   The {@link Item} to be edited
	 * @param model  The {@code Spring} Page {@link Model}
	 *
	 * @return Returns the edit page for a item with all attributes from the given item prefilled.
	 */
	@GetMapping("/admin/supplier/{suppId}/items/edit/{itemId}")
	String getEditItemForSupplier(@PathVariable("suppId") long suppId, @PathVariable("itemId") Item item, Model model) {
		if (suppId != item.getSupplier().getId()) {
			return String.format("redirect:/admin/supplier/%d/items", suppId);
		}

		model.addAttribute("itemForm", new ItemForm(item.getGroupId(), item.getWeight(), item.getName(), item.getPicture(), item.getVariant(), item.getDescription(), item.getSupplierPrice().getNumber().doubleValue(), item.getCategory()));
		model.addAttribute("suppId", suppId);
		model.addAttribute("itemId", item.getId());
		model.addAttribute("categories", Category.values());
		model.addAttribute("edit", true);

		return "supplierItemform";
	}

	/**
	 * Handles all POST-Requests for '/admin/supplier/{suppId}/items/edit/{itemId}'.
	 * Sets the new information's of an {@link Item} from the {@link ItemForm} and updates the {@link ItemCatalog} via
	 * the {@link ItemService}, but only if the given {@link Supplier} is valid.
	 *
	 * @param suppId The id of a {@link Supplier}
	 * @param item   The {@link Item} to be edited
	 * @param form   The {@link ItemForm} with the information about new {@link Item}
	 *
	 * @return Redirects to the item overview of the given supplier.
	 */
	@PostMapping("/admin/supplier/{suppId}/items/edit/{itemId}")
	String editItemForSupplier(@PathVariable("suppId") long suppId, @PathVariable("itemId") Item item, @ModelAttribute("itemForm") ItemForm form) {
		if (suppId != item.getSupplier().getId()) {
			return String.format("redirect:/admin/supplier/%d/items", suppId);
		}

		item.setName(form.getName());
		item.setPrice(Money.of(form.getPrice(), Currencies.EURO));
		item.setDescription(form.getDescription());
		item.setPicture(form.getPicture());

		itemService.addOrUpdateItem(item);

		return String.format("redirect:/admin/supplier/%d/items", suppId);
	}

	/**
	 * Handles all POST-Requests for '/admin/supplier/{suppId}/items/delete/{itemId}'.
	 * Deletes the given {@link Item} if the supplier is valid.
	 *
	 * @param suppId The id of a {@link Supplier}
	 * @param item   The {@link Item} to delete
	 *
	 * @return Redirects to the item overview of the given supplier.
	 */
	@PostMapping("/admin/supplier/{suppId}/items/delete/{itemId}")
	String deleteItemForSupplier(@PathVariable("suppId") long suppId, @PathVariable("itemId") Item item) {
		if (suppId != item.getSupplier().getId()) {
			return String.format("redirect:/admin/supplier/%d/items", suppId);
		}

		itemService.removeItem(item);

		return String.format("redirect:/admin/supplier/%d/items", suppId);
	}

	/**
	 * Handles all POST-Requests for '/admin/supplier/{suppId}/items/toggle/{itemId}'.
	 * Changes the visibility status of the given {@link Item} if the supplier is valid.
	 *
	 * @param suppId The id of a {@link Supplier}
	 * @param item   The {@link Item} to be toggle visibility
	 *
	 * @return Redirects to the item overview of the given supplier.
	 */
	@PostMapping("/admin/supplier/{suppId}/items/toggle/{itemId}")
	String toggleItemForSupplier(@PathVariable("suppId") long suppId, @PathVariable("itemId") Item item) {
		if (suppId != item.getSupplier().getId()) {
			return String.format("redirect:/admin/supplier/%d/items", suppId);
		}

		item.setVisible(!item.isVisible());

		itemService.addOrUpdateItem(item);

		return String.format("redirect:/admin/supplier/%d/items", suppId);
	}

	/**
	 * Handles all GET-Requests for '/admin/statistic'
	 *
	 * @param model The {@code Spring} Page {@link Model}
	 *
	 * @return Returns the montly statistic page.
	 */
	@GetMapping("/admin/statistic")
	String getMonthlyStatistic(Model model) {
		final LocalDateTime time = businessTime.getTime();

		model.addAttribute("statistic", itemService.analyse(time));
		model.addAttribute("timeFrom", time.minusDays(30));
		model.addAttribute("timeTo", time);

		return "monthlyStatistic";
	}

}
