package com.example.batch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

public class SimpleItemReader implements ItemReader<String> {
	
	private List<String> dataset = new ArrayList<String>();
	private Iterator<String> iterator;
	
	public SimpleItemReader() {
		this.dataset.add("1");
		this.dataset.add("2");
		this.dataset.add("3");
		this.dataset.add("4");
		this.dataset.add("5");
		this.dataset.add("6");
		this.dataset.add("7");
		this.iterator = this.dataset.iterator();
	}

	@Override
	public String read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
		return this.iterator.hasNext() ? this.iterator.next() : null ;
	}

}
