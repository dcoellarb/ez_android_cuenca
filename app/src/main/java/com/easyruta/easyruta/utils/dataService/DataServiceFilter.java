package com.easyruta.easyruta.utils.dataService;

/**
 * Created by dcoellar on 5/3/16.
 */
public class DataServiceFilter {

    private String operator;
    private String field;
    private Object value;

    public DataServiceFilter(String operator, String field, Object value) {
        this.operator = operator;
        this.field = field;
        this.value = value;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
