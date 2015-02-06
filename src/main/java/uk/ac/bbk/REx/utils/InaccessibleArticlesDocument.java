package uk.ac.bbk.REx.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class InaccessibleArticlesDocument
{
    private Document doc;
    private List<String> pmids;
    private String urlFormat;

    public InaccessibleArticlesDocument() throws IOException
    {
        doc = Jsoup.parse(this.getClass().getResourceAsStream(
                "inaccessibleArticlesDocumentBase.html"), "UTF-8", "");
        urlFormat = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/elink.fcgi?dbfrom=pubmed&id=%s&retmode=ref";
        pmids = new ArrayList<String>();
    }

    public void addArticle(String pmid)
    {
        pmids.add(pmid);
    }

    public void writeDocument(Writer writer) throws IOException
    {
        Element articlesList = doc.select("#articlesList").first();

        for(String pmid : pmids)
        {
            Element newArticle = articlesList.appendElement("li");
            Element link = newArticle.appendElement("a");
            String url = String.format(urlFormat, pmid);
            link.attr("href", url);
            link.attr("target", "_blank");
            link.text(pmid);
        }

        writer.write(doc.toString());
    }
}
