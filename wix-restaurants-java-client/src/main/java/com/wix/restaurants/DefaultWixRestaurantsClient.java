package com.wix.restaurants;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.api.client.http.HttpRequestFactory;
import com.openrest.v1_1.*;
import com.openrest.v1_1.Error;
import com.wix.restaurants.authentication.DefaultWixRestaurantsAuthenticationClient;
import com.wix.restaurants.authentication.WixRestaurantsAuthenticationClient;
import com.wix.restaurants.exceptions.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DefaultWixRestaurantsClient implements WixRestaurantsClient {
    private final OpenrestClient openrest;
    private final WixRestaurantsAuthenticationClient authenticationClient;

    public DefaultWixRestaurantsClient(HttpRequestFactory requestFactory, Integer connectTimeout, Integer readTimeout,
                                       Integer numberOfRetries) {
        openrest = new OpenrestClient(requestFactory, connectTimeout, readTimeout, numberOfRetries, Endpoints.PRODUCTION);
        authenticationClient = new DefaultWixRestaurantsAuthenticationClient(
                requestFactory, connectTimeout, readTimeout, numberOfRetries,
                com.wix.restaurants.authentication.Endpoints.PRODUCTION);
    }

    @Override
    public WixRestaurantsAuthenticationClient getAuthenticationClient() {
        return authenticationClient;
    }

    @Override
    public RestaurantFullInfo retrieveRestaurantInfo(String restaurantId) {
        final GetOrganizationFullRequest getOrganizationFullRequest = new GetOrganizationFullRequest();
        getOrganizationFullRequest.organizationId = restaurantId;

        final RestaurantFullInfo getOrganizationFullResponse = request(
                getOrganizationFullRequest, new TypeReference<Response<RestaurantFullInfo>>() {});

        return getOrganizationFullResponse;
    }

    @Override
    public Order submitOrder(String accessToken, Order order) {
        final SubmitOrderRequest submitOrderRequest = new SubmitOrderRequest();
        submitOrderRequest.accessToken = accessToken;
        submitOrderRequest.order = order;

        final OrderConfirmation submitOrderResponse = request(
                submitOrderRequest, new TypeReference<Response<OrderConfirmation>>() {});

        return submitOrderResponse.order;
    }

    @Override
    public Order retrieveOrderAsOwner(String orderId, String ownerToken) {
        final GetOrderRequest getOrderRequest = new GetOrderRequest();
        getOrderRequest.orderId = orderId;
        getOrderRequest.viewMode = Order.ORDER_VIEW_MODE_CUSTOMER;
        getOrderRequest.ownerToken = ownerToken;

        final Order getOrderResponse = request(
                getOrderRequest, new TypeReference<Response<Order>>() {});

        return getOrderResponse;
    }

    @Override
    public Order retrieveOrderAsRestaurant(String accessToken, String orderId) {
        final GetOrderRequest getOrderRequest = new GetOrderRequest();
        getOrderRequest.accessToken = accessToken;
        getOrderRequest.orderId = orderId;
        getOrderRequest.viewMode = Order.ORDER_VIEW_MODE_RESTAURANT;

        final Order getOrderResponse = request(
                getOrderRequest, new TypeReference<Response<Order>>() {});

        return getOrderResponse;
    }


    @Override
    public List<SearchResult> search(Filter filter, int limit) {
        final SearchRequest searchRequest = new SearchRequest();
        searchRequest.filter = filter;
        searchRequest.limit = limit;

        final SearchResponse searchResponse = request(
                searchRequest, new TypeReference<Response<SearchResponse>>() {});

        return searchResponse.results;
    }

    @Override
    public List<Order> retrieveNewOrders(String accessToken, String restaurantId) {
        final QueryOrdersRequest queryOrdersRequest = new QueryOrdersRequest();
        queryOrdersRequest.accessToken = accessToken;
        queryOrdersRequest.restaurantIds = Collections.singleton(restaurantId);
        queryOrdersRequest.viewMode = Order.ORDER_VIEW_MODE_RESTAURANT;
        queryOrdersRequest.status = Order.ORDER_STATUS_NEW;
        queryOrdersRequest.ordering = "asc";
        queryOrdersRequest.limit = Integer.MAX_VALUE;

        final OrdersResponse queryOrdersResponse = request(
                queryOrdersRequest, new TypeReference<Response<OrdersResponse>>() {});

        return queryOrdersResponse.results;
    }

    @Override
    public Order acceptOrder(String accessToken, String orderId, Map<String, String> externalIds) {
        final SetOrderStatusRequest setOrderStatusRequest = new SetOrderStatusRequest();
        setOrderStatusRequest.accessToken = accessToken;
        setOrderStatusRequest.orderId = orderId;
        setOrderStatusRequest.status = Order.ORDER_STATUS_ACCEPTED;
        setOrderStatusRequest.externalIds = externalIds;

        final Order setOrderStatusResponse = request(
                setOrderStatusRequest, new TypeReference<Response<Order>>() {});

        return setOrderStatusResponse;
    }

    @Override
    public Order rejectOrder(String accessToken, String orderId, String comment) {
        final SetOrderStatusRequest setOrderStatusRequest = new SetOrderStatusRequest();
        setOrderStatusRequest.accessToken = accessToken;
        setOrderStatusRequest.orderId = orderId;
        setOrderStatusRequest.status = Order.ORDER_STATUS_CANCELLED;
        setOrderStatusRequest.comment = comment;

        final Order setOrderStatusResponse = request(
                setOrderStatusRequest, new TypeReference<Response<Order>>() {});

        return setOrderStatusResponse;
    }

    private <T> T request(Request request, TypeReference<Response<T>> responseType) {
        try {
            return openrest.request(request, responseType);
        } catch (IOException e) {
            throw new CommunicationException(e.getMessage(), e);
        } catch (OpenrestException e) {
            throw translateException(e);
        }
    }

    private static RestaurantsException translateException(OpenrestException e) {
        switch (e.error()) {
            case Error.ERROR_NO_PERMISSION:
                return new NoPermissionException(e.errorMessage(), e);
            case Error.ERROR_INVALID_DATA:
                return new InvalidDataException(e.errorMessage(), e);
            case Error.ERROR_INTERNAL:
                return new InternalException(e.errorMessage(), e);
            default:
                return new RestaurantsException(e.error() + "|" + e.errorMessage(), e);
        }
    }
}
