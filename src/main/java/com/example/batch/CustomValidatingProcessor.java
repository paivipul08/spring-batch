package com.example.batch;

import java.util.UUID;

import org.springframework.batch.item.ItemProcessor;

public class CustomValidatingProcessor implements ItemProcessor<Order, TrackingOrder> {

	@Override
	public TrackingOrder process(Order item) throws Exception {
		TrackingOrder trackingOrder = new TrackingOrder(item);
		trackingOrder.setTrackingNumber(getTrackingNumber());
		return trackingOrder;
	}
	
	private String getTrackingNumber() throws OrderProcessingException {
		if(Math.random() < .30) {
			throw new OrderProcessingException();
		}
		
		return UUID.randomUUID().toString();
	}

}
