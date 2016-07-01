package com.easyruta.easyruta.utils;

import com.easyruta.easyruta.Constants;
import com.pubnub.api.Pubnub;

/**
 * Created by dcoellar on 11/27/15.
 */
public class PubnubService {

    public static String PEDIDO_CREADO = "pedidoCreado";
    public static String PEDIDO_ASIGNADO = "pedidoAsignado";
    public static String PEDIDO_INICIADO = "pedidoIniciado";
    public static String PEDIDO_CARGA_INICIADA = "pedidoCargaIniciada";
    public static String PEDIDO_LLEGADA_MARCADA = "pedidoLlegadaMarcada";
    public static String PEDIDO_FINALIZADO = "pedidoFinalizado";
    public static String PEDIDO_CANCELADO = "pedidoCancelado";
    public static String LOCATION_ADDED = "locationAdded";

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
