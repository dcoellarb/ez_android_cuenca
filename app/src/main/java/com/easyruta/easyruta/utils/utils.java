package com.easyruta.easyruta.utils;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by dcoellar on 11/30/15.
 */
public class utils {

    public static String formatDate(Date date){
        String result = "";

        String day = "";

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DATE, 1);

        if (calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) && calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) && calendar.get(Calendar.DATE) == today.get(Calendar.DATE)){
            day = "Hoy";
        }else if (calendar.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) && calendar.get(Calendar.MONTH) == yesterday.get(Calendar.MONTH) && calendar.get(Calendar.DATE) == yesterday.get(Calendar.DATE)){
            day = "Ayer";
        }else if (calendar.get(Calendar.YEAR) == tomorrow.get(Calendar.YEAR) && calendar.get(Calendar.MONTH) == tomorrow.get(Calendar.MONTH) && calendar.get(Calendar.DATE) == tomorrow.get(Calendar.DATE)) {
            day = "Manana";
        }else{
            day = monthName(calendar.get(Calendar.MONTH)) + " " + calendar.get(Calendar.DATE);
        }
        result = day + " a las " + formatTime(calendar.get(Calendar.HOUR)) + ":" + formatTime(calendar.get(Calendar.MINUTE)) + formatAMPM(calendar.get(Calendar.AM_PM));
        return result;
    }

    private static String monthName(int month){
        String name = "";
        switch (month){
            case 1:
                name = "Enero";
                break;
            case 2:
                name = "Febrero";
                break;
            case 3:
                name = "Marzo";
                break;
            case 4:
                name = "Abril";
                break;
            case 5:
                name = "Mayo";
                break;
            case 6:
                name = "Junio";
                break;
            case 7:
                name = "Julio";
                break;
            case 8:
                name = "Agosto";
                break;
            case 9:
                name = "Septiembre";
                break;
            case 0:
                name = "Octubre";
                break;
            case 11:
                name = "Noviembre";
                break;
            case 12:
                name = "Dicienmbre";
                break;
        }
        return name;
    }

    private static String formatTime(int time){
        String result = String.valueOf(time);
        if (time < 10){
            result = "0" + String.valueOf(time);
        }
        return result;
    }

    private static String formatAMPM(int ampm){
        String result = "pm";
        if (ampm == 1){
            result = "am";
        }
        return result;
    }

}
