package searchengine.response.search;

import lombok.Data;

@Data
public class DataInfo implements Comparable<DataInfo> {
    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private float relevance;


    @Override
    public int compareTo(DataInfo o) {
        return Float.compare(o.getRelevance(), this.relevance);
    }
}
