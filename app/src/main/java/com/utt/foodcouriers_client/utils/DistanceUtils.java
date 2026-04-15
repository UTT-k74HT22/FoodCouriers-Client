package com.utt.foodcouriers_client.utils;

public class DistanceUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;

    public static double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    public static String formatDistance(double distanceKm) {
        if (distanceKm < 1) {
            return String.format("%.0fm", distanceKm * 1000);
        } else {
            return String.format("%.1fkm", distanceKm);
        }
    }

    public static String formatDistanceFromCoordinates(Double restaurantLat, Double restaurantLon,
                                                        Double deliveryLat, Double deliveryLon) {
        if (restaurantLat == null || restaurantLon == null || deliveryLat == null || deliveryLon == null) {
            return "";
        }
        double distance = calculateDistanceKm(restaurantLat, restaurantLon, deliveryLat, deliveryLon);
        return formatDistance(distance);
    }
}