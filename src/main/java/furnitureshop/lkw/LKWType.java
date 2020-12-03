package furnitureshop.lkw;

import org.javamoney.moneta.Money;
import org.salespointframework.core.Currencies;
import org.springframework.util.Assert;

import javax.money.MonetaryAmount;
import java.util.Optional;

public enum LKWType {

	SMALL(
			"3,5t", 2000, "smalllkw.jpg",
			Money.of(19.99, Currencies.EURO), Money.of(4.99, Currencies.EURO)
	),
	MEDIUM(
			"5,5t", 4000, "mediumlkw.jpg",
			Money.of(49.99, Currencies.EURO), Money.of(9.99, Currencies.EURO)
	),
	LARGE(
			"7,5t", 6000, "largelkw.jpg",
			Money.of(79.99, Currencies.EURO), Money.of(19.99, Currencies.EURO)
	);

	// Displayname of the LKWType
	private final String name;

	// Maximum transport weight
	private final int weight;

	// Picturename of the LKWType
	private final String picture;

	// Price if an LKW of this Type is rent
	private final MonetaryAmount charterPrice;

	// Price for an delivery with an LKW of this Type
	private final MonetaryAmount delieveryPrice;

	LKWType(String name, int weight, String picture, MonetaryAmount charterPrice, MonetaryAmount delieveryPrice) {
		this.name = name;
		this.weight = weight;
		this.picture = picture;
		this.charterPrice = charterPrice;
		this.delieveryPrice = delieveryPrice;
	}

	public String getName() {
		return name;
	}

	public int getWeight() {
		return weight;
	}

	public String getPicture() {
		return picture;
	}

	public MonetaryAmount getCharterPrice() {
		return charterPrice;
	}

	public MonetaryAmount getDelieveryPrice() {
		return delieveryPrice;
	}

	/**
	 * Finds an {@link LKWType} by its case insensitivity Enum-Name or Displayname.
	 * Similar to {@link LKWType#valueOf(String)} but with no {@link Exception}.
	 *
	 * @param name The name of the type
	 *
	 * @return The {@link LKWType} with this name
	 */
	public static Optional<LKWType> getByName(String name) {
		Assert.hasText(name, "Name must not be null");

		for (LKWType type : LKWType.values()) {
			if (type.name().equalsIgnoreCase(name) || type.getName().equalsIgnoreCase(name)) {
				return Optional.of(type);
			}
		}

		return Optional.empty();
	}

	/**
	 * Finds an {@link LKWType} by its weight. Used to find an {@link LKW} which can transport the weight.
	 * Returns the next biggest {@link LKWType} to fit the weight.
	 *
	 * @param weight The minimum weight of the {@link LKW}
	 *
	 * @return The {@link LKWType} with the weight
	 */
	public static Optional<LKWType> getByWeight(int weight) {
		Assert.isTrue(weight >= 0, "Weight must be greater than 0");

		LKWType minType = null;

		for (LKWType type : LKWType.values()) {
			if (type.weight >= weight && (minType == null || type.weight < minType.weight)) {
				minType = type;
			}
		}

		return Optional.ofNullable(minType);
	}

}