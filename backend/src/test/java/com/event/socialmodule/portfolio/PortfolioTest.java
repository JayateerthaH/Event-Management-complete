package com.event.socialmodule.portfolio;

import com.event.eventmodule.api.EventApi.AttendedEvent;
import com.event.eventmodule.api.EventCard;
import com.event.socialmodule.portfolio.PortfolioService.Stats;
import com.event.socialmodule.portfolio.PortfolioService.View;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PortfolioTest {

    private static AttendedEvent attended(long id, String category) {
        return new AttendedEvent(new EventCard(id, "E" + id, "", category, null, new Date(), "Hall", 50L, "0", null, 1), null, false);
    }

    @Test
    void categoriesAreDistinctIgnoringCaseAndMostAttendedFirst() {
        var list = List.of(attended(1, "Talk"), attended(2, "Workshop"), attended(3, "workshop "), attended(4, null), attended(5, " "));
        assertThat(PortfolioService.categories(list)).containsExactly("Workshop", "Talk");
    }

    @Test
    void previewDescriptionCountsWithSingulars() {
        View v = new View("Asha", "CSE", null, null, List.of(), new Stats(1, 0, 3, 1), List.of(), List.of(), List.of());
        assertThat(PortfolioController.describe(v)).isEqualTo("CSE. 1 event attended (QR-verified), 0 certificates, 3 badges.");
    }
}
