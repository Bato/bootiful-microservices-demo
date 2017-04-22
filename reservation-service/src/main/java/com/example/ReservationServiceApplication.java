package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Collection;
import java.util.stream.Stream;


interface ReservationServiceChannels {
	@Input
	// SubscribableChannel input();
	MessageChannel input();
}

@EnableBinding(ReservationServiceChannels.class)
@EnableDiscoveryClient
@SpringBootApplication
public class ReservationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReservationServiceApplication.class, args);
	}
}

//@MessageEndpoint
//class ReservationProcessor {
//
//	private final ReservationRepository reservationRepository;
//
//	@Autowired
//	public ReservationProcessor(ReservationRepository reservationRepository) {
//		this.reservationRepository = reservationRepository;
//	}
//
//	@ServiceActivator(inputChannel = "input")
//	public void acceptNewReservations(Message<String> msg) {
//		String rn = msg.getPayload();
//
//		this.reservationRepository.save(new Reservation(rn));
//	}
//}

@MessageEndpoint
class ReservationProcessor {

    private final ReservationRepository reservationRepository;

    @ServiceActivator(inputChannel = "input")
    public void onMessage(Message<String> msg) {
        this.reservationRepository.save(new Reservation(msg.getPayload()));
    }

    @Autowired
    public ReservationProcessor(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }
}


@Component
class NexonHealthIndicator implements HealthIndicator {

	@Override
	public Health health() {
		return Health.status("I <3 Nexon!!").build();
	}
}


@RestController
@RefreshScope
class MessageRestController {

	private final String value;

	@Autowired
	public MessageRestController(
			@Value("${message}") String value) {
		this.value = value;
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/message")
	public String read() {
		return this.value;
	}

}

@Component
class SampleDataCLR implements CommandLineRunner {

	private final ReservationRepository reservationRepository;

	@Autowired
	public SampleDataCLR(ReservationRepository reservationRepository) {
		this.reservationRepository = reservationRepository;
	}

	@Override
	public void run(String... args) throws Exception {
		Stream.of("Josh", "Thivakar", "André", "Thomas", "Markus",
				"Patrick", "Mugdin", "Uwe")
				.forEach(name -> reservationRepository.save(new Reservation(name)));

		reservationRepository.findAll().forEach(System.out::println);
	}
}

@RepositoryRestResource
interface ReservationRepository extends JpaRepository<Reservation, Long> {

	@RestResource(path = "by-name")
	Collection<Reservation> findByReservationName(@Param("rn") String rn);
}

@Entity
class Reservation {

	@Id
	@GeneratedValue
	private Long id; 

	private String reservationName; 

	Reservation() {// why JPA why???
	}

	public Reservation(String reservationName) {
		this.reservationName = reservationName;
	}
	
	public Long getId() {
		return id;
	}

	public String getReservationName() {
		return reservationName;
	}

	@Override
	public String toString() {
		return "Reservation{" +
				"id=" + id +
				", reservationName='" + reservationName + '\'' +
				'}';
	}
}
