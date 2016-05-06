package com.easyruta.easyruta.utils.exceptions;

/**
 * Created by dcoellar on 4/26/16.
 */
public class EzException extends Exception {

    private EzExceptions type;

    public EzException(EzExceptions type){
        super(type.toString());
        this.type = type;
    }

    public EzExceptions getType() {
        return type;
    }
}

