package com.easyruta.easyruta.utils.dataService;

/**
 * Created by dcoellar on 5/3/16.
 */
public class DataServiceOrder {

    private Boolean ascending;
    private String field;

    public DataServiceOrder(Boolean ascending, String field) {
        this.ascending = ascending;
        this.field = field;
    }

    public Boolean getAscending() {
        return ascending;
    }

    public void setAscending(Boolean ascending) {
        this.ascending = ascending;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }
}
