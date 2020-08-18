package com.example.batch;

import java.math.BigDecimal;

import org.springframework.batch.item.ItemProcessor;

public class FreeshippingItemProcessor implements ItemProcessor<TrackingOrder, TrackingOrder> {

	@Override
	public TrackingOrder process(TrackingOrder item) throws Exception {
		if(item.getCost().compareTo(new BigDecimal(80)) ==1) {
			item.setFreeShipping(true);
		}
		return item.isFreeShipping()? item:null;
	}

}
