package searchengine.response;

import lombok.Data;
import searchengine.response.statistics.StatisticsData;

@Data
public class StatisticsResponse {
    private boolean result;
    private StatisticsData statistics;
}
