package com.example.batch;

import java.util.List;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.support.builder.CompositeItemProcessorBuilder;
import org.springframework.batch.item.validator.BeanValidatingItemProcessor;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.FileSystemResource;

@EnableBatchProcessing
@SpringBootApplication
public class SpringBatchApplication {
	
	//private static final Logger logger = LoggerFactory.getLogger(SpringBatchApplication.class);
	private static final Logger logger = Logger.getLogger(SpringBatchApplication.class.getName());
	
	public static String[] tokens = new String[] {"order_id","first_name","last_name","email","cost","item_id","item_name","ship_date"};
	@Autowired
	private JobBuilderFactory jobBuilderFactory;
	
	@Autowired
	private StepBuilderFactory stepBuilderFactory;

	@Autowired
	private DataSource dataSource;

	private static  String ORDER_SQL ="select order_id ,first_name ,last_name,"
			+ "email,cost,item_id,item_name,ship_date "
			+ " from SHIPPED_ORDER order by order_id";


	@Bean
	public JobExecutionDecider decider() {
		return new DeliveryDecider();
	}
	
	
	@Bean
	public Step leaveAtDoorStep() {
		return this.stepBuilderFactory.get("leaveAtDoorStep").tasklet(new Tasklet() {
			
			@Override
			public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
				logger.info("Leaving Package at door");
				return RepeatStatus.FINISHED;
			}
		}).build();
	}
	
	
	@Bean
	public Step storePackagerStep() {
		return this.stepBuilderFactory.get("storePackagerStep").tasklet(new Tasklet() {
			
			@Override
			public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
				logger.info("Package is stored while customer address is located");
				return RepeatStatus.FINISHED;
			}
		}).build();
	}
	
	
	@Bean
	public Step givePackageToCustomerStep() {
		return this.stepBuilderFactory.get("givePackageToCustomerStep").tasklet(new Tasklet() {
			
			@Override
			public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
				logger.info("Package given to customer");
				return RepeatStatus.FINISHED;
			}
		}).build();
	}
	
	
	@Bean
	public Step driveToAddressStep() {
		boolean lost=false;
		return this.stepBuilderFactory.get("driveToAddressStep").tasklet(new Tasklet() {
			
			@Override
			public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
				if(lost)
					throw new RuntimeException("Lost while driving to customer address");
				logger.info("Sucessfully arrived at customer address");
				return RepeatStatus.FINISHED;
			}
		}).build();
	}
	
	@Bean
	public Step packageItemStep() {
		return this.stepBuilderFactory.get("packageItemStep").tasklet(new Tasklet() {
			
			@Override
			public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
				String item = chunkContext.getStepContext().getJobParameters().get("item").toString();
				String date = chunkContext.getStepContext().getJobParameters().get("run.date").toString();
				logger.info(String.format("The Item %s been packaged on %s", item,date));
				return RepeatStatus.FINISHED;
			}
		}).build();
	}
	
	/*
	@Bean
	public Job deliverPackageJob() {
		return this.jobBuilderFactory.get("deliverPackageJob")
				.start(packageItemStep())
				.next(driveToAddressStep())
					.on("FAILED")
						.to(storePackagerStep())
					.from(driveToAddressStep())
						.on("*")
							.to(decider())
								.on("PRESENT")
									.to(givePackageToCustomerStep())
							.from(decider())
								.on("*").to(leaveAtDoorStep())
				.end()
				.build();
	}
	*/
	
	@Bean
	public ItemProcessor<Order,Order> orderValidatingProcessor() {
		BeanValidatingItemProcessor<Order> itemProcessor = new BeanValidatingItemProcessor<Order>();
		itemProcessor.setFilter(true);
		return itemProcessor;
	}
	
	@Bean
	public ItemProcessor<Order,TrackingOrder> customValidatingProcessor() {
		return new CustomValidatingProcessor();
	}
	
	@Bean
	public ItemProcessor<TrackingOrder,TrackingOrder> freeShippingProcessor() {
		return new FreeshippingItemProcessor();
	}
	
	@Bean
	public ItemProcessor<Order,TrackingOrder> compositeItemProcessor() {
		return new CompositeItemProcessorBuilder<Order,TrackingOrder>()
				.delegates(orderValidatingProcessor(),customValidatingProcessor(),freeShippingProcessor())
				.build();
	}
	
	@Bean
	public ItemReader<String> itemReader(){
		return new SimpleItemReader();
	}
	
	@Bean
	public ItemReader<Order> orderItemReader(){
		FlatFileItemReader<Order> itemReader = new FlatFileItemReader<Order>();
		itemReader.setLinesToSkip(1);
		itemReader.setResource(new FileSystemResource("D:/demo-workspace/spring-batch/data/shipped_orders.csv"));
		
		DefaultLineMapper<Order> lineMapper = new DefaultLineMapper<Order>();
		DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
		tokenizer.setNames(tokens);
		
		lineMapper.setLineTokenizer(tokenizer);
		lineMapper.setFieldSetMapper(new OrderFieldSetMapper());
		
		itemReader.setLineMapper(lineMapper);
		return itemReader;
		
	}
	
	@Bean
	public ItemReader<Order> jdbcOrderItemReader(){
		return new JdbcCursorItemReaderBuilder<Order>()
				.dataSource(dataSource)
				.name("jdbcOrderItemReader")
				.sql(ORDER_SQL)
				.rowMapper(new OrderRowMapper())
				.build();
		
	}
	
	@Bean
	public ItemReader<Order> jdbcPagingOrderItemReader() throws Exception{
		return new JdbcPagingItemReaderBuilder<Order>()
				.dataSource(dataSource)
				.name("jdbcOrderItemReader")
				.queryProvider(queryProvider())
				.rowMapper(new OrderRowMapper())
				.pageSize(10)
				.build();
		
	}
	


	private PagingQueryProvider queryProvider() throws Exception {
		SqlPagingQueryProviderFactoryBean factory = new SqlPagingQueryProviderFactoryBean();
		
		factory.setDataSource(dataSource);
		factory.setSelectClause("select order_id ,first_name ,last_name,email,cost,item_id,item_name,ship_date");
		factory.setFromClause("from SHIPPED_ORDER");
		factory.setSortKey("order_id");
		return factory.getObject();
	}


	@Bean
	public Step chunckBasedStep() {
		return this.stepBuilderFactory.get("chunckBasedStep")
				.<String,String>chunk(3)
				.reader(itemReader())
				.writer(new ItemWriter<String>() {

					@Override
					public void write(List<? extends String> items) throws Exception {
						System.out.println("Received list size "+items.size());
						items.forEach(System.out::println);
						
					}
				}).build() ;
	}
	
	@Bean
	public Step chunckBasedOrderStep() {
		return this.stepBuilderFactory.get("chunckBasedOrderStep")
				.<Order,Order>chunk(3)
				.reader(orderItemReader())
				.writer(new ItemWriter<Order>() {

					@Override
					public void write(List<? extends Order> items) throws Exception {
						System.out.println("Received list size "+items.size());
						items.forEach(System.out::println);
						
					}
				}).build() ;
	}
	
	@Bean
	public Step chunckBasedJdbcOrderStep() {
		return this.stepBuilderFactory.get("chunckBasedJdbcOrderStep")
				.<Order,Order>chunk(3)
				.reader(jdbcOrderItemReader())
				.writer(new ItemWriter<Order>() {

					@Override
					public void write(List<? extends Order> items) throws Exception {
						System.out.println("Received list size "+items.size());
						items.forEach(System.out::println);
						
					}
				}).build() ;
	}
	
	@Bean
	public Step chunckBasedJdbcPagingOrderStep() {
		return this.stepBuilderFactory.get("chunckBasedJdbcPagingOrderStepNew")
				.<Order,Order>chunk(10)
				.reader(jdbcOrderItemReader())
				.processor(orderValidatingProcessor())
				.writer(new ItemWriter<Order>() {

					@Override
					public void write(List<? extends Order> items) throws Exception {
						System.out.println("Received list size "+items.size());
						items.forEach(System.out::println);
						
					}
				}).build() ;
	}
	
	@Bean
	public Step chunckBasedJdbcPagingOrderCustomProcessorStep() {
		return this.stepBuilderFactory.get("chunckBasedJdbcPagingOrderCompositeProcessorStepNew1")
				.<Order,TrackingOrder>chunk(10)
				.reader(jdbcOrderItemReader())
//				.processor(customValidatingProcessor())
				.processor(compositeItemProcessor())
				.faultTolerant()
				.skip(OrderProcessingException.class)
				.skipLimit(5)
				.listener(new CustomSkipListener())
				.writer(new ItemWriter<TrackingOrder>() {

					@Override
					public void write(List<? extends TrackingOrder> items) throws Exception {
						System.out.println("Received list size "+items.size());
						items.forEach(System.out::println);
						
					}
				}).build() ;
	}
	

	


	


	@Bean
	public Job job() {
//		return this.jobBuilderFactory.get("job").start(chunckBasedStep()).build();
//		return this.jobBuilderFactory.get("job").start(chunckBasedOrderStep()).build();
//		return this.jobBuilderFactory.get("job").start(chunckBasedJdbcOrderStep()).build();
//		return this.jobBuilderFactory.get("job").start(chunckBasedJdbcPagingOrderStep()).build();
		return this.jobBuilderFactory.get("job").start(chunckBasedJdbcPagingOrderCustomProcessorStep()).build();
	}
	
	public static void main(String[] args) {
		SpringApplication.run(SpringBatchApplication.class, args);
	}
	
	

}
