package se.gu.utils;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class CurrentTimeDateCalendar  implements Serializable {
    private static final long serialVersionUID = 3031739383720173779L;

    public static void printCurrentTimeUsingDate() {

        Date date = new Date();

        String strDateFormat = "hh:mm:ss a";

        DateFormat dateFormat = new SimpleDateFormat(strDateFormat);

        String formattedDate= dateFormat.format(date);

        System.out.println(formattedDate);

    }

    public static void printCurrentTimeUsingCalendar() {

        Calendar cal = Calendar.getInstance();

        Date date=cal.getTime();

        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

        String formattedDate=dateFormat.format(date);

        System.out.println(formattedDate);

    }
}
