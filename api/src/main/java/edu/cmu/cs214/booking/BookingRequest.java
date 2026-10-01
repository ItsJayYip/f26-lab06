package edu.cmu.cs214.booking;

/** The inputs needed to create a booking. */
public record BookingRequest(String roomId, long startMinute, long endMinute,
                             String waitlistKey, String notes) {
}
