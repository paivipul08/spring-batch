package com.example.batch;

import org.springframework.batch.core.SkipListener;

public class CustomSkipListener implements SkipListener<Order, TrackingOrder> {

	@Override
	public void onSkipInRead(Throwable t) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSkipInWrite(TrackingOrder item, Throwable t) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSkipInProcess(Order item, Throwable t) {
		System.out.println("Skipping processing of item :"+item.getOrderId());
		
	}

}
