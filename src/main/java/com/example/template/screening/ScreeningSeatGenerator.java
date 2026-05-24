package com.example.template.screening;

import com.example.template.seat.Seat;
import com.example.template.seat.SeatRepository;
import com.example.template.venue.Room;
import org.springframework.stereotype.Service;

@Service
public class ScreeningSeatGenerator {

    private final SeatRepository seatRepository;

    public ScreeningSeatGenerator(SeatRepository seatRepository) {
        this.seatRepository = seatRepository;
    }

    public void generateSeats(Screening screening, Room room) {
        int numRows = room.getRows() != null ? room.getRows() : 8;
        int numCols = room.getCols() != null ? room.getCols() : 10;

        for (int r = 0; r < numRows; r++) {
            String rowLabel = rowLabel(r);
            for (int c = 1; c <= numCols; c++) {
                Seat seat = new Seat();
                seat.setScreening(screening);
                seat.setRowLabel(rowLabel);
                seat.setColNum(c);
                seat.setStatus("free");
                if (numRows == 10) {
                    seat.setType((rowLabel.equals("E") || rowLabel.equals("F")) ? "preferential" : "standard");
                } else {
                    seat.setType((rowLabel.equals("D") || rowLabel.equals("E")) ? "preferential" : "standard");
                }
                seatRepository.save(seat);
            }
        }
    }

    private String rowLabel(int index) {
        if (index < 26) {
            return String.valueOf((char) ('A' + index));
        }
        return "R" + (index + 1);
    }
}
