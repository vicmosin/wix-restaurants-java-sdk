package com.wix.restaurants.examples;

import com.openrest.v1_1.Item;
import com.openrest.v1_1.Order;
import com.openrest.v1_1.OrderItem;
import com.openrest.v1_1.RestaurantFullInfo;
import com.wix.restaurants.DefaultWixRestaurantsClient;
import com.wix.restaurants.WixRestaurantsClient;
import com.wix.restaurants.builders.ContactBuilder;
import com.wix.restaurants.examples.helpers.MenuHelper;
import com.wix.restaurants.helpers.PriceCalculator;
import com.wix.restaurants.i18n.Locale;
import com.wix.restaurants.orders.builders.OrderBuilder;
import com.wix.restaurants.orders.builders.OrderItemBuilder;
import com.wix.restaurants.orders.builders.PickupBuilder;
import com.wix.restaurants.payments.builders.CashPaymentBuilder;
import scala.concurrent.duration.Duration;

import java.util.Currency;

/**
 * Demonstrates the "Submit Order" flow.
 * 1) Retrieve the test restaurant's menu
 * 2) Create an order with 1 simple item, and 1 item with variations
 * 3) Submit the order
 * 4) Query the submitted order's status
 *
 * @see <a href="http://www.thetestaurant.com">The Testaurant</a>
 */
public class SubmitOrderExample {
    private final WixRestaurantsClient wixRestaurants;

    public SubmitOrderExample(WixRestaurantsClient wixRestaurants) {
        this.wixRestaurants = wixRestaurants;
    }

    public void runExample() {
        final String restaurantId = "8830975305376234"; // "The Testaurant"

        // 1. Retrieve Menu
        System.out.print("Retrieving menu...");
        final RestaurantFullInfo full = wixRestaurants.retrieveRestaurantInfo(restaurantId);
        System.out.println(" done (menus: " + full.menu.sections.size() + ", items: " + full.menu.items.size() + ").");

        // 2. Build Order
        final Order order = buildSomeOrder(full);

        // 3. Submit Order
        System.out.print("Submitting order...");
        final Order submittedOrder = wixRestaurants.submitOrder(null, order);
        System.out.println(" done (order ID: " + submittedOrder.id + ", status: " + submittedOrder.status +
                ", ownerToken: " + submittedOrder.ownerToken + ").");

        // 4. Query Order status
        System.out.print("Retrieving order...");
        final Order retrievedOrder = wixRestaurants.retrieveOrderAsOwner(submittedOrder.ownerToken, submittedOrder.restaurantId, submittedOrder.id);
        System.out.println(" done (status: " + retrievedOrder.status + ").");
    }

    private Order buildSomeOrder(RestaurantFullInfo full) {
        final MenuHelper menuHelper = new MenuHelper(full.menu);
        final PriceCalculator calculator = new PriceCalculator();

        // Create OrderItems (in a real scenario, the customer would be making these choices in the UI)
        final Item carpaccio = menuHelper.getItem("7285589409963911");
        final OrderItem carpaccioOrderItem = new OrderItemBuilder(carpaccio)
                .setComment("Extra cheese please")
                .build();

        final Item coke = menuHelper.getItem("1712127355705869");
        final Item smallCoke = menuHelper.getItem("6011645467251806");
        final OrderItem cokeOrderItem = new OrderItemBuilder(coke)
                .addChoice(0, new OrderItemBuilder(smallCoke, coke.variations.get(0)).build())
                .build();

        // Calculate OrderItems total price
        final double orderItemsPrice = calculator.price(carpaccioOrderItem, cokeOrderItem);

        return new OrderBuilder()
                .setDeveloper("org.example")
                .setRestaurant(full.restaurant.id)
                .setLocale(Locale.fromJavaLocale(java.util.Locale.US))
                .setCurrency(Currency.getInstance(full.restaurant.currency))
                .setContact(new ContactBuilder()
                        .setFirstName("John")
                        .setLastName("Doe")
                        .setPhone("+12024561111")
                        .setEmail("johndoe@example.org")
                        .build())
                .setDispatch(new PickupBuilder()
                        .forAsap()
                        .build())
                .addItem(carpaccioOrderItem)
                .addItem(cokeOrderItem)
                .setComment("I'm allergic to nuts.")
                .addPayment(new CashPaymentBuilder()
                        .amount(orderItemsPrice)
                        .build())
                .build();
    }

    public static void main(String[] args) throws Exception {
        final WixRestaurantsClient wixRestaurants = new DefaultWixRestaurantsClient(
                "https://api.wixrestaurants.com/v2",
                "https://auth.wixrestaurants.com/v2",
                Duration.Inf());

        new SubmitOrderExample(wixRestaurants).runExample();
    }
}
