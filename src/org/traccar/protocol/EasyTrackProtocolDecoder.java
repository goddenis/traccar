/*
 * Copyright 2013 Anton Tananaev (anton.tananaev@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.protocol;

import java.util.Calendar;
import java.util.Properties;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;

import org.traccar.BaseProtocolDecoder;
import org.traccar.database.DataManager;
import org.traccar.helper.Log;
import org.traccar.model.ExtendedInfoFormatter;
import org.traccar.model.Position;

public class EasyTrackProtocolDecoder extends BaseProtocolDecoder {

    public EasyTrackProtocolDecoder(String protocol) {
        super(protocol);
    }

    static private Pattern pattern = Pattern.compile(
            "\\*..," +                          // Manufacturer
            "(\\d+)," +                         // IMEI
            "([^,]{2})," +                      // Command
            "([AV])," +                         // Validity
            "(\\p{XDigit}{2})" +                // Year
            "(\\p{XDigit}{2})" +                // Month
            "(\\p{XDigit}{2})," +               // Day
            "(\\p{XDigit}{2})" +                // Hours
            "(\\p{XDigit}{2})" +                // Minutes
            "(\\p{XDigit}{2})," +               // Seconds
            "(\\p{XDigit})" +
            "(\\p{XDigit}{7})," +               // Latitude
            "(\\p{XDigit})" +
            "(\\p{XDigit}{7})," +               // Longitude
            "(\\p{XDigit}{4})," +               // Speed
            "(\\p{XDigit}{4})," +               // Course
            "(\\p{XDigit}{8})," +               // Status
            "(\\p{XDigit}+)," +                 // Signal
            "(\\d+)," +                         // Power
            "(\\p{XDigit}{4})," +               // Oil
            "(\\p{XDigit}+),?" +                // Milage
            "(\\d+)?" +                         // Altitude
            ".*");

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        String sentence = (String) msg;

        // Parse message
        Matcher parser = pattern.matcher(sentence);
        if (!parser.matches()) {
            return null;
        }

        // Create new position
        Position position = new Position();
        ExtendedInfoFormatter extendedInfo = new ExtendedInfoFormatter(getProtocol());

        Integer index = 1;

        // Get device by IMEI
        if (!identify(parser.group(index++))) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        // Command
        extendedInfo.set("command", parser.group(index++));

        // Validity
        position.setValid(parser.group(index++).compareTo("A") == 0);
        
        // Date
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.YEAR, 2000 + Integer.parseInt(parser.group(index++), 16));
        time.set(Calendar.MONTH, Integer.parseInt(parser.group(index++), 16) - 1);
        time.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parser.group(index++), 16));
        time.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parser.group(index++), 16));
        time.set(Calendar.MINUTE, Integer.parseInt(parser.group(index++), 16));
        time.set(Calendar.SECOND, Integer.parseInt(parser.group(index++), 16));
        position.setTime(time.getTime());

        // Location
        int hemisphere = parser.group(index++).equals("8") ? -1 : 1;
        position.setLatitude(
                hemisphere * Integer.parseInt(parser.group(index++), 16) / 600000.0);

        hemisphere = parser.group(index++).equals("8") ? -1 : 1;
        position.setLongitude(
                hemisphere * Integer.parseInt(parser.group(index++), 16) / 600000.0);
        
        position.setSpeed(Integer.parseInt(parser.group(index++), 16) / 100.0);
        position.setCourse(Integer.parseInt(parser.group(index++), 16) / 100.0);

        // Status
        extendedInfo.set("status", parser.group(index++));

        // Signal
        extendedInfo.set("signal", parser.group(index++));

        // Power
        extendedInfo.set("power", Double.valueOf(parser.group(index++)));

        // Oil
        extendedInfo.set("oil", Integer.parseInt(parser.group(index++), 16));

        // Milage
        extendedInfo.set("milage", Integer.parseInt(parser.group(index++), 16));
        
        // Altitude
        String altitude = parser.group(index++);
        if (altitude != null) {
            position.setAltitude(Double.valueOf(altitude));
        } else {
            position.setAltitude(0.0);
        }

        position.setExtendedInfo(extendedInfo.toString());
        return position;
    }

}
