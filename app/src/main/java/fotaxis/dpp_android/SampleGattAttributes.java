/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fotaxis.dpp_android;

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class SampleGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    public static String SENSOR_DATA_SERVICE="50872a7f-7ea8-4d83-8bde-c6ee61bd4ef4";
    public static String SENSOR_DATA_TEMPERATURE="00002a6e-0000-1000-8000-00805f9b34fb";
    public static String SENSOR_DATA_CO2="e524eeca-e59b-4406-8fad-9f6d9e3c3cac";
    public static String SENSOR_DATA_CO="c17ff774-029a-4792-83e1-8ac25d1b7995";
    public static String SENSOR_DATA_ACCE="193dce4e-9e40-4b7b-a216-960c80b55c38";

    static {
        // Sample Services.
        attributes.put("0000180d-0000-1000-8000-00805f9b34fb", "Heart Rate Service");

        // Sample Services.
        attributes.put("00001800-0000-1000-8000-00805f9b34fb", "Generic Access");
        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");

        attributes.put(SENSOR_DATA_SERVICE, "Sensor Data Service");
        attributes.put("0000180f-0000-1000-8000-00805f9b34fb", "Battery Service");

        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
        attributes.put("00002a24-0000-1000-8000-00805f9b34fb", "Model Number");
        attributes.put("00002a25-0000-1000-8000-00805f9b34fb", "Serial Number");
        attributes.put("00002a00-0000-1000-8000-00805f9b34fb", "Device Name");
        attributes.put("00002a01-0000-1000-8000-00805f9b34fb", "Gap Appearance");
        attributes.put("00002a19-0000-1000-8000-00805f9b34fb", "Battery Level");

        attributes.put(SENSOR_DATA_TEMPERATURE, "Temperature");

        attributes.put(SENSOR_DATA_CO2, "CO2");
        attributes.put(SENSOR_DATA_CO, "Co");
        attributes.put(SENSOR_DATA_ACCE, "Accelerometer");



        // Sample Characteristics.
        attributes.put(HEART_RATE_MEASUREMENT, "Heart Rate Measurement");
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
