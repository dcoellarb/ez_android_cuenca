package com.easyruta.easyruta.utils;

import com.easyruta.easyruta.Constants;
import com.pubnub.api.Pubnub;

/**
 * Created by dcoellar on 11/27/15.
 */
public class PubnubService {

    public static String NEW_PEDIDOS = "new_pedidos";
    public static String PEDIDO_TOMADO = "pedido_tomado";
    public static String PEDIDO_CONFIRMADO = "pedido_confirmado";
    public static String PEDIDO_CONFIRMADO_PROVEEDOR = "pedido_confirmado_proveedor";
    public static String PEDIDO_RECHAZADO = "pedido_rechazado";
    public static String PEDIDO_RECHAZADO_PROVEEDOR = "pedido_rechazado_proveedor";
    public static String PEDIDO_INICIADO = "pedido_iniciado";
    public static String PEDIDO_COMPLETADO = "pedido_completado";
    public static String PEDIDO_CANCELADO = "pedido_cancelado";
    public static String PEDIDO_CANCELADO_TRANSPORTISTA = "pedido_cancelado_transportista";
    public static String PEDIDO_CANCELADO_PROVOEEDOR = "pedido_cancelado_proveedor";
    public static String PEDIDO_CANCELADO_CONFIRMADO = "pedido_cancelado_confirmado";
    public static String PEDIDO_CANCELADO_CONFIRMADO_TRANSPORTISTA = "pedido_cancelado_confirmado_transportista";
    public static String PEDIDO_CANCELADO_CONFIRMADO_PROVEEDOR = "pedido_cancelado_confirmado_proveedor";
    public static String PEDIDO_TIMEOUT = "pedido_timeout";

    private String uuid;
    private Pubnub pubnub;

    public PubnubService(){

        pubnub = new Pubnub(Constants.PUBNUB_PUB_KEY, Constants.PUBNUB_SUB_KEY);
        String uuid = pubnub.uuid();
        pubnub.setUUID(uuid);

    }
    public String getUuid() { return uuid; }
    public Pubnub getPubnub() {
        return pubnub;
    }
}
