package cinema;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.util.JSONPObject;
import netscape.javascript.JSObject;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.*;

@SpringBootApplication
public class Main {
    static int rows = 9;
    static int cols = 9;
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
        System.out.println("this is running!!");
    }

    public class Ticket {
        public UUID token;
        public Seat ticket;


        public class Seat {
            public int row;
            public int column;
            public int price;
            private boolean available;
            public Seat(int i, int j) {
                this.row = i;
                this.column = j;
                this.price = (this.row <= 4) ? 10 : 8;
                this.available = true;
            }
        }
        public Ticket(int i, int j) {
            this.ticket = new Seat(i, j);
            setToken();
        }
        public void setToken() {
            this.token = UUID.randomUUID();
        }
        public UUID getToken() {
            return this.token;
        }


    }
    Movie sherk2 = new Movie(rows, cols);
    public class Movie {
        public int total_rows;
        public int total_columns;
        public List<Ticket> available_seats;
        public PublicInformation public_information;

        private String password;

        public Movie(int rows, int cols) {

            this.total_rows = rows;
            this.total_columns = cols;
            Ticket[] seats = new Ticket[rows * cols];
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    seats[(i * rows) + j] = new Ticket(i + 1, j + 1);
                }
            }
            this.available_seats = List.of(seats);
            this.public_information = new PublicInformation(rows, cols, available_seats);
            this.password = "super_secret";
        }
        class PublicInformation {
            public int total_rows;
            public int total_columns;
            public List<Ticket.Seat> available_seats;


            public PublicInformation(int rows, int cols, List<Ticket> available_seats) {
                this.total_rows = rows;
                this.total_columns = cols;
                Ticket.Seat[] seats = new Ticket.Seat[rows*cols];
                for (int i = 0; i < rows; i++) {
                    for (int j = 0; j < cols; j++) {
                        seats[(i * rows) + j] = available_seats.get((i * rows) + j).ticket;
                    }
                }
                this.available_seats = List.of(seats);
            }
        }


        public Stats getStats() {
            Stats stats = new Stats();
            for (int i = 0; i < available_seats.size(); i++) {
                if (available_seats.get(i).ticket.available) {
                    stats.number_of_available_seats++;

                } else {
                    stats.number_of_purchased_tickets++;
                    stats.current_income += available_seats.get(i).ticket.price;
                }
            }
            return stats;
        }

    }

    // error handlers
    // bad request
    public class TicketAlreadyPurchasedException extends RuntimeException {
        public TicketAlreadyPurchasedException(String cause) {
            super(cause);
        }
    }

    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public class WrongPasswordException extends RuntimeException {
        public WrongPasswordException(String cause) {
            super(cause);
        }
    }

    public class InvalidPasswordException extends ResponseEntityExceptionHandler {

    }

    public class CustomErrorMessage {
        public int statusCode;
        public LocalDateTime timestamp;
        public String error;
//        public String description;

        public CustomErrorMessage(int statusCode, LocalDateTime timestamp, String error){
            this.statusCode = statusCode;
            this.timestamp = timestamp;
            this.error = error;
//            this.description = description;
        }
    }

    @ControllerAdvice
    public class ControllerExceptionHandler  extends ResponseEntityExceptionHandler{
        @ExceptionHandler(TicketAlreadyPurchasedException.class)
        public ResponseEntity<CustomErrorMessage> handleAlreadyPurchased(TicketAlreadyPurchasedException e) {
            CustomErrorMessage body = new CustomErrorMessage(
                HttpStatus.BAD_REQUEST.value(),
                LocalDateTime.now(),
                e.getMessage()
//                request.getDescription(false)
            );
            return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
        }
//        @ExceptionHandler(InvalidPasswordException.class)
//        public ResponseEntity<CustomErrorMessage> wrongPassword(InvalidPasswordException e) {
//            CustomErrorMessage body = new CustomErrorMessage(
//                HttpStatus.UNAUTHORIZED.value(),
//                LocalDateTime.now(),
//                e.getMessage()
////                request.getDescription(false)
//            );
//            return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
//        }



        @ExceptionHandler(WrongPasswordException.class)
//        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public ResponseEntity<CustomErrorMessage> notPassword(WrongPasswordException e) {
            CustomErrorMessage body = new CustomErrorMessage(
                HttpStatus.UNAUTHORIZED.value(),
                LocalDateTime.now(),
                e.getMessage()
//                request.getDescription(false)
            );
            return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
        }
        @Override
        protected ResponseEntity<Object> handleHttpMessageNotReadable (
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatus status,
            WebRequest request
        ) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("error", "The password is wrong!");
            HttpStatus newStatus = HttpStatus.UNAUTHORIZED;
            return new ResponseEntity<>(body, headers, newStatus);
        }

        @Override
        protected ResponseEntity<Object> handleHttpMediaTypeNotSupported (
            HttpMediaTypeNotSupportedException ex,
            HttpHeaders headers,
            HttpStatus status,
            WebRequest request
        ) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("error", ex.getMessage());
            return new ResponseEntity<>(body, headers, HttpStatus.I_AM_A_TEAPOT);
        }

    }



    public static class PurchaseTicket {
        public int row;
        public int column;
    }
    @RestController
    public class SeatsController {
        @GetMapping("/seats")
        public Movie.PublicInformation returnSeat() {
            return sherk2.public_information;
        }

        @PostMapping("/purchase")
        public Ticket buyTheTicket(@RequestBody PurchaseTicket ticketInfo) {
            // validation of the query
            if (ticketInfo.row > 9 || ticketInfo.column > 9 || ticketInfo.row < 1 || ticketInfo.column < 1) {
                throw new TicketAlreadyPurchasedException("The number of a row or a column is out of bounds!");
            }
            // search is available
            Ticket ticket = sherk2.available_seats.get((ticketInfo.row - 1) * rows + (ticketInfo.column - 1));
            if (ticket.ticket.available) {
                sherk2.available_seats.get((ticketInfo.row - 1) * rows + (ticketInfo.column - 1)).ticket.available =
                    false;
                return ticket;
            } else {
                throw new TicketAlreadyPurchasedException("The ticket has been already purchased!");
            }
        }
        @PostMapping("/return")
        public HashMap<String, Ticket.Seat> returnTicket(@Validated @RequestBody Token token) {
            if (token.token == null) {
                throw new TicketAlreadyPurchasedException("Wrong token!");
            }
            for (int i = 0; i < sherk2.available_seats.size(); i++) {
                if (sherk2.available_seats.get(i).getToken().equals(token.token)) {
                    Ticket.Seat ticket = sherk2.available_seats.get(i).ticket;
                    ticket.available = true;
                    HashMap<String, Ticket.Seat> returned_ticket = new HashMap<>();
                    returned_ticket.put("returned_ticket", ticket);
                    return returned_ticket;
                }
            }
            throw new TicketAlreadyPurchasedException("Wrong token!");

        }
        @PostMapping("/stats")
        public Stats stats(@RequestBody Map<String, String> password) {
//            if (password.password == null) {
//                throw new InvalidPasswordException();
//            }
            System.out.println(password.get("password"));
            if (Objects.equals(password.get("password"), sherk2.password)) {
                return sherk2.getStats();
            }
            throw new WrongPasswordException("The password is wrong!");
        }
        @PostMapping(value = "/stats", consumes = "application/x-www-form-urlencoded")
        public Stats stats(String password) {
//            if (password.password == null) {
//                throw new InvalidPasswordException();
//            }
            System.out.println(password);
            if (Objects.equals(password, sherk2.password)) {
                return sherk2.getStats();
            }
            throw new WrongPasswordException("The password is wrong!");
        }
    }
}
