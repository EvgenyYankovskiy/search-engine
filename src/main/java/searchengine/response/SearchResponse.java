package searchengine.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import searchengine.response.search.DataInfo;

import java.util.List;
@Data
@AllArgsConstructor
public class SearchResponse {
    private boolean result;
    private String error;
    private Integer count;
    private List<DataInfo> data;
}
