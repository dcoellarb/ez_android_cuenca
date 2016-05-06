package com.easyruta.easyruta.utils.dataService;

import java.util.Collection;

/**
 * Created by dcoellar on 5/3/16.
 */
public class DataServiceQueryParameters {

    private Collection<DataServiceFilter> filters;
    private Collection<DataServiceOrder> orders;
    private Collection<String> includes;

    public DataServiceQueryParameters(Collection<DataServiceFilter> filters, Collection<DataServiceOrder> orders, Collection<String> includes) {
        this.filters = filters;
        this.orders = orders;
        this.includes = includes;
    }

    public Collection<DataServiceFilter> getFilters() {
        return filters;
    }

    public Collection<DataServiceOrder> getOrders() {
        return orders;
    }

    public Collection<String> getIncludes() {
        return includes;
    }
}
