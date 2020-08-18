package com.example.batch;

import org.springframework.beans.BeanUtils;

public class TrackingOrder extends Order{
	private String trackingNumber;
	
	private boolean freeShipping;
	
	public TrackingOrder() {
		// TODO Auto-generated constructor stub
	}
	
	public TrackingOrder(Order order) {
		BeanUtils.copyProperties(order, this);
	}

	public String getTrackingNumber() {
		return trackingNumber;
	}

	public void setTrackingNumber(String trackingNumber) {
		this.trackingNumber = trackingNumber;
	}

	public boolean isFreeShipping() {
		return freeShipping;
	}

	public void setFreeShipping(boolean freeShipping) {
		this.freeShipping = freeShipping;
	}

	@Override
	public String toString() {
		return "TrackingOrder [trackingNumber=" + trackingNumber + ", freeShipping=" + freeShipping + "]";
	}
	
	
}
